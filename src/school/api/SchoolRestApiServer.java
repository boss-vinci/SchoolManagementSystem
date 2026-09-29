package school.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import school.service.StudentDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small JSON REST API for the School Management System.
 *
 * Endpoints:
 * GET    /students
 * GET    /students/{id}
 * POST   /students
 * PUT    /students/{id}
 * DELETE /students/{id}
 * GET    /courses
 * GET    /results
 * GET    /payments
 */
public final class SchoolRestApiServer {

    public static final int DEFAULT_PORT = 8080;

    private static final Pattern JSON_ENTRY = Pattern.compile(
        "\\\"((?:\\\\.|[^\\\"])*)\\\"\\s*:\\s*(\\\"(?:\\\\.|[^\\\"])*\\\"|-?\\d+(?:\\.\\d+)?|true|false|null)",
        Pattern.CASE_INSENSITIVE
    );

    private static HttpServer server;
    private static ExecutorService executor;
    private static Consumer<String> logger = System.out::println;

    private SchoolRestApiServer() {
    }

    public static synchronized void start(int port, Consumer<String> logConsumer) throws IOException {
        if (server != null) {
            log("REST API is already running on port " + server.getAddress().getPort() + ".");
            return;
        }

        logger = logConsumer == null ? System.out::println : logConsumer;
        HttpServer newServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        executor = Executors.newFixedThreadPool(6);
        newServer.setExecutor(executor);

        newServer.createContext("/health", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                methodNotAllowed(exchange, "GET");
                return;
            }
            sendJson(exchange, 200, "{\"status\":\"UP\",\"service\":\"School Management REST API\"}");
        });
        newServer.createContext("/students", new StudentsHandler());
        newServer.createContext("/courses", exchange -> handleCollectionGet(exchange, "courses"));
        newServer.createContext("/results", exchange -> handleCollectionGet(exchange, "results"));
        newServer.createContext("/payments", exchange -> handleCollectionGet(exchange, "payments"));

        newServer.start();
        server = newServer;
        log("REST API started at http://127.0.0.1:" + port);
        log("Available endpoints: /students, /courses, /results, /payments");
    }

    public static synchronized void stop() {
        if (server == null) {
            log("REST API is not running.");
            return;
        }

        server.stop(0);
        server = null;

        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        log("REST API stopped.");
    }

    public static synchronized boolean isRunning() {
        return server != null;
    }

    private static final class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                addCommonHeaders(exchange);

                if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }

                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod().toUpperCase();

                if ("/students".equals(path) || "/students/".equals(path)) {
                    switch (method) {
                        case "GET" -> getStudents(exchange);
                        case "POST" -> createStudent(exchange);
                        default -> methodNotAllowed(exchange, "GET, POST");
                    }
                    return;
                }

                if (!path.startsWith("/students/")) {
                    sendError(exchange, 404, "Endpoint not found.");
                    return;
                }

                String rawId = path.substring("/students/".length());
                if (rawId.isBlank() || rawId.contains("/")) {
                    sendError(exchange, 404, "Student endpoint not found.");
                    return;
                }

                String studentId = URLDecoder.decode(rawId, StandardCharsets.UTF_8);
                switch (method) {
                    case "GET" -> getStudent(exchange, studentId);
                    case "PUT" -> updateStudent(exchange, studentId);
                    case "DELETE" -> deleteStudent(exchange, studentId);
                    default -> methodNotAllowed(exchange, "GET, PUT, DELETE");
                }
            } catch (IllegalArgumentException e) {
                sendError(exchange, 400, e.getMessage());
            } catch (SQLException e) {
                log("Database error: " + e.getMessage());
                sendError(exchange, 500, "Database error: " + safeMessage(e));
            } catch (Exception e) {
                log("REST error: " + e.getMessage());
                sendError(exchange, 500, "Server error: " + safeMessage(e));
            }
        }
    }

    private static void getStudents(HttpExchange exchange) throws SQLException, IOException {
        String sql = """
            SELECT s.student_id, s.first_name, s.last_name, s.email,
                   d.department_name, s.level
            FROM students s
            LEFT JOIN departments d ON d.department_id = s.department_id
            ORDER BY s.student_id
            """;

        StringBuilder json = new StringBuilder("[");
        int count = 0;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                if (count++ > 0) json.append(',');
                json.append(studentJson(rs));
            }
        }

        json.append(']');
        sendJson(exchange, 200, json.toString());
        log("GET /students -> 200 (" + count + " records)");
    }

    private static void getStudent(HttpExchange exchange, String studentId) throws SQLException, IOException {
        try (Connection connection = StudentDatabase.connect()) {
            StudentRecord student = findStudent(connection, studentId);
            if (student == null) {
                sendError(exchange, 404, "Student not found: " + studentId);
                return;
            }

            sendJson(exchange, 200, student.toJson());
            log("GET /students/" + studentId + " -> 200");
        }
    }

    private static void createStudent(HttpExchange exchange) throws IOException, SQLException {
        Map<String, String> body = parseJsonObject(readBody(exchange));

        String studentId = required(body, "studentId");
        String firstName = required(body, "firstName");
        String lastName = required(body, "lastName");
        String department = required(body, "department");
        int level = parseLevel(required(body, "level"));
        String email = blankToNull(body.get("email"));

        try (Connection connection = StudentDatabase.connect()) {
            if (studentExists(connection, studentId)) {
                sendError(exchange, 409, "Student already exists: " + studentId);
                return;
            }
        }

        int departmentId = StudentDatabase.addDepartment(department);

        String sql = """
            INSERT INTO students
            (student_id, first_name, last_name, email, department_id, level)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            if (email == null) statement.setNull(4, Types.VARCHAR);
            else statement.setString(4, email);
            statement.setInt(5, departmentId);
            statement.setInt(6, level);
            statement.executeUpdate();
        }

        try (Connection connection = StudentDatabase.connect()) {
            StudentRecord created = findStudent(connection, studentId);
            sendJson(exchange, 201, created.toJson());
        }
        log("POST /students -> 201 (" + studentId + ")");
    }

    private static void updateStudent(HttpExchange exchange, String studentId) throws IOException, SQLException {
        Map<String, String> body = parseJsonObject(readBody(exchange));

        try (Connection connection = StudentDatabase.connect()) {
            StudentRecord current = findStudent(connection, studentId);
            if (current == null) {
                sendError(exchange, 404, "Student not found: " + studentId);
                return;
            }

            String firstName = valueOrDefault(body.get("firstName"), current.firstName());
            String lastName = valueOrDefault(body.get("lastName"), current.lastName());
            String email = body.containsKey("email") ? blankToNull(body.get("email")) : current.email();
            String department = valueOrDefault(body.get("department"), current.department());
            int level = body.containsKey("level") ? parseLevel(body.get("level")) : current.level();

            int departmentId = StudentDatabase.addDepartment(department);
            String sql = """
                UPDATE students
                SET first_name = ?, last_name = ?, email = ?, department_id = ?, level = ?
                WHERE student_id = ?
                """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, firstName);
                statement.setString(2, lastName);
                if (email == null) statement.setNull(3, Types.VARCHAR);
                else statement.setString(3, email);
                statement.setInt(4, departmentId);
                statement.setInt(5, level);
                statement.setString(6, studentId);
                statement.executeUpdate();
            }
        }

        try (Connection connection = StudentDatabase.connect()) {
            StudentRecord updated = findStudent(connection, studentId);
            sendJson(exchange, 200, updated.toJson());
        }
        log("PUT /students/" + studentId + " -> 200");
    }

    private static void deleteStudent(HttpExchange exchange, String studentId) throws IOException, SQLException {
        try (Connection connection = StudentDatabase.connect()) {
            if (!studentExists(connection, studentId)) {
                sendError(exchange, 404, "Student not found: " + studentId);
                return;
            }

            String sql = "DELETE FROM students WHERE student_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, studentId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            if (isConstraintViolation(e)) {
                sendError(exchange, 409,
                    "Student cannot be deleted while related enrollments, results, payments, attendance or accounts exist.");
                return;
            }
            throw e;
        }

        sendJson(exchange, 200, "{\"message\":\"Student deleted successfully.\",\"studentId\":" + jsonString(studentId) + "}");
        log("DELETE /students/" + studentId + " -> 200");
    }

    private static void handleCollectionGet(HttpExchange exchange, String resource) throws IOException {
        try {
            addCommonHeaders(exchange);
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                methodNotAllowed(exchange, "GET");
                return;
            }

            switch (resource) {
                case "courses" -> getCourses(exchange);
                case "results" -> getResults(exchange);
                case "payments" -> getPayments(exchange);
                default -> sendError(exchange, 404, "Endpoint not found.");
            }
        } catch (SQLException e) {
            log("Database error: " + e.getMessage());
            sendError(exchange, 500, "Database error: " + safeMessage(e));
        } catch (Exception e) {
            log("REST error: " + e.getMessage());
            sendError(exchange, 500, "Server error: " + safeMessage(e));
        }
    }

    private static void getCourses(HttpExchange exchange) throws SQLException, IOException {
        String sql = """
            SELECT c.course_id, c.course_name, d.department_name,
                   c.teacher_id,
                   COALESCE(t.first_name || ' ' || t.last_name, '') AS teacher_name
            FROM courses c
            LEFT JOIN departments d ON d.department_id = c.department_id
            LEFT JOIN teachers t ON t.teacher_id = c.teacher_id
            ORDER BY c.course_id
            """;

        StringBuilder json = new StringBuilder("[");
        int count = 0;
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                if (count++ > 0) json.append(',');
                json.append('{')
                    .append("\"courseId\":").append(jsonString(rs.getString("course_id"))).append(',')
                    .append("\"courseName\":").append(jsonString(rs.getString("course_name"))).append(',')
                    .append("\"department\":").append(jsonNullable(rs.getString("department_name"))).append(',')
                    .append("\"teacherId\":").append(jsonNullable(rs.getString("teacher_id"))).append(',')
                    .append("\"teacherName\":").append(jsonNullable(blankToNull(rs.getString("teacher_name"))))
                    .append('}');
            }
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
        log("GET /courses -> 200 (" + count + " records)");
    }

    private static void getResults(HttpExchange exchange) throws SQLException, IOException {
        String sql = """
            SELECT r.result_id, r.student_id, r.course_id,
                   c.course_name, r.score, r.grade
            FROM results r
            LEFT JOIN courses c ON c.course_id = r.course_id
            ORDER BY r.result_id
            """;

        StringBuilder json = new StringBuilder("[");
        int count = 0;
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                if (count++ > 0) json.append(',');
                json.append('{')
                    .append("\"resultId\":").append(rs.getInt("result_id")).append(',')
                    .append("\"studentId\":").append(jsonString(rs.getString("student_id"))).append(',')
                    .append("\"courseId\":").append(jsonString(rs.getString("course_id"))).append(',')
                    .append("\"courseName\":").append(jsonNullable(rs.getString("course_name"))).append(',')
                    .append("\"score\":").append(rs.getDouble("score")).append(',')
                    .append("\"grade\":").append(jsonNullable(rs.getString("grade")))
                    .append('}');
            }
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
        log("GET /results -> 200 (" + count + " records)");
    }

    private static void getPayments(HttpExchange exchange) throws SQLException, IOException {
        String sql = """
            SELECT payment_id, student_id, amount, payment_date, payment_method
            FROM payments
            ORDER BY payment_id
            """;

        StringBuilder json = new StringBuilder("[");
        int count = 0;
        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                if (count++ > 0) json.append(',');
                BigDecimal amount = rs.getBigDecimal("amount");
                json.append('{')
                    .append("\"paymentId\":").append(rs.getInt("payment_id")).append(',')
                    .append("\"studentId\":").append(jsonString(rs.getString("student_id"))).append(',')
                    .append("\"amount\":").append(amount == null ? "0" : amount.toPlainString()).append(',')
                    .append("\"paymentDate\":").append(jsonString(rs.getString("payment_date"))).append(',')
                    .append("\"paymentMethod\":").append(jsonNullable(rs.getString("payment_method")))
                    .append('}');
            }
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
        log("GET /payments -> 200 (" + count + " records)");
    }

    private static StudentRecord findStudent(Connection connection, String studentId) throws SQLException {
        String sql = """
            SELECT s.student_id, s.first_name, s.last_name, s.email,
                   d.department_name, s.level
            FROM students s
            LEFT JOIN departments d ON d.department_id = s.department_id
            WHERE s.student_id = ?
            """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                return new StudentRecord(
                    rs.getString("student_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("department_name"),
                    rs.getInt("level")
                );
            }
        }
    }

    private static boolean studentExists(Connection connection, String studentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                 "SELECT 1 FROM students WHERE student_id = ?")) {
            statement.setString(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static String studentJson(ResultSet rs) throws SQLException {
        return new StudentRecord(
            rs.getString("student_id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("department_name"),
            rs.getInt("level")
        ).toJson();
    }

    private record StudentRecord(
        String studentId,
        String firstName,
        String lastName,
        String email,
        String department,
        int level
    ) {
        String toJson() {
            return "{" +
                "\"studentId\":" + jsonString(studentId) + "," +
                "\"firstName\":" + jsonString(firstName) + "," +
                "\"lastName\":" + jsonString(lastName) + "," +
                "\"email\":" + jsonNullable(email) + "," +
                "\"department\":" + jsonNullable(department) + "," +
                "\"level\":" + level +
                "}";
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    private static Map<String, String> parseJsonObject(String json) {
        if (json == null || json.isBlank() || !json.trim().startsWith("{") || !json.trim().endsWith("}")) {
            throw new IllegalArgumentException("Request body must be a JSON object.");
        }

        Map<String, String> values = new LinkedHashMap<>();
        Matcher matcher = JSON_ENTRY.matcher(json);
        while (matcher.find()) {
            String key = unescapeJson(matcher.group(1));
            String raw = matcher.group(2).trim();
            String value;
            if (raw.startsWith("\"") && raw.endsWith("\"")) {
                value = unescapeJson(raw.substring(1, raw.length() - 1));
            } else if ("null".equalsIgnoreCase(raw)) {
                value = null;
            } else {
                value = raw;
            }
            values.put(key, value);
        }

        if (values.isEmpty()) {
            throw new IllegalArgumentException("JSON body does not contain supported fields.");
        }
        return values;
    }

    private static String required(Map<String, String> body, String key) {
        String value = body.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value.trim();
    }

    private static int parseLevel(String raw) {
        try {
            int level = Integer.parseInt(raw.trim());
            if (level < 100 || level > 500 || level % 100 != 0) {
                throw new IllegalArgumentException("Level must be 100, 200, 300, 400 or 500.");
            }
            return level;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Level must be a whole number.");
        }
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean isConstraintViolation(SQLException e) {
        String state = e.getSQLState();
        return state != null && state.startsWith("23");
    }

    private static void methodNotAllowed(HttpExchange exchange, String allowed) throws IOException {
        exchange.getResponseHeaders().set("Allow", allowed);
        sendError(exchange, 405, "Method not allowed. Allowed: " + allowed);
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, "{\"error\":" + jsonString(message == null ? "Unknown error" : message) + "}");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        addCommonHeaders(exchange);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void addCommonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
    }

    private static String jsonNullable(String value) {
        return value == null ? "null" : jsonString(value);
    }

    private static String jsonString(String value) {
        if (value == null) return "null";
        return "\"" + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t") + "\"";
    }

    private static String unescapeJson(String value) {
        return value
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\");
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private static void log(String message) {
        try {
            logger.accept(message);
        } catch (Exception ignored) {
        }
    }
}
