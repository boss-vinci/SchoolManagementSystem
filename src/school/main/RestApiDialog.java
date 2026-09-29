package school.main;

import school.api.SchoolRestApiServer;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Controls and demonstrates the JSON REST API. */
public class RestApiDialog extends JDialog {

    private final JTextArea outputArea = new JTextArea();
    private final JTextField hostField = new JTextField("127.0.0.1");
    private final JTextField portField = new JTextField(String.valueOf(SchoolRestApiServer.DEFAULT_PORT));
    private final JTextField studentIdField = new JTextField("ST001");
    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public RestApiDialog(JFrame owner) {
        super(owner, "REST API Services", true);

        setSize(950, 720);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("REST API SERVICES", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel subtitle = new JLabel(
            "HTTP • JSON • GET • POST • PUT • DELETE",
            SwingConstants.CENTER
        );

        JPanel heading = new JPanel(new GridLayout(2, 1, 4, 4));
        heading.add(title);
        heading.add(subtitle);

        JPanel settings = new JPanel(new GridLayout(2, 3, 8, 6));
        settings.add(new JLabel("Host"));
        settings.add(new JLabel("Port"));
        settings.add(new JLabel("Student ID"));
        settings.add(hostField);
        settings.add(portField);
        settings.add(studentIdField);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        outputArea.setMargin(new Insets(10, 10, 10, 10));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JButton start = new JButton("Start REST API");
        JButton stop = new JButton("Stop REST API");
        JButton health = new JButton("Health Check");
        JButton getStudents = new JButton("GET Students");
        JButton getStudent = new JButton("GET Student by ID");
        JButton getCourses = new JButton("GET Courses");
        JButton getResults = new JButton("GET Results");
        JButton getPayments = new JButton("GET Payments");
        JButton crudDemo = new JButton("Run Student CRUD Demo");
        JButton clear = new JButton("Clear Display");
        JButton close = new JButton("Close");

        start.addActionListener(e -> startApi());
        stop.addActionListener(e -> SchoolRestApiServer.stop());
        health.addActionListener(e -> sendGet("/health"));
        getStudents.addActionListener(e -> sendGet("/students"));
        getStudent.addActionListener(e -> {
            String id = studentIdField.getText().trim();
            if (id.isEmpty()) {
                showError(new IllegalArgumentException("Enter a Student ID."));
                return;
            }
            sendGet("/students/" + encodePath(id));
        });
        getCourses.addActionListener(e -> sendGet("/courses"));
        getResults.addActionListener(e -> sendGet("/results"));
        getPayments.addActionListener(e -> sendGet("/payments"));
        crudDemo.addActionListener(e -> runCrudDemo());
        clear.addActionListener(e -> outputArea.setText(""));
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new GridLayout(0, 3, 8, 8));
        buttons.add(start);
        buttons.add(stop);
        buttons.add(health);
        buttons.add(getStudents);
        buttons.add(getStudent);
        buttons.add(getCourses);
        buttons.add(getResults);
        buttons.add(getPayments);
        buttons.add(crudDemo);
        buttons.add(clear);
        buttons.add(close);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(settings, BorderLayout.NORTH);
        center.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        root.add(heading, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);

        add(root);
        append("Ready. Start the REST API, then test the JSON endpoints.");
        setVisible(true);
    }

    private void startApi() {
        try {
            SchoolRestApiServer.start(parsePort(), this::appendFromWorker);
        } catch (Exception e) {
            showError(e);
        }
    }

    private void sendGet(String path) {
        runRequest("GET", path, null);
    }

    private void runRequest(String method, String path, String body) {
        append(method + " " + path);

        new SwingWorker<ApiResponse, Void>() {
            @Override
            protected ApiResponse doInBackground() throws Exception {
                return send(method, path, body);
            }

            @Override
            protected void done() {
                try {
                    ApiResponse response = get();
                    append("HTTP " + response.statusCode() + "\n" + prettyJson(response.body()));
                } catch (Exception e) {
                    showError(new Exception("REST request failed. Is the REST API running?", e));
                }
            }
        }.execute();
    }

