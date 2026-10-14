package smartmail;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Swing dashboard: storage bar, inbox table with AI categories, and an "AI Quick Clean"
 * button with confirmation, undo and purge. Uses the same services as the CLI.
 *
 *   java -cp out smartmail.Dashboard
 *   java -cp out smartmail.Dashboard --llm <url> <model>      (needs OPENAI_API_KEY)
 */
public class DashboardPanel extends JPanel {
    private static final String[] COLS = {"ID", "Sender", "Subject", "Date", "Size", "AI Category"};

    private final MockMailbox box;
    private final ThresholdMonitorService monitor;
    private final CleaningService cleaner;
    private final ExecutionService exec;

    private final JProgressBar usageBar = new JProgressBar(0, 100);
    private final JLabel usageLabel = new JLabel();
    private final JLabel alertLabel = new JLabel(" ");
    private final JLabel statsLabel = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextArea log = new JTextArea(5, 40);
    private final JButton cleanBtn = new JButton("AI Quick Clean");
    private final JButton undoBtn = new JButton("Undo Last Clean");
    private final JButton purgeBtn = new JButton("Purge Trash");
    private final JProgressBar workBar = new JProgressBar();
    private List<EmailMessage> shown = new ArrayList<>();

    public DashboardPanel(CleaningService cleaner) {
        this.cleaner = cleaner;
        this.box = new MockMailbox(8L * 1024 * 1024);
        this.monitor = new ThresholdMonitorService(70.0, 1_000_000);
        this.exec = new ExecutionService(box);
        buildUi();
        refresh();
        log("Ready. Mailbox loaded with " + shown.size() + " messages.");
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // --- top: storage panel
        usageBar.setStringPainted(true);
        usageBar.setPreferredSize(new Dimension(100, 28));
        alertLabel.setFont(alertLabel.getFont().deriveFont(Font.BOLD));
        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.setBorder(BorderFactory.createTitledBorder("Storage"));
        top.add(usageLabel, BorderLayout.NORTH);
        top.add(usageBar, BorderLayout.CENTER);
        JPanel sub = new JPanel(new BorderLayout());
        sub.add(alertLabel, BorderLayout.WEST);
        sub.add(statsLabel, BorderLayout.EAST);
        top.add(sub, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);

        // --- centre: table
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        table.getColumnModel().getColumn(4).setMaxWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);
        table.setDefaultRenderer(Object.class, new CategoryRenderer());
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createTitledBorder("Inbox & notifications"));
        add(sp, BorderLayout.CENTER);

        // --- bottom: buttons + log
        cleanBtn.setFont(cleanBtn.getFont().deriveFont(Font.BOLD, 14f));
        cleanBtn.addActionListener(e -> quickClean());
        undoBtn.addActionListener(e -> {
            int n = exec.undoLast();
            log(n == 0 ? "Nothing to undo." : "Restored " + n + " email(s) from Trash.");
            clearCategories();
            refresh();
        });
        purgeBtn.addActionListener(e -> purge());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        workBar.setVisible(false);
        workBar.setStringPainted(true);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.add(cleanBtn); buttons.add(undoBtn); buttons.add(purgeBtn); buttons.add(refreshBtn);
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        legend.add(chip("SAFE_TO_DELETE", CategoryRenderer.DELETE));
        legend.add(chip("ARCHIVE", CategoryRenderer.ARCHIVE));
        legend.add(chip("KEEP", CategoryRenderer.KEEP));
        JPanel row = new JPanel(new BorderLayout());
        row.add(buttons, BorderLayout.WEST);
        row.add(legend, BorderLayout.EAST);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logSp = new JScrollPane(log);
        logSp.setBorder(BorderFactory.createTitledBorder("Activity log"));

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(row, BorderLayout.NORTH);
        bottom.add(workBar, BorderLayout.CENTER);
        bottom.add(logSp, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(960, 700));
    }

    private static JLabel chip(String text, Color bg) {
        JLabel l = new JLabel(" " + text + " ", SwingConstants.CENTER);
        l.setOpaque(true); l.setBackground(bg); l.setForeground(Color.BLACK);
        l.setBorder(BorderFactory.createLineBorder(bg.darker()));
        return l;
    }

    // ---------- actions ----------

    private void quickClean() {
        List<EmailMessage> cand = monitor.scanForCandidates(box.fetchAll());
        if (cand.isEmpty()) { log("No cleanup candidates found."); return; }
        setBusy(true, cand.size());
        log("Scanning... " + cand.size() + " candidate(s). Sending to cleaning engine.");

        new SwingWorker<Void, Integer>() {
            @Override protected Void doInBackground() {
                for (int i = 0; i < cand.size(); i++) {
                    cleaner.classify(List.of(cand.get(i)));
                    publish(i + 1);
                }
                return null;
            }
            @Override protected void process(List<Integer> chunks) {
                workBar.setValue(chunks.get(chunks.size() - 1));
                refreshTableOnly();
            }
            @Override protected void done() {
                setBusy(false, 0);
                refreshTableOnly();
                confirmAndExecute(cand);
            }
        }.execute();
    }

    private void confirmAndExecute(List<EmailMessage> cand) {
        List<EmailMessage> del = exec.flagForDeletion(cand);
        long archived = cand.stream().filter(m -> m.getCategory() == Category.ARCHIVE).count();
        long bytes = del.stream().mapToLong(EmailMessage::getSizeInBytes).sum();
        if (del.isEmpty() && archived == 0) { log("Nothing to delete or archive."); return; }

        StringBuilder sb = new StringBuilder();
        sb.append(del.size()).append(" email(s) will be moved to Trash (")
          .append(String.format("%.2f MB", bytes / 1048576.0)).append("):\n\n");
        for (EmailMessage m : del) sb.append("  • ").append(m.getSender()).append(" - ").append(m.getSubject()).append('\n');
        if (archived > 0) sb.append('\n').append(archived).append(" email(s) will be archived.");
        sb.append("\n\nYou can undo this afterwards. Proceed?");

        if (confirm("Confirm AI Quick Clean", sb.toString(), Math.min(18, del.size() + 7))) {
            long freed = exec.execute(cand);
            log(String.format("Moved %d email(s) to Trash, freed %.2f MB. Undo available.", del.size(), freed / 1048576.0));
        } else {
            del.forEach(m -> m.setFlaggedForDeletion(false));
            log("Clean cancelled. Nothing was changed.");
        }
        refresh();
    }

    /** Confirmation dialog; protected so tests can override it. */
    protected boolean confirm(String title, String message, int rows) {
        JTextArea area = new JTextArea(message, rows, 52);
        area.setEditable(false); area.setBackground(UIManager.getColor("Panel.background"));
        return JOptionPane.showConfirmDialog(this, new JScrollPane(area), title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void purge() {
        if (box.trashCount() == 0) { log("Trash is empty."); return; }
        if (confirm("Purge Trash", "Permanently delete " + box.trashCount()
                + " email(s) in Trash?\nThis cannot be undone.", 3)) {
            int n = box.trashCount();
            exec.purge();
            log("Permanently deleted " + n + " email(s).");
            refresh();
        }
    }

    private void clearCategories() { for (EmailMessage m : box.fetchAll()) m.setCategory(null); }

    private void setBusy(boolean busy, int max) {
        cleanBtn.setEnabled(!busy); undoBtn.setEnabled(!busy); purgeBtn.setEnabled(!busy);
        workBar.setVisible(busy);
        workBar.setMaximum(Math.max(1, max)); workBar.setValue(0);
        workBar.setString("AI analysing emails...");
    }

    // ---------- view refresh ----------

    private void refresh() { refreshTableOnly(); refreshStorage(); }

    private void refreshTableOnly() {
        shown = box.fetchAll();
        model.setRowCount(0);
        for (EmailMessage m : shown)
            model.addRow(new Object[]{m.getMessageId(), m.getSender(), m.getSubject(), m.getDate(),
                    String.format("%.1f KB", m.getSizeInBytes() / 1024.0),
                    m.getCategory() == null ? "-" : m.getCategory().name()});
    }

    private void refreshStorage() {
        double pct = box.usagePercent();
        usageBar.setValue((int) Math.round(pct));
        usageBar.setString(String.format("%.0f%% Storage Used", pct));
        boolean over = monitor.isThresholdExceeded(box);
        usageBar.setForeground(pct >= 90 ? new Color(0xC62828) : over ? new Color(0xEF6C00) : new Color(0x2E7D32));
        usageLabel.setText(String.format("%.2f MB of %.0f MB used", box.usedMB(), box.quotaMB()));
        String msg = over ? "\u26A0 Threshold exceeded (" + (int) monitor.getThresholdPercent() + "%) - cleanup recommended"
                          : "\u2713 Storage within limit";
        if (box.trashCount() > 0)
            msg += String.format("  |  Trash holds %.2f MB: purge it to free server space", box.trashBytes() / 1048576.0);
        alertLabel.setText(msg);
        alertLabel.setForeground(over ? new Color(0xC62828) : new Color(0x2E7D32));
        statsLabel.setText("Inbox: " + shown.size() + "   Trash: " + box.trashCount() + "   Archive: " + box.archiveCount());
        undoBtn.setEnabled(true);
    }

    private void log(String s) {
        log.append(java.time.LocalTime.now().withNano(0) + "  " + s + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    /** Colours each row by AI category (dark text on light fills, readable in any theme). */
    private static class CategoryRenderer extends DefaultTableCellRenderer {
        static final Color DELETE = new Color(0xF8BBBB), ARCHIVE = new Color(0xBBD7F8), KEEP = new Color(0xC8E6C9);
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            Object cat = t.getValueAt(r, 5);
            Color bg = null;
            if ("SAFE_TO_DELETE".equals(cat)) bg = DELETE;
            else if ("ARCHIVE".equals(cat)) bg = ARCHIVE;
            else if ("KEEP".equals(cat)) bg = KEEP;
            if (!sel && bg != null) { setBackground(bg); setForeground(Color.BLACK); }
            else if (!sel) { setBackground(t.getBackground()); setForeground(t.getForeground()); }
            setHorizontalAlignment(c == 4 ? RIGHT : LEFT);
            return this;
        }
    }
}
