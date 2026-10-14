package smartmail;

import java.util.*;

/** Tracks storage thresholds and scans messages for cleanup keywords / large size. */
public class ThresholdMonitorService {
    private final double thresholdPercent;
    private final long largeAttachmentBytes;
    private final List<String> keywords;

    public ThresholdMonitorService(double thresholdPercent, long largeAttachmentBytes) {
        this.thresholdPercent = thresholdPercent;
        this.largeAttachmentBytes = largeAttachmentBytes;
        this.keywords = new ArrayList<>(List.of(
                "otp", "unsubscribe", "verification code", "storage full", "newsletter", "limited time offer"));
    }

    public void addKeyword(String k) { keywords.add(k.toLowerCase()); }
    public double getThresholdPercent() { return thresholdPercent; }

    public boolean isThresholdExceeded(Mailbox box) { return box.usagePercent() >= thresholdPercent; }

    /** Returns messages that match a keyword or exceed the large-size limit. */
    public List<EmailMessage> scanForCandidates(List<EmailMessage> messages) {
        List<EmailMessage> out = new ArrayList<>();
        for (EmailMessage m : messages) {
            String hay = (m.getSubject() + " " + m.getSnippet()).toLowerCase();
            boolean kw = keywords.stream().anyMatch(hay::contains);
            if (kw || m.getSizeInBytes() >= largeAttachmentBytes) out.add(m);
        }
        return out;
    }
}
