package smartmail;

import java.time.LocalDate;

/** Data model for one email / notification. */
public class EmailMessage {
    private final String messageId;
    private final String sender;
    private final String subject;
    private final String snippet;
    private final LocalDate date;
    private final long sizeInBytes;
    private boolean flaggedForDeletion;
    private Category category;   // set by the AI/cleaning engine

    public EmailMessage(String messageId, String sender, String subject,
                        String snippet, LocalDate date, long sizeInBytes) {
        this.messageId = messageId;
        this.sender = sender;
        this.subject = subject;
        this.snippet = snippet;
        this.date = date;
        this.sizeInBytes = sizeInBytes;
    }

    public String getMessageId() { return messageId; }
    public String getSender() { return sender; }
    public String getSubject() { return subject; }
    public String getSnippet() { return snippet; }
    public LocalDate getDate() { return date; }
    public long getSizeInBytes() { return sizeInBytes; }
    public boolean isFlaggedForDeletion() { return flaggedForDeletion; }
    public void setFlaggedForDeletion(boolean v) { this.flaggedForDeletion = v; }
    public Category getCategory() { return category; }
    public void setCategory(Category c) { this.category = c; }

    @Override public String toString() {
        return String.format("[%s] %-22s %-38s %6.1f KB %s", messageId, trim(sender, 22),
                trim(subject, 38), sizeInBytes / 1024.0, category == null ? "" : category);
    }
    private static String trim(String s, int n) { return s.length() <= n ? s : s.substring(0, n - 1) + "…"; }
}
