package school.model.user;

public final class AdminUser implements SystemUser {

    private final String username;

    public AdminUser(String username) {
        this.username = username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String getProfileId() {
        return null;
    }
}
