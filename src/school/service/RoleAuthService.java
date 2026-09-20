package school.service;

import java.sql.*;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class RoleAuthService {

    private static final int ITERATIONS = 210000;

    public static final class Session {

        private final String username;
        private final String role;
        private final String profileId;

        private Session(String username, String role, String profileId) {
            this.username = username;
            this.role = role;
            this.profileId = profileId;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }

        public String getProfileId() {
            return profileId;
        }

        public boolean isAdmin() {
            return role.equals("ADMIN");
        }
    }

    private static byte[] hash(char[] password, byte[] salt,
                               int iterations)
            throws GeneralSecurityException {

        PBEKeySpec spec = new PBEKeySpec(
            password, salt, iterations, 256
        );

        try {
            SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();

        } finally {
            spec.clearPassword();
        }
    }

    public static Session login(String username, char[] password)
            throws SQLException, GeneralSecurityException {

        if (username == null || password == null) {
            return null;
        }

        String sql = """
            SELECT u.username, u.password_hash, u.role,
                   p.student_id, p.teacher_id
            FROM users u
            LEFT JOIN account_profiles p
            ON u.user_id = p.user_id
            WHERE u.username = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                String stored = result.getString("password_hash");

                if (stored == null) {
                    return null;
                }

                String[] parts = stored.split("\\$", -1);

                if (parts.length != 4 ||
                    !parts[0].equals("pbkdf2")) {
                    return null;
                }

                int iterations;

                try {
                    iterations = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    return null;
                }

                if (iterations < ITERATIONS || iterations > 1000000) {
                    return null;
                }

                byte[] salt;
                byte[] expected;

                try {
                    salt = Base64.getDecoder().decode(parts[2]);
                    expected = Base64.getDecoder().decode(parts[3]);
                } catch (IllegalArgumentException e) {
                    return null;
                }

                byte[] actual = hash(password, salt, iterations);

                boolean valid = MessageDigest.isEqual(expected, actual);

                Arrays.fill(actual, (byte) 0);

                if (!valid) {
                    return null;
                }

                String role = result.getString("role");
                String profileId = null;

                switch (role) {

                    case "ADMIN":
                    case "ACCOUNTANT":
                        break;

                    case "TEACHER":
                        profileId = result.getString("teacher_id");
                        break;

                    case "STUDENT":
                        profileId = result.getString("student_id");
                        break;

                    default:
                        return null;
                }

                if ((role.equals("STUDENT") ||
                     role.equals("TEACHER")) &&
                    profileId == null) {
                    return null;
                }

                return new Session(
                    result.getString("username"),
                    role,
                    profileId
                );
            }
        }
    }

    public static void createAccount(
            Session admin,
            String username,
            char[] password,
            String role,
            String profileId)
            throws SQLException, GeneralSecurityException {

        if (admin == null || !admin.isAdmin()) {
            throw new SecurityException("Administrator access required.");
        }

        if (username == null ||
            !username.matches("[A-Za-z0-9._-]{3,32}")) {

            throw new IllegalArgumentException(
                "Username must contain 3-32 valid characters."
            );
        }

        if (password == null || password.length < 12) {
            throw new IllegalArgumentException(
                "Password must contain at least 12 characters."
            );
        }

        if (!role.equals("STUDENT") &&
            !role.equals("TEACHER") &&
            !role.equals("ACCOUNTANT")) {

            throw new IllegalArgumentException("Invalid account role.");
        }

        String linkedId = profileId == null ? "" : profileId.trim();

        if (role.equals("ACCOUNTANT")) {

            if (!linkedId.isEmpty()) {
                throw new IllegalArgumentException(
                    "Accountants do not require a student or teacher ID."
                );
            }

        } else if (linkedId.isEmpty()) {

            throw new IllegalArgumentException(
                "A matching student or teacher ID is required."
            );
        }

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        byte[] passwordHash = hash(password, salt, ITERATIONS);

        String storedHash = "pbkdf2$" + ITERATIONS + "$" +
            Base64.getEncoder().encodeToString(salt) + "$" +
            Base64.getEncoder().encodeToString(passwordHash);

        Arrays.fill(passwordHash, (byte) 0);

        try (Connection connection = StudentDatabase.connect()) {

            connection.setAutoCommit(false);

            try {

                if (!role.equals("ACCOUNTANT")) {

                    String table = role.equals("STUDENT")
                        ? "students" : "teachers";

                    String column = role.equals("STUDENT")
                        ? "student_id" : "teacher_id";

                    String check = "SELECT 1 FROM " + table +
                                   " WHERE " + column + " = ?";

                    try (PreparedStatement statement =
                             connection.prepareStatement(check)) {

                        statement.setString(1, linkedId);

                        try (ResultSet result = statement.executeQuery()) {

                            if (!result.next()) {
                                throw new IllegalArgumentException(
                                    "The linked record does not exist."
                                );
                            }
                        }
                    }
                }

                String insert = """
                    INSERT INTO users
                    (username, password_hash, role)
                    VALUES (?, ?, ?)
                    """;

                try (PreparedStatement statement =
                         connection.prepareStatement(insert)) {

                    statement.setString(1, username);
                    statement.setString(2, storedHash);
                    statement.setString(3, role);

                    statement.executeUpdate();
                }

                int userId;

                try (Statement statement = connection.createStatement();
                     ResultSet result =
                         statement.executeQuery("SELECT last_insert_rowid()")) {

                    result.next();
                    userId = result.getInt(1);
                }

                String profileSql = """
                    INSERT INTO account_profiles
                    (user_id, student_id, teacher_id)
                    VALUES (?, ?, ?)
                    """;

                try (PreparedStatement statement =
                         connection.prepareStatement(profileSql)) {

                    statement.setInt(1, userId);

                    if (role.equals("STUDENT")) {
                        statement.setString(2, linkedId);
                    } else {
                        statement.setNull(2, Types.VARCHAR);
                    }

                    if (role.equals("TEACHER")) {
                        statement.setString(3, linkedId);
                    } else {
                        statement.setNull(3, Types.VARCHAR);
                    }

                    statement.executeUpdate();
                }

                connection.commit();

            } catch (SQLException | RuntimeException e) {

                connection.rollback();
                throw e;
            }
        }
    }
} 