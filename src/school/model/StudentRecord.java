package school.model;

/**
 * Model used by the student-management MVC flow.
 */
public final class StudentRecord {

    private final String studentId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String department;
    private final int level;

    public StudentRecord(
            String studentId,
            String firstName,
            String lastName,
            String email,
            String department,
            int level) {

        this.studentId = studentId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.level = level;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public int getLevel() {
        return level;
    }
}
