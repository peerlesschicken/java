# Smart Mail & Notification Storage Manager (Phase 1 prototype)

Requires JDK 11+ (no external libraries).

    javac -d out src/smartmail/*.java
    java -cp out smartmail.Main            # interactive CLI, offline rule-based "AI"
    java -cp out smartmail.Main --auto     # scripted demo
    OPENAI_API_KEY=... java -cp out smartmail.Main --llm https://api.openai.com/v1/chat/completions gpt-4o-mini
    # Ollama: --llm http://localhost:11434/v1/chat/completions llama3

## GUI dashboard (Phase 3)

    java -cp out smartmail.Dashboard
    OPENAI_API_KEY=... java -cp out smartmail.Dashboard --llm https://api.openai.com/v1/chat/completions gpt-4o-mini

Needs a desktop JDK (Swing). Storage bar, colour-coded inbox table, AI Quick Clean with confirmation dialog,
Undo and Purge. Screenshots in docs/.

Layout: Mailbox (interface) -> MockMailbox | (Phase 2: ImapMailbox via Jakarta Mail)
ThresholdMonitorService -> CleaningService (RuleBased | Llm) -> ExecutionService (trash + undo).
DashboardPanel (Swing UI) is wrapped by Dashboard (JFrame launcher) and reuses the same services as the CLI.
