package smartmail;

import java.time.LocalDate;
import java.util.*;

/** In-memory mailbox with realistic sample data (no credentials needed). */
public class MockMailbox implements Mailbox {
    private final List<EmailMessage> inbox = new ArrayList<>();
    private final List<EmailMessage> trash = new ArrayList<>();
    private final List<EmailMessage> archive = new ArrayList<>();
    private final long quota;

    public MockMailbox(long quotaBytes) {
        this.quota = quotaBytes;
        LocalDate t = LocalDate.now();
        int id = 1;
        add(id++, "HDFC Bank", "Your OTP is 482913", "Use this OTP to complete your transaction. Do not share.", t.minusDays(40), 4_000);
        add(id++, "Amazon.in", "Limited time offer: 60% off today!", "Deals just for you. Unsubscribe anytime.", t.minusDays(12), 180_000);
        add(id++, "Medium Digest", "Your weekly Newsletter", "Top stories this week... Unsubscribe", t.minusDays(30), 250_000);
        add(id++, "Swiggy", "Verification Code: 7731", "Your verification code expires in 10 minutes.", t.minusDays(90), 3_500);
        add(id++, "Prof. Mehta", "Re: Semester project review", "Please send the revised report by Friday.", t.minusDays(2), 22_000);
        add(id++, "Flipkart", "Invoice for order #OD9981", "Your invoice and receipt are attached.", t.minusDays(20), 640_000);
        add(id++, "Google", "Security alert: new sign-in", "A new device signed in to your account.", t.minusDays(5), 9_000);
        add(id++, "Mom", "Dinner on Sunday?", "Are you coming home this weekend?", t.minusDays(1), 2_000);
        add(id++, "LinkedIn", "5 jobs you may like - Unsubscribe", "Jobs matching your profile.", t.minusDays(8), 120_000);
        add(id++, "no-reply@system", "Storage full warning", "Your mailbox is almost full. Free up space.", t.minusDays(3), 5_000);
        add(id++, "TeamLead", "Sprint planning notes", "Attached are the notes from today's sprint planning.", t.minusDays(4), 1_800_000);
        add(id++, "Zomato", "Limited time offer: free delivery", "Order now. Unsubscribe", t.minusDays(60), 210_000);
        add(id++, "Paytm", "OTP for login", "Your OTP is 120045. Valid for 5 minutes.", t.minusDays(75), 3_800);
        add(id++, "Cloud Backup", "Backup logs 2024-01", "Auto-generated system log. Verification Code not required.", t.minusDays(200), 3_200_000);
    }

    private void add(int n, String from, String subj, String snip, LocalDate d, long size) {
        inbox.add(new EmailMessage(String.format("M%03d", n), from, subj, snip, d, size));
    }

    @Override public List<EmailMessage> fetchAll() { return new ArrayList<>(inbox); }
    @Override public long usedBytes() { return inbox.stream().mapToLong(EmailMessage::getSizeInBytes).sum()
            + trash.stream().mapToLong(EmailMessage::getSizeInBytes).sum()
            + archive.stream().mapToLong(EmailMessage::getSizeInBytes).sum(); }
    @Override public long quotaBytes() { return quota; }
    @Override public void moveToTrash(EmailMessage m) { if (inbox.remove(m)) trash.add(m); }
    @Override public void restoreFromTrash(EmailMessage m) { if (trash.remove(m)) inbox.add(m); }
    @Override public void archive(EmailMessage m) { if (inbox.remove(m)) archive.add(m); }
    @Override public void purgeTrash() { trash.clear(); }
    public int trashCount() { return trash.size(); }
    public int archiveCount() { return archive.size(); }
}
