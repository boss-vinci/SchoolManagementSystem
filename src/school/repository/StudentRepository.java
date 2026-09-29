package school.repository;

import school.model.StudentRecord;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository pattern: database operations are separated from the GUI and business rules.
 */
public interface StudentRepository {

    List<StudentRecord> findAll() throws SQLException;

    Optional<StudentRecord> findById(String studentId) throws SQLException;

    void save(StudentRecord student) throws SQLException;

    boolean updateLevel(String studentId, int level) throws SQLException;

    boolean deleteById(String studentId) throws SQLException;
}
