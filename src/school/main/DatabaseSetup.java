package school.main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatabaseSetup {

    public static void main(String[] args) {

        try {

            Files.createDirectories(Path.of("data"));

            Connection connection = DriverManager.getConnection(
                "jdbc:sqlite:data/school.db"
            );

            Statement statement = connection.createStatement();

            statement.execute("PRAGMA foreign_keys = ON");

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS departments (
                    department_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    department_name TEXT NOT NULL UNIQUE
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS students (
                    student_id TEXT PRIMARY KEY,
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL,
                    gender TEXT,
                    date_of_birth TEXT,
                    email TEXT UNIQUE,
                    phone TEXT,
                    department_id INTEGER,
                    level INTEGER,
                    FOREIGN KEY (department_id)
                    REFERENCES departments(department_id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS teachers (
                    teacher_id TEXT PRIMARY KEY,
                    first_name TEXT NOT NULL,
                    last_name TEXT NOT NULL,
                    email TEXT UNIQUE,
                    department_id INTEGER,
                    FOREIGN KEY (department_id)
                    REFERENCES departments(department_id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS courses (
                    course_id TEXT PRIMARY KEY,
                    course_name TEXT NOT NULL,
                    department_id INTEGER,
                    teacher_id TEXT,
                    FOREIGN KEY (department_id)
                    REFERENCES departments(department_id),
                    FOREIGN KEY (teacher_id)
                    REFERENCES teachers(teacher_id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS enrollments (
                    student_id TEXT NOT NULL,
                    course_id TEXT NOT NULL,
                    PRIMARY KEY (student_id, course_id),
                    FOREIGN KEY (student_id)
                    REFERENCES students(student_id),
                    FOREIGN KEY (course_id)
                    REFERENCES courses(course_id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS results (
                    result_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id TEXT NOT NULL,
                    course_id TEXT NOT NULL,
                    score REAL NOT NULL CHECK(score BETWEEN 0 AND 100),
                    grade TEXT,
                    FOREIGN KEY (student_id, course_id)
                    REFERENCES enrollments(student_id, course_id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS payments (
                    payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id TEXT NOT NULL,
                    amount REAL NOT NULL CHECK(amount > 0),
                    payment_date TEXT NOT NULL,
                    payment_method TEXT,
                    FOREIGN KEY (student_id)
                    REFERENCES students(student_id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS attendance (
                    attendance_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id TEXT NOT NULL,
                    course_id TEXT NOT NULL,
                    attendance_date TEXT NOT NULL,
                    status TEXT NOT NULL,
                    FOREIGN KEY (student_id, course_id)
                    REFERENCES enrollments(student_id, course_id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(
                        role IN ('ADMIN', 'TEACHER',
                                 'STUDENT', 'ACCOUNTANT')
                    )
                )
            """);

            statement.close();
            connection.close();

            System.out.println("Database created successfully!");
            System.out.println("All 9 database tables created.");

        } catch (SQLException | java.io.IOException e) {

            System.out.println("Database error: " + e.getMessage());

        }
    }
} 