    private void runCrudDemo() {
        append("Starting CRUD demo with temporary student APITEST001...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                StringBuilder out = new StringBuilder();

                // Clean up a previous interrupted demo if necessary.
                send("DELETE", "/students/APITEST001", null);

                ApiResponse post = send("POST", "/students", """
                    {
                      "studentId":"APITEST001",
                      "firstName":"API",
                      "lastName":"Test",
                      "email":"api.test@example.com",
                      "department":"Software Engineering",
                      "level":100
                    }
                    """);
                appendResponse(out, "POST /students", post);

                ApiResponse put = send("PUT", "/students/APITEST001", """
                    {
                      "email":"api.updated@example.com",
                      "level":200
                    }
                    """);
                appendResponse(out, "PUT /students/APITEST001", put);

                ApiResponse get = send("GET", "/students/APITEST001", null);
                appendResponse(out, "GET /students/APITEST001", get);

                ApiResponse delete = send("DELETE", "/students/APITEST001", null);
                appendResponse(out, "DELETE /students/APITEST001", delete);

                return out.toString();
            }

            @Override
            protected void done() {
                try {
                    append(get());
                    append("CRUD demo finished. Temporary student was removed.");
                } catch (Exception e) {
                    showError(new Exception("CRUD demo failed.", e));
                }
            }
        }.execute();
    }

    private ApiResponse send(String method, String path, String body) throws Exception {
        URI uri = URI.create(baseUrl() + path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json");

        switch (method) {
            case "GET" -> builder.GET();
            case "DELETE" -> builder.DELETE();
            case "POST" -> builder
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            case "PUT" -> builder
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        HttpResponse<String> response = client.send(
            builder.build(),
            HttpResponse.BodyHandlers.ofString()
        );
        return new ApiResponse(response.statusCode(), response.body());
    }

    private String baseUrl() {
        String host = hostField.getText().trim();
        if (host.isEmpty()) host = "127.0.0.1";
        return "http://" + host + ":" + parsePort();
    }

    private int parsePort() {
        int port = Integer.parseInt(portField.getText().trim());
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535.");
        }
        return port;
    }

    private static String encodePath(String value) {
        return value.replace(" ", "%20");
    }

    private static void appendResponse(StringBuilder out, String label, ApiResponse response) {
        out.append(label)
           .append(" -> HTTP ")
           .append(response.statusCode())
           .append('\n')
           .append(prettyJson(response.body()))
           .append("\n\n");
    }

    private static String prettyJson(String json) {
        if (json == null || json.isBlank()) return "(empty response)";

        StringBuilder out = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        int indent = 0;

        for (char c : json.trim().toCharArray()) {
            if (escaped) {
                out.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\' && inString) {
                out.append(c);
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                out.append(c);
                continue;
            }
            if (inString) {
                out.append(c);
                continue;
            }

            switch (c) {
                case '{', '[' -> {
                    out.append(c).append('\n');
                    indent++;
                    appendIndent(out, indent);
                }
                case '}', ']' -> {
                    out.append('\n');
                    indent = Math.max(0, indent - 1);
                    appendIndent(out, indent);
                    out.append(c);
                }
                case ',' -> {
                    out.append(c).append('\n');
                    appendIndent(out, indent);
                }
                case ':' -> out.append(": ");
                default -> {
                    if (!Character.isWhitespace(c)) out.append(c);
                }
            }
        }
        return out.toString();
    }

    private static void appendIndent(StringBuilder out, int indent) {
        out.append("  ".repeat(Math.max(0, indent)));
    }

    private void appendFromWorker(String message) {
        SwingUtilities.invokeLater(() -> append(message));
    }

    private void append(String message) {
        outputArea.append(message + System.lineSeparator());
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    private void showError(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        if (message == null || message.isBlank()) message = cause.getClass().getSimpleName();
        JOptionPane.showMessageDialog(this, message, "REST API Error", JOptionPane.ERROR_MESSAGE);
        append("ERROR: " + message);
    }

    private record ApiResponse(int statusCode, String body) {
    }
}
