package smartmail;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Offline "mock LLM": heuristic classifier so the prototype works without API credits. */
public class RuleBasedCleaningService implements CleaningService {
    @Override public void classify(List<EmailMessage> list) {
        for (EmailMessage m : list) m.setCategory(decide(m));
    }

    Category decide(EmailMessage m) {
        String t = (m.getSubject() + " " + m.getSnippet()).toLowerCase();
        long ageDays = ChronoUnit.DAYS.between(m.getDate(), LocalDate.now());

        if (t.contains("invoice") || t.contains("receipt") || t.contains("security alert")) return Category.ARCHIVE;
        if (t.contains("otp") || t.contains("verification code")) return ageDays > 1 ? Category.SAFE_TO_DELETE : Category.KEEP;
        if (t.contains("unsubscribe") || t.contains("newsletter") || t.contains("limited time offer")) return Category.SAFE_TO_DELETE;
        if (t.contains("storage full") || t.contains("auto-generated")) return Category.SAFE_TO_DELETE;
        return Category.KEEP;   // personal / work / unknown => be conservative
    }
}
