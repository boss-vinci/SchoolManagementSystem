package school.main;

import school.util.AppLogger;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class SystemLogsDialog extends JDialog {

    private final JTextArea logArea = new JTextArea();

    public SystemLogsDialog(JFrame owner) {

        super(owner, "System Logs", true);

        setSize(1000, 650);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        );

        JLabel heading = new JLabel(
            "APPLICATION EVENT LOG",
            SwingConstants.CENTER
        );
        heading.setFont(new Font("Segoe UI", Font.BOLD, 24));

        JLabel pathLabel = new JLabel(
            "Log file: " + AppLogger.getLogPath().toAbsolutePath()
        );

        JPanel north = new JPanel(new BorderLayout(5, 5));
        north.add(heading, BorderLayout.NORTH);
        north.add(pathLabel, BorderLayout.SOUTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setLineWrap(false);

        JButton refresh = new JButton("Refresh Logs");
        JButton close = new JButton("Close");

        refresh.addActionListener(e -> refreshLogs());
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(refresh);
        buttons.add(close);

        main.add(north, BorderLayout.NORTH);
        main.add(new JScrollPane(logArea), BorderLayout.CENTER);
        main.add(buttons, BorderLayout.SOUTH);

        add(main);

        refreshLogs();
        setVisible(true);
    }

    private void refreshLogs() {

        Path path = AppLogger.getLogPath();

        try {
            if (!Files.exists(path)) {
                logArea.setText("No log entries have been created yet.");
                return;
            }

            logArea.setText(Files.readString(path));
            logArea.setCaretPosition(logArea.getDocument().getLength());

        } catch (Exception e) {
            logArea.setText(
                "Could not read the log file: " + e.getMessage()
            );
        }
    }
}
