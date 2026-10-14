package smartmail;

import java.util.List;

/** Classifies emails into SAFE_TO_DELETE / ARCHIVE / KEEP. */
public interface CleaningService {
    void classify(List<EmailMessage> candidates);
}
