package school.network;

import school.service.StudentDatabase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Simple TCP server for student self-service requests.
 * Protocol examples:
 * PROFILE|ST001
 * RESULTS|ST001
 * COURSES|ST001
 * PAYMENT_STATUS|ST001
 * REGISTER_COURSE|ST001|SEN101
 */
public final class SchoolServer {

    public static final int DEFAULT_PORT = 5050;

    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static ExecutorService acceptor;
    private static ExecutorService clientPool;
    private static ServerSocket serverSocket;
    private static Consumer<String> logger = System.out::println;

    private SchoolServer() {
    }

    public static synchronized void start(int port, Consumer<String> logConsumer) throws IOException {
        if (running.get()) {
            log("Server is already running on port " + serverSocket.getLocalPort() + ".");
            return;
        }

        logger = logConsumer == null ? System.out::println : logConsumer;
        serverSocket = new ServerSocket(port);
        running.set(true);
        acceptor = Executors.newSingleThreadExecutor();
        clientPool = Executors.newFixedThreadPool(4);

        acceptor.submit(() -> {
            log("Server started on port " + port + ". Waiting for student clients...");
            while (running.get()) {
                try {
                    Socket client = serverSocket.accept();
                    clientPool.submit(() -> handleClient(client));
                } catch (IOException e) {
                    if (running.get()) {
                        log("Server error: " + e.getMessage());
                    }
                }
            }
        });
    }

    public static synchronized void stop() {
        if (!running.get()) {
            log("Server is not running.");
            return;
        }

        running.set(false);
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }

        if (acceptor != null) {
            acceptor.shutdownNow();
        }
        if (clientPool != null) {
            clientPool.shutdownNow();
        }

        log("Server stopped.");
    }

    public static boolean isRunning() {
        return running.get();
    }

    private static void handleClient(Socket socket) {
        String remote = String.valueOf(socket.getRemoteSocketAddress());
        log("Client connected: " + remote);

        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            String request = reader.readLine();
            if (request == null || request.isBlank()) {
                sendError(writer, "Empty request.");
                return;
            }

            log("Request received: " + request);
            processRequest(request.trim(), writer);

        } catch (Exception e) {
            log("Client handling error: " + e.getMessage());
        } finally {
            log("Client disconnected: " + remote);
        }
    }

    private static void processRequest(String request, PrintWriter writer) throws SQLException {
        String[] parts = request.split("\\|", -1);
        String command = parts[0].trim().toUpperCase();

        switch (command) {
            case "PING" -> sendResponse(writer, "PONG");
            case "PROFILE" -> {
                requireParts(parts, 2);
                sendResponse(writer, getProfile(parts[1].trim()));
            }
            case "RESULTS" -> {
                requireParts(parts, 2);
                sendResponse(writer, getResults(parts[1].trim()));
            }
            case "COURSES" -> {
                requireParts(parts, 2);
                sendResponse(writer, getCourses(parts[1].trim()));
            }
            case "PAYMENT_STATUS" -> {
                requireParts(parts, 2);
                sendResponse(writer, getPaymentStatus(parts[1].trim()));
            }
            case "REGISTER_COURSE" -> {
                requireParts(parts, 3);
                sendResponse(writer, registerCourse(parts[1].trim(), parts[2].trim()));
            }
            default -> sendError(writer, "Unknown command: " + command);
        }
    }

    private static void requireParts(String[] parts, int expected) {
        if (parts.length < expected) {
            throw new IllegalArgumentException("Invalid request format.");
        }
    }

    private static String getProfile(String studentId) throws SQLException {
        String sql = """
            SELECT s.student_id, s.first_name, s.last_name, s.email,
                   s.level, d.department_name
            FROM students s
            LEFT JOIN departments d ON d.department_id = s.department_id
            WHERE s.student_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return "Student not found: " + studentId;
                }
                return "Student ID: " + rs.getString("student_id") + "\n" +
                       "Name: " + rs.getString("first_name") + " " + rs.getString("last_name") + "\n" +
                       "Email: " + valueOrDash(rs.getString("email")) + "\n" +
                       "Department: " + valueOrDash(rs.getString("department_name")) + "\n" +
                       "Level: " + rs.getInt("level");
            }
        }
    }

    private static String getResults(String studentId) throws SQLException {
        String sql = """
            SELECT r.course_id, c.course_name, r.score, r.grade
            FROM results r
            JOIN courses c ON c.course_id = r.course_id
            WHERE r.student_id = ?
            ORDER BY r.course_id
            """;

        StringBuilder out = new StringBuilder("Results for ").append(studentId).append(':');
        int count = 0;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    count++;
                    out.append("\n")
                       .append(rs.getString("course_id")).append(" - ")
                       .append(rs.getString("course_name")).append(": ")
                       .append(rs.getDouble("score")).append(" (")
                       .append(valueOrDash(rs.getString("grade"))).append(')');
                }
            }
        }

        if (count == 0) {
            out.append("\nNo results found.");
        }
        return out.toString();
    }

    private static String getCourses(String studentId) throws SQLException {
        String sql = """
            SELECT c.course_id, c.course_name
            FROM enrollments e
            JOIN courses c ON c.course_id = e.course_id
            WHERE e.student_id = ?
            ORDER BY c.course_id
            """;

        StringBuilder out = new StringBuilder("Registered courses for ").append(studentId).append(':');
        int count = 0;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    count++;
                    out.append("\n")
                       .append(rs.getString("course_id")).append(" - ")
                       .append(rs.getString("course_name"));
                }
            }
        }

        if (count == 0) {
            out.append("\nNo registered courses found.");
        }
        return out.toString();
    }

    private static String getPaymentStatus(String studentId) throws SQLException {
        String sql = """
            SELECT COUNT(*) AS payment_count,
                   COALESCE(SUM(amount), 0) AS total_paid
            FROM payments
            WHERE student_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return "Payment status for " + studentId + "\n" +
                       "Payments recorded: " + rs.getInt("payment_count") + "\n" +
                       "Total paid: " + rs.getBigDecimal("total_paid");
            }
        }
    }

    private static String registerCourse(String studentId, String courseId) throws SQLException {
        try (Connection connection = StudentDatabase.connect()) {
            if (!exists(connection, "SELECT 1 FROM students WHERE student_id = ?", studentId)) {
                return "Student not found: " + studentId;
            }
            if (!exists(connection, "SELECT 1 FROM courses WHERE course_id = ?", courseId)) {
                return "Course not found: " + courseId;
            }

            String duplicateSql = "SELECT 1 FROM enrollments WHERE student_id = ? AND course_id = ?";
            try (PreparedStatement check = connection.prepareStatement(duplicateSql)) {
                check.setString(1, studentId);
                check.setString(2, courseId);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        return studentId + " is already registered for " + courseId + ".";
                    }
                }
            }

            String insert = "INSERT INTO enrollments(student_id, course_id) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setString(1, studentId);
                statement.setString(2, courseId);
                statement.executeUpdate();
            }

            return "Course registration successful: " + studentId + " -> " + courseId;
        }
    }

    private static boolean exists(Connection connection, String sql, String value) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void sendResponse(PrintWriter writer, String response) {
        writer.println("OK");
        for (String line : response.split("\\R", -1)) {
            writer.println(line);
        }
        writer.println("END");
    }

    private static void sendError(PrintWriter writer, String message) {
        writer.println("ERROR");
        writer.println(message);
        writer.println("END");
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static void log(String message) {
        try {
            logger.accept(message);
        } catch (Exception ignored) {
        }
    }
}
