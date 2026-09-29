package school.model.user;

public final class StudentUser implements SystemUser {

    private final String username;
    private final String studentId;

    public StudentUser(String username, String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student profile ID is required.");
        }
        this.username = username;
        this.studentId = studentId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getRole() {
        return "STUDENT";
    }

    @Override
    public String getProfileId() {
        return studentId;
    }
}
