package school.main;

import school.network.SchoolServer;
import school.network.StudentNetworkClient;

import javax.swing.*;
import java.awt.*;

/** Controls the TCP server and provides a simple student network client. */
public class NetworkServicesDialog extends JDialog {

    private final JTextArea outputArea = new JTextArea();
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField(String.valueOf(SchoolServer.DEFAULT_PORT));
    private final JTextField studentIdField = new JTextField("ST001");
    private final JTextField courseIdField = new JTextField("SEN101");
    private final JComboBox<String> requestBox = new JComboBox<>(new String[] {
        "View Profile",
        "View Results",
        "View Registered Courses",
        "View Payment Status",
        "Register Course"
    });

    public NetworkServicesDialog(JFrame owner) {
        super(owner, "Network Services", true);

        setSize(900, 680);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("NETWORK SERVICES", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel subtitle = new JLabel(
            "TCP Server • Student Client • Socket Communication • Request/Response Protocol",
            SwingConstants.CENTER
        );

        JPanel heading = new JPanel(new GridLayout(2, 1, 4, 4));
        heading.add(title);
        heading.add(subtitle);

        JPanel settings = new JPanel(new GridLayout(2, 5, 8, 6));
        settings.add(new JLabel("Host"));
        settings.add(new JLabel("Port"));
        settings.add(new JLabel("Student ID"));
        settings.add(new JLabel("Request"));
        settings.add(new JLabel("Course ID"));
        settings.add(hostField);
        settings.add(portField);
        settings.add(studentIdField);
        settings.add(requestBox);
        settings.add(courseIdField);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setMargin(new Insets(10, 10, 10, 10));

        JButton start = new JButton("Start Server");
        JButton stop = new JButton("Stop Server");
        JButton ping = new JButton("Test Connection");
        JButton send = new JButton("Send Student Request");
        JButton clear = new JButton("Clear Display");
        JButton close = new JButton("Close");

        start.addActionListener(e -> startServer());
        stop.addActionListener(e -> SchoolServer.stop());
        ping.addActionListener(e -> sendRequest("PING"));
        send.addActionListener(e -> sendStudentRequest());
        clear.addActionListener(e -> outputArea.setText(""));
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(2, 3, 8, 8));
        buttons.add(start);
        buttons.add(stop);
        buttons.add(ping);
        buttons.add(send);
        buttons.add(clear);
        buttons.add(close);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(settings, BorderLayout.NORTH);
        center.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        root.add(heading, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        add(root);
        append("Ready. Start the server, then send requests from the student client.");
        setVisible(true);
    }

    private void startServer() {
        try {
            int port = parsePort();
            SchoolServer.start(port, this::appendFromWorker);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void sendStudentRequest() {
        String studentId = studentIdField.getText().trim();
        if (studentId.isEmpty()) {
            showError(new IllegalArgumentException("Enter a Student ID."));
            return;
        }

        String selected = String.valueOf(requestBox.getSelectedItem());
        String request;

        switch (selected) {
            case "View Profile" -> request = "PROFILE|" + studentId;
            case "View Results" -> request = "RESULTS|" + studentId;
            case "View Registered Courses" -> request = "COURSES|" + studentId;
            case "View Payment Status" -> request = "PAYMENT_STATUS|" + studentId;
            case "Register Course" -> {
                String courseId = courseIdField.getText().trim();
                if (courseId.isEmpty()) {
                    showError(new IllegalArgumentException("Enter a Course ID."));
                    return;
                }
                request = "REGISTER_COURSE|" + studentId + "|" + courseId;
            }
            default -> throw new IllegalStateException("Unknown request type.");
        }

        sendRequest(request);
    }

    private void sendRequest(String request) {
        final String host = hostField.getText().trim();
        final int port;
        try {
            port = parsePort();
        } catch (Exception e) {
            showError(e);
            return;
        }

        append("CLIENT -> " + request);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return StudentNetworkClient.sendRequest(host, port, request);
            }

            @Override
            protected void done() {
                try {
                    append("SERVER RESPONSE:\n" + get());
                } catch (Exception e) {
                    showError(new Exception("Client request failed. Is the server running?", e));
                }
            }
        }.execute();
    }

    private int parsePort() {
        int port = Integer.parseInt(portField.getText().trim());
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
        return port;
    }

    private void appendFromWorker(String message) {
        SwingUtilities.invokeLater(() -> append(message));
    }

    private void append(String message) {
        outputArea.append(message + System.lineSeparator());
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void showError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        JOptionPane.showMessageDialog(this, message, "Network Error", JOptionPane.ERROR_MESSAGE);
        append("ERROR: " + message);
    }
}
