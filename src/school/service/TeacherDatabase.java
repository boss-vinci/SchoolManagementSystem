package school.service;

import java.sql.*;

public class TeacherDatabase {

    public static void addTeacher(
            String id, String firstName,
            String lastName, String email,
            int departmentId) throws SQLException {

        String sql = """
            INSERT INTO teachers
            (teacher_id, first_name, last_name,
             email, department_id)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(teacher_id) DO NOTHING
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, id);
            statement.setString(2, firstName);
            statement.setString(3, lastName);
            statement.setString(4, email);
            statement.setInt(5, departmentId);

            int rows = statement.executeUpdate();

            System.out.println(rows > 0
                ? "Teacher registered successfully."
                : "Teacher already exists.");
        }
    }

    public static void displayTeachers() throws SQLException {

        String sql = """
            SELECT t.teacher_id, t.first_name,
                   t.last_name, t.email,
                   d.department_name
            FROM teachers t
            LEFT JOIN departments d
            ON t.department_id = d.department_id
            ORDER BY t.teacher_id
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            System.out.println("\nTEACHER LIST");

            while (result.next()) {

                System.out.println(
                    result.getString("teacher_id") + " | " +
                    result.getString("first_name") + " " +
                    result.getString("last_name") + " | " +
                    result.getString("email") + " | " +
                    result.getString("department_name")
                );
            }
        }
    }

    public static void updateTeacher(
            String id, String email) throws SQLException {

        String sql = """
            UPDATE teachers
            SET email = ?
            WHERE teacher_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, email);
            statement.setString(2, id);

            int rows = statement.executeUpdate();

            System.out.println(rows > 0
                ? "Teacher updated successfully."
                : "Teacher not found.");
        }
    }

    public static void deleteTeacher(String id)
            throws SQLException {

        String sql = """
            DELETE FROM teachers
            WHERE teacher_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, id);

            int rows = statement.executeUpdate();

            System.out.println(rows > 0
                ? "Teacher deleted successfully."
                : "Teacher not found.");
        }
    }

    public static void main(String[] args) {

        try {

            int departmentId =
                StudentDatabase.addDepartment(
                    "Software Engineering"
                );

            addTeacher(
                "DEMO-T001",
                "John",
                "Smith",
                "john.demo@example.com",
                departmentId
            );

            displayTeachers();

            updateTeacher(
                "DEMO-T001",
                "john.updated@example.com"
            );

            displayTeachers();

            deleteTeacher("DEMO-T001");

            System.out.println(
                "\nTeacher management test completed."
            );

        } catch (SQLException e) {

            System.out.println(
                "Database error: " + e.getMessage()
            );
        }
    }
} 