package school.model.user;

public final class TeacherUser implements SystemUser {

    private final String username;
    private final String teacherId;

    public TeacherUser(String username, String teacherId) {
        if (teacherId == null || teacherId.isBlank()) {
            throw new IllegalArgumentException("Teacher profile ID is required.");
        }
        this.username = username;
        this.teacherId = teacherId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getRole() {
        return "TEACHER";
    }

    @Override
    public String getProfileId() {
        return teacherId;
    }
}
