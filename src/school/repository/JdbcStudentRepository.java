package school.repository;

import school.model.StudentRecord;
import school.service.StudentDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of the StudentRepository abstraction.
 */
public final class JdbcStudentRepository implements StudentRepository {

    private static final String SELECT_BASE = """
        SELECT s.student_id, s.first_name, s.last_name,
               s.email, d.department_name, s.level
        FROM students s
        LEFT JOIN departments d
        ON s.department_id = d.department_id
        """;

    @Override
    public List<StudentRecord> findAll() throws SQLException {

        String sql = SELECT_BASE + " ORDER BY s.student_id";
        List<StudentRecord> students = new ArrayList<>();

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                students.add(map(result));
            }
        }

        return students;
    }

    @Override
    public Optional<StudentRecord> findById(String studentId)
            throws SQLException {

        String sql = SELECT_BASE + " WHERE s.student_id = ?";

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);

            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                    ? Optional.of(map(result))
                    : Optional.empty();
            }
        }
    }

    @Override
    public void save(StudentRecord student) throws SQLException {

        int departmentId =
            StudentDatabase.addDepartment(student.getDepartment());

        String sql = """
            INSERT INTO students
            (student_id, first_name, last_name,
             email, department_id, level)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, student.getStudentId());
            statement.setString(2, student.getFirstName());
            statement.setString(3, student.getLastName());

            if (student.getEmail() == null || student.getEmail().isBlank()) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, student.getEmail());
            }

            statement.setInt(5, departmentId);
            statement.setInt(6, student.getLevel());
            statement.executeUpdate();
        }
    }

    @Override
    public boolean updateLevel(String studentId, int level)
            throws SQLException {

        String sql = """
            UPDATE students
            SET level = ?
            WHERE student_id = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, level);
            statement.setString(2, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(String studentId) throws SQLException {

        String sql = "DELETE FROM students WHERE student_id = ?";

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, studentId);
            return statement.executeUpdate() > 0;
        }
    }

    private StudentRecord map(ResultSet result) throws SQLException {
        return new StudentRecord(
            result.getString("student_id"),
            result.getString("first_name"),
            result.getString("last_name"),
            result.getString("email"),
            result.getString("department_name"),
            result.getInt("level")
        );
    }
}
