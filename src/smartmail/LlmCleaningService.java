package smartmail;

import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.regex.*;

/**
 * Real LLM-backed classifier (OpenAI-compatible chat endpoint; works with Ollama's
 * /v1/chat/completions too). Falls back to KEEP on any error (safety first).
 * Dependency-free: builds/parses the small JSON by hand.
 */
public class LlmCleaningService implements CleaningService {
    private final String apiUrl, apiKey, model;
    private final HttpClient client = HttpClient.newHttpClient();

    public LlmCleaningService(String apiUrl, String apiKey, String model) {
        this.apiUrl = apiUrl; this.apiKey = apiKey; this.model = model;
    }

    @Override public void classify(List<EmailMessage> list) {
        for (EmailMessage m : list) m.setCategory(ask(m));
    }

    private Category ask(EmailMessage m) {
        String prompt = "Classify this email as exactly one word: SAFE_TO_DELETE (OTPs, expired promos, "
                + "newsletters), ARCHIVE (invoices, receipts, security alerts) or KEEP (personal/work). "
                + "Sender: " + m.getSender() + ". Subject: " + m.getSubject() + ". Snippet: " + m.getSnippet();
        String body = "{\"model\":\"" + model + "\",\"temperature\":0,\"messages\":[{\"role\":\"user\",\"content\":\""
                + esc(prompt) + "\"}]}";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            Matcher mt = Pattern.compile("\"content\"\\s*:\\s*\"([^\"]*)\"").matcher(res.body());
            if (mt.find()) {
                String ans = mt.group(1).toUpperCase();
                if (ans.contains("SAFE_TO_DELETE")) return Category.SAFE_TO_DELETE;
                if (ans.contains("ARCHIVE")) return Category.ARCHIVE;
            }
        } catch (Exception e) {
            System.err.println("LLM error for " + m.getMessageId() + ": " + e.getMessage());
        }
        return Category.KEEP;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }
}
