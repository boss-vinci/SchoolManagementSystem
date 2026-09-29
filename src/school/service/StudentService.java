package school.service;

import school.model.StudentRecord;
import school.repository.StudentRepository;
import school.util.AppLogger;

import java.sql.SQLException;
import java.util.List;

/**
 * Service Layer pattern: contains student business rules and validation.
 */
public final class StudentService {

    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public List<StudentRecord> getAllStudents() throws SQLException {
        return repository.findAll();
    }

    public void registerStudent(
            String id,
            String firstName,
            String lastName,
            String email,
            String department,
            int level) throws SQLException {

        String cleanId = required(id, "Student ID");
        String cleanFirst = required(firstName, "First name");
        String cleanLast = required(lastName, "Last name");
        String cleanDepartment = required(department, "Department");
        String cleanEmail = email == null ? "" : email.trim();

        validateLevel(level);

        if (repository.findById(cleanId).isPresent()) {
            throw new IllegalArgumentException(
                "Student ID already exists: " + cleanId
            );
        }

        repository.save(new StudentRecord(
            cleanId,
            cleanFirst,
            cleanLast,
            cleanEmail,
            cleanDepartment,
            level
        ));

        AppLogger.info(
            "STUDENT_REGISTERED",
            "Student registered: " + cleanId
        );
    }

    public void updateLevel(String studentId, int level)
            throws SQLException {

        String cleanId = required(studentId, "Student ID");
        validateLevel(level);

        if (!repository.updateLevel(cleanId, level)) {
            throw new IllegalArgumentException("Student not found: " + cleanId);
        }

        AppLogger.info(
            "STUDENT_UPDATED",
            "Student level updated: " + cleanId + " -> " + level
        );
    }

    public void deleteStudent(String studentId) throws SQLException {

        String cleanId = required(studentId, "Student ID");

        if (!repository.deleteById(cleanId)) {
            throw new IllegalArgumentException("Student not found: " + cleanId);
        }

        AppLogger.info(
            "STUDENT_DELETED",
            "Student deleted: " + cleanId
        );
    }

    private static String required(String value, String label) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return cleaned;
    }

    private static void validateLevel(int level) {
        if (level < 100 || level > 500 || level % 100 != 0) {
            throw new IllegalArgumentException(
                "Level must be 100, 200, 300, 400 or 500."
            );
        }
    }
}
