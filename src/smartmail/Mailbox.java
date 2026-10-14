package smartmail;

import java.util.List;

/**
 * Abstraction over a mail source. Phase 1 uses {@link MockMailbox};
 * Phase 2 can add an ImapMailbox (Jakarta Mail) implementing the same interface.
 */
public interface Mailbox {
    List<EmailMessage> fetchAll();
    long usedBytes();
    long quotaBytes();
    void moveToTrash(EmailMessage m);
    void restoreFromTrash(EmailMessage m);
    void archive(EmailMessage m);
    void purgeTrash();

    default double usedMB()  { return usedBytes()  / (1024.0 * 1024.0); }
    default double quotaMB() { return quotaBytes() / (1024.0 * 1024.0); }
    default double usagePercent() { return 100.0 * usedBytes() / quotaBytes(); }
}
