# Smart Mail & Notification Storage Manager (Phase 1 prototype)

Requires JDK 11+ (no external libraries).

    javac -d out src/smartmail/*.java
    java -cp out smartmail.Main            # interactive CLI, offline rule-based "AI"
    java -cp out smartmail.Main --auto     # scripted demo
    OPENAI_API_KEY=... java -cp out smartmail.Main --llm https://api.openai.com/v1/chat/completions gpt-4o-mini
    # Ollama: --llm http://localhost:11434/v1/chat/completions llama3

Layout: Mailbox (interface) -> MockMailbox | (Phase 2: ImapMailbox via Jakarta Mail)
ThresholdMonitorService -> CleaningService (RuleBased | Llm) -> ExecutionService (trash + undo).
