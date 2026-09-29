package school.controller;

import school.model.StudentRecord;
import school.repository.JdbcStudentRepository;
import school.service.StudentService;

import java.sql.SQLException;
import java.util.List;

/**
 * MVC controller for the student-management screen.
 */
public final class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    public static StudentController createDefault() {
        return new StudentController(
            new StudentService(new JdbcStudentRepository())
        );
    }

    public List<StudentRecord> getStudents() throws SQLException {
        return service.getAllStudents();
    }

    public void registerStudent(
            String id,
            String firstName,
            String lastName,
            String email,
            String department,
            int level) throws SQLException {

        service.registerStudent(
            id,
            firstName,
            lastName,
            email,
            department,
            level
        );
    }

    public void updateStudentLevel(String studentId, int level)
            throws SQLException {
        service.updateLevel(studentId, level);
    }

    public void deleteStudent(String studentId) throws SQLException {
        service.deleteStudent(studentId);
    }
}
