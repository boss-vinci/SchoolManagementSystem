package school.main;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import school.exception.SchoolExceptions.StudentNotFoundException;
import school.model.SchoolData.Gender;
import school.model.SchoolData.Student;
import school.model.SchoolData.StudentLevel;
import school.service.Repository;

import static org.junit.jupiter.api.Assertions.*;

class StudentRepositoryTest {

    private Repository<Student> students;
    private Student student;

    @BeforeEach
    void setUp() {
        students = new Repository.InMemory<>();
        student = new Student(
            "TEST001",
            "JUnit Student",
            Gender.MALE,
            StudentLevel.LEVEL_100
        );
    }

    @Test
    void registersAndSearchesStudentSuccessfully()
            throws StudentNotFoundException {

        students.save(student);

        Student found = SchoolDemo.findStudent(students, "TEST001");

        assertNotNull(found);
        assertEquals("TEST001", found.getId());
        assertEquals("JUnit Student", found.getName());
        assertEquals(StudentLevel.LEVEL_100, found.getLevel());
    }

    @Test
    void duplicateStudentIdIsRejected() {
        students.save(student);

        Student duplicate = new Student(
            "TEST001",
            "Duplicate Student",
            Gender.FEMALE,
            StudentLevel.LEVEL_200
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> students.save(duplicate)
        );
    }

    @Test
    void missingStudentThrowsCustomException() {
        assertThrows(
            StudentNotFoundException.class,
            () -> SchoolDemo.findStudent(students, "MISSING")
        );
    }
}
