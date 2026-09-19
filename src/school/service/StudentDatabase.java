package school.service;

import java.sql.*;

public class StudentDatabase {

    private static final String URL = "jdbc:sqlite:data/school.db";

    public static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }

        return connection;
    }

    public static int addDepartment(String name) throws SQLException {

        try (Connection connection = connect()) {

            String insert = """
                INSERT OR IGNORE INTO departments
                (department_name) VALUES (?)
                """;

            try (PreparedStatement statement =
                     connection.prepareStatement(insert)) {

                statement.setString(1, name);
                statement.executeUpdate();
            }

            String search = """
                SELECT department_id FROM departments
                WHERE department_name = ?
                """;

            try (PreparedStatement statement =
                     connection.prepareStatement(search)) {

                statement.setString(1, name);

                try (ResultSet result = statement.executeQuery()) {

                    if (result.next()) {
                        return result.getInt("department_id");
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

        String sql = """
            INSERT INTO students
            (student_id, first_name, last_name,
             email, department_id, level)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(student_id) DO NOTHING
            """;

        try (Connection connection = connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, id);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setString(4, email);
            statement.setInt(5, departmentId);
            statement.setInt(6, level);

            int rows = statement.executeUpdate();

            if (rows > 0) {
                System.out.println("Student registered successfully.");
            } else {
                System.out.println("Student ID already exists.");
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