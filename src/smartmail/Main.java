package smartmail;

import java.util.*;

/**
 * CLI prototype. Run with no args for the offline rule engine, or:
 *   java smartmail.Main --llm https://api.openai.com/v1/chat/completions gpt-4o-mini   (needs OPENAI_API_KEY)
 *   java smartmail.Main --auto     (non-interactive demo run)
 */
public class Main {
    public static void main(String[] args) {
        boolean auto = Arrays.asList(args).contains("--auto");
        MockMailbox box = new MockMailbox(8L * 1024 * 1024);     // 8 MB demo quota
        ThresholdMonitorService monitor = new ThresholdMonitorService(70.0, 1_000_000);
        CleaningService cleaner = new RuleBasedCleaningService();
        for (int i = 0; i < args.length - 2; i++)
            if (args[i].equals("--llm"))
                cleaner = new LlmCleaningService(args[i + 1], System.getenv("OPENAI_API_KEY"), args[i + 2]);
        ExecutionService exec = new ExecutionService(box);
        Scanner in = new Scanner(System.in);
        List<EmailMessage> lastBatch = new ArrayList<>();

        while (true) {
            status(box, monitor);
            System.out.println("\n1) List inbox  2) AI Quick Clean  3) Undo last clean  4) Purge trash  5) Quit");
            String c = auto ? autoStep(box, lastBatch) : prompt(in, "> ");
            if (c == null || c.equals("5")) break;
            switch (c) {
                case "1" -> box.fetchAll().forEach(System.out::println);
                case "2" -> {
                    List<EmailMessage> cand = monitor.scanForCandidates(box.fetchAll());
                    System.out.println("\nScanning… " + cand.size() + " candidate(s) found.");
                    cleaner.classify(cand);
                    cand.forEach(System.out::println);
                    List<EmailMessage> del = exec.flagForDeletion(cand);
                    long bytes = del.stream().mapToLong(EmailMessage::getSizeInBytes).sum();
                    System.out.printf("%n%d email(s) flagged for deletion (%.2f MB). Rest of ARCHIVE items will be archived.%n",
                            del.size(), bytes / 1048576.0);
                    String ok = auto ? "y" : prompt(in, "Move to Trash? (y/n) ");
                    if (ok.equalsIgnoreCase("y")) {
                        long freed = exec.execute(cand);
                        System.out.printf("Done. Moved %.2f MB to Trash (undo available).%n", freed / 1048576.0);
                    } else del.forEach(m -> m.setFlaggedForDeletion(false));
                }
                case "3" -> System.out.println("Restored " + exec.undoLast() + " email(s).");
                case "4" -> { exec.purge(); System.out.println("Trash purged permanently."); }
                default -> System.out.println("Unknown option.");
            }
        }
        System.out.println("Bye.");
    }

    private static int step = 0;
    private static String autoStep(Mailbox b, List<EmailMessage> l) {
        return switch (step++) { case 0 -> "1"; case 1 -> "2"; case 2 -> "3"; case 3 -> "2"; case 4 -> "4"; default -> "5"; };
    }

    private static String prompt(Scanner in, String p) {
        System.out.print(p);
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    private static void status(Mailbox box, ThresholdMonitorService mon) {
        double pct = box.usagePercent();
        int filled = (int) Math.min(30, Math.round(pct / 100 * 30));
        System.out.printf("%n=== Storage: [%s%s] %.0f%% (%.2f / %.0f MB)%s%n",
                "#".repeat(filled), "-".repeat(30 - filled), pct, box.usedMB(), box.quotaMB(),
                mon.isThresholdExceeded(box) ? "  ⚠ THRESHOLD EXCEEDED (" + (int) mon.getThresholdPercent() + "%)" : "");
    }
}
