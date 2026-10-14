package smartmail;

import javax.swing.*;

/**
 * Entry point for the GUI.
 *   java -cp out smartmail.Dashboard
 *   java -cp out smartmail.Dashboard --llm <url> <model>      (needs OPENAI_API_KEY)
 */
public class Dashboard {
    public static void main(String[] args) {
        CleaningService cleaner = new RuleBasedCleaningService();
        for (int i = 0; i < args.length - 2; i++)
            if (args[i].equals("--llm"))
                cleaner = new LlmCleaningService(args[i + 1], System.getenv("OPENAI_API_KEY"), args[i + 2]);
        final CleaningService c = cleaner;
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
            JFrame f = new JFrame("Smart Mail & Notification Storage Manager");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new DashboardPanel(c));
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
