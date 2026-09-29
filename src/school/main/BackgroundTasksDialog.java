package school.main;

import school.service.BackgroundTaskService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** GUI for Part 27 multithreading demonstrations. */
public class BackgroundTasksDialog extends JDialog {

    private final JTextArea outputArea = new JTextArea();

    public BackgroundTasksDialog(JFrame owner) {
        super(owner, "Background Tasks", true);

        setSize(900, 650);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("BACKGROUND TASKS & SYSTEM OPERATIONS", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel subtitle = new JLabel(
            "Multithreading • Thread Pool • Reports • Backups • Record Processing",
            SwingConstants.CENTER
        );

        JPanel heading = new JPanel(new GridLayout(2, 1, 4, 4));
        heading.add(title);
        heading.add(subtitle);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setMargin(new Insets(12, 12, 12, 12));

        JButton threadDemo = new JButton("Run Thread + Runnable Demo");
        JButton report = new JButton("Generate Report in Background");
        JButton process = new JButton("Process Student Records");
        JButton backup = new JButton("Create Database Backup");
        JButton poolDemo = new JButton("Run 3 Thread-Pool Tasks");
        JButton history = new JButton("Refresh Task History");
        JButton clear = new JButton("Clear Display");
        JButton close = new JButton("Close");

        threadDemo.addActionListener(e -> {
            append("Starting direct Thread/Runnable demonstration...");
            BackgroundTaskService.runThreadDemo(this::appendFromWorker);
        });

        report.addActionListener(e -> {
            append("Submitting report generation to ExecutorService...");
            BackgroundTaskService.generateReportAsync(this::appendFromWorker);
        });

        process.addActionListener(e -> {
            append("Submitting student record processing to ExecutorService...");
            BackgroundTaskService.processStudentRecordsAsync(this::appendFromWorker);
        });

        backup.addActionListener(e -> {
            append("Submitting H2 backup task to ExecutorService...");
            BackgroundTaskService.createDatabaseBackupAsync(this::appendFromWorker);
        });

        poolDemo.addActionListener(e -> {
            append("Submitting three tasks to the fixed thread pool...");
            BackgroundTaskService.runThreadPoolDemo(this::appendFromWorker);
        });

        history.addActionListener(e -> refreshHistory());
        clear.addActionListener(e -> outputArea.setText(""));
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(4, 2, 8, 8));
        buttons.add(threadDemo);
        buttons.add(report);
        buttons.add(process);
        buttons.add(backup);
        buttons.add(poolDemo);
        buttons.add(history);
        buttons.add(clear);
        buttons.add(close);

        root.add(heading, BorderLayout.NORTH);
        root.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        add(root);
        append("Ready. Background work will run without freezing the Swing user interface.");
        setVisible(true);
    }

    private void appendFromWorker(String message) {
        SwingUtilities.invokeLater(() -> append(message));
    }

    private void append(String message) {
        outputArea.append(message + System.lineSeparator());
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void refreshHistory() {
        List<String> history = BackgroundTaskService.getHistorySnapshot();
        outputArea.setText("TASK HISTORY (thread-safe ConcurrentLinkedQueue)\n");
        outputArea.append("================================================\n");
        if (history.isEmpty()) {
            outputArea.append("No completed/recorded tasks yet.\n");
        } else {
            for (String item : history) {
                outputArea.append(item + System.lineSeparator());
            }
        }
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
}
