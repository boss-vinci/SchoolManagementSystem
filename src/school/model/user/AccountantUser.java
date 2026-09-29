package school.model.user;

public final class AccountantUser implements SystemUser {

    private final String username;

    public AccountantUser(String username) {
        this.username = username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getRole() {
        return "ACCOUNTANT";
    }

    @Override
    public String getProfileId() {
        return null;
    }
}
