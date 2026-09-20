package school.main;

import school.service.StudentDatabase;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class RoleDatabaseSetup {

    public static void main(String[] args) {

        String sql = """
            CREATE TABLE IF NOT EXISTS account_profiles (
                user_id INTEGER PRIMARY KEY,
                student_id TEXT UNIQUE,
                teacher_id TEXT UNIQUE,

                FOREIGN KEY (user_id)
                REFERENCES users(user_id)
                ON DELETE CASCADE,

                FOREIGN KEY (student_id)
                REFERENCES students(student_id)
                ON DELETE RESTRICT,

                FOREIGN KEY (teacher_id)
                REFERENCES teachers(teacher_id)
                ON DELETE RESTRICT,

                CHECK (
                    student_id IS NULL
                    OR teacher_id IS NULL
                )
            )
            """;

        try (Connection connection = StudentDatabase.connect();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);

            System.out.println(
                "Account profiles table created successfully."
            );

            System.out.println(
                "Student and teacher account relationships ready."
            );

            System.out.println(
                "Existing administrator account preserved."
            );

        } catch (SQLException e) {

            System.out.println(
                "Database error: " + e.getMessage()
            );
        }
    } 
} 