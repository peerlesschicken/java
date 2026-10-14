package smartmail;

import java.util.*;

/** Applies classification results with a safety-first, undoable workflow. */
public class ExecutionService {
    private final Mailbox box;
    private final Deque<List<EmailMessage>> undoStack = new ArrayDeque<>();

    public ExecutionService(Mailbox box) { this.box = box; }

    /** Marks SAFE_TO_DELETE items as flagged (preview step; nothing is moved yet). */
    public List<EmailMessage> flagForDeletion(List<EmailMessage> classified) {
        List<EmailMessage> out = new ArrayList<>();
        for (EmailMessage m : classified)
            if (m.getCategory() == Category.SAFE_TO_DELETE) { m.setFlaggedForDeletion(true); out.add(m); }
        return out;
    }

    /** Moves flagged items to Trash (recoverable) and archives ARCHIVE items. */
    public long execute(List<EmailMessage> classified) {
        List<EmailMessage> trashed = new ArrayList<>();
        long freed = 0;
        for (EmailMessage m : classified) {
            if (m.isFlaggedForDeletion()) { box.moveToTrash(m); trashed.add(m); }
            else if (m.getCategory() == Category.ARCHIVE) box.archive(m);
        }
        undoStack.push(trashed);
        return trashed.stream().mapToLong(EmailMessage::getSizeInBytes).sum();
    }

    public int undoLast() {
        if (undoStack.isEmpty()) return 0;
        List<EmailMessage> last = undoStack.pop();
        for (EmailMessage m : last) { box.restoreFromTrash(m); m.setFlaggedForDeletion(false); }
        return last.size();
    }

    /** Permanent purge — irreversible, clears undo history. */
    public void purge() { box.purgeTrash(); undoStack.clear(); }
}
