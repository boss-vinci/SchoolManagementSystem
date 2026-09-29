package school.factory;

import school.model.user.AccountantUser;
import school.model.user.AdminUser;
import school.model.user.StudentUser;
import school.model.user.SystemUser;
import school.model.user.TeacherUser;

import java.util.Locale;

/**
 * Factory pattern: creates the correct user object for a role.
 */
public final class UserFactory {

    private UserFactory() {
    }

    public static SystemUser create(
            String username,
            String role,
            String profileId) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }

        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("User role is required.");
        }

        return switch (role.trim().toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> new AdminUser(username);
            case "TEACHER" -> new TeacherUser(username, profileId);
            case "STUDENT" -> new StudentUser(username, profileId);
            case "ACCOUNTANT" -> new AccountantUser(username);
            default -> throw new IllegalArgumentException(
                "Unsupported user role: " + role
            );
        };
    }
}
