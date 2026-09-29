package school.service;

import school.util.AppLogger;
import school.util.DatabaseManager;

import java.sql.*;

public class StudentDatabase {

    public static Connection connect() throws SQLException {
        try {
            return DatabaseManager.getInstance().getConnection();
        } catch (SQLException e) {
            AppLogger.error(
                "DATABASE_ERROR",
                "Database connection failed.",
                e
            );
            throw e;
        }
    }

    public static int addDepartment(String name) throws SQLException {

        String departmentName = name == null ? "" : name.trim();

        if (departmentName.isEmpty()) {
            throw new IllegalArgumentException(
                "Department name cannot be empty."
            );
        }

        try (Connection connection = connect()) {

            String search = """
                SELECT department_id
                FROM departments
                WHERE department_name = ?
                """;

            try (PreparedStatement statement =
                     connection.prepareStatement(search)) {

                statement.setString(1, departmentName);

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return result.getInt("department_id");
                    }
                }
            }

            String insert = """
                INSERT INTO departments (department_name)
                VALUES (?)
                """;

            try (PreparedStatement statement =
                     connection.prepareStatement(
                         insert,
                         Statement.RETURN_GENERATED_KEYS
                     )) {

                statement.setString(1, departmentName);
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        }

        throw new SQLException("Department could not be created.");
    }

    public static void addStudent(
            String id,
            String firstName,
            String lastName,
            String email,
            int departmentId,
            int level) throws SQLException {

        try (Connection connection = connect()) {

            String check = """
                SELECT 1 FROM students
                WHERE student_id = ?
                """;

            try (PreparedStatement statement =
                     connection.prepareStatement(check)) {

                statement.setString(1, id);

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        System.out.println("Student ID already exists.");
                        return;
                    }
                }
            }

            String sql = """
                INSERT INTO students
                (student_id, first_name, last_name,
                 email, department_id, level)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

            try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

                statement.setString(1, id);
                statement.setString(2, firstName);
                statement.setString(3, lastName);

                if (email == null || email.isBlank()) {
                    statement.setNull(4, Types.VARCHAR);
                } else {
                    statement.setString(4, email);
                }

                statement.setInt(5, departmentId);
                statement.setInt(6, level);
                statement.executeUpdate();

                AppLogger.info(
                    "STUDENT_REGISTERED",
                    "Student registered through service: " + id
                );

                System.out.println("Student registered successfully.");
            }
        }
    }

    public static void displayStudents() throws SQLException {

        String sql = """
            SELECT s.student_id, s.first_name, s.last_name,
                   s.email, s.level, d.department_name
            FROM students s
            LEFT JOIN departments d
            ON s.department_id = d.department_id
            ORDER BY s.student_id
            """;

        try (Connection connection = connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            System.out.println("\nSTUDENT LIST");

            while (result.next()) {

                System.out.println(
                    result.getString("student_id") + " | " +
                    result.getString("first_name") + " " +
                    result.getString("last_name") + " | " +
                    result.getString("department_name") + " | Level " +
                    result.getInt("level")
                );
            }
        }
    }

    public static void searchStudent(String id) throws SQLException {

        String sql = """
            SELECT * FROM students
            WHERE student_id = ?
            """;

        try (Connection connection = connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    System.out.println("Student Found");
                    System.out.println("ID: " +
                        result.getString("student_id"));

                    System.out.println("Name: " +
                        result.getString("first_name") + " " +
                        result.getString("last_name"));

                    System.out.println("Email: " +
                        result.getString("email"));

                    System.out.println("Level: " +
                        result.getInt("level"));

                } else {
                    System.out.println("Student not found.");
                }
            }
        }
    }

    public static void updateStudent(
            String id, int level) throws SQLException {

        String sql = """
            UPDATE students
            SET level = ?
            WHERE student_id = ?
            """;

        try (Connection connection = connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setInt(1, level);
            statement.setString(2, id);

            int rows = statement.executeUpdate();

            System.out.println(rows > 0
                ? "Student updated successfully."
                : "Student not found.");
        }
    }

    public static void deleteStudent(String id) throws SQLException {

        String sql = "DELETE FROM students WHERE student_id = ?";

        try (Connection connection = connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, id);

            int rows = statement.executeUpdate();

            System.out.println(rows > 0
                ? "Student deleted successfully."
                : "Student not found.");
        }
    }

    public static void main(String[] args) {

        try {

            int departmentId =
                addDepartment("Software Engineering");

            addStudent(
                "DEMO001",
                "Test",
                "Student",
                "demo001@example.com",
                departmentId,
                100
            );

            displayStudents();

            searchStudent("DEMO001");

            updateStudent("DEMO001", 200);

            searchStudent("DEMO001");

            deleteStudent("DEMO001");

            System.out.println("Database CRUD test completed.");

        } catch (SQLException e) {

            System.out.println("Database error: " + e.getMessage());
        }
    }
}
