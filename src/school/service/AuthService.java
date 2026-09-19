package school.service;

import java.sql.*;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class AuthService {

    private static final int ITERATIONS = 210000;

    public static boolean hasAdmin() throws SQLException {

        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            return result.next() && result.getInt(1) > 0;
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
                SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256"
                );

            return factory.generateSecret(spec).getEncoded();

        } finally {
            spec.clearPassword();
        }
    }

    public static void createAdmin(char[] password)
            throws SQLException, GeneralSecurityException {

        if (password == null || password.length < 12) {
            throw new IllegalArgumentException(
                "Password must contain at least 12 characters."
            );
        }

        if (hasAdmin()) {
            throw new IllegalStateException(
                "An administrator already exists."
            );
        }

        byte[] salt = new byte[16];

        new SecureRandom().nextBytes(salt);

        byte[] passwordHash = hash(
            password, salt, ITERATIONS
        );

        String storedHash =
            "pbkdf2$" + ITERATIONS + "$" +
            Base64.getEncoder().encodeToString(salt) + "$" +
            Base64.getEncoder().encodeToString(passwordHash);

        String sql = """
            INSERT INTO users (username, password_hash, role)
            VALUES (?, ?, ?)
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, "admin");
            statement.setString(2, storedHash);
            statement.setString(3, "ADMIN");

            statement.executeUpdate();
        }

        Arrays.fill(passwordHash, (byte) 0);
    }

    public static boolean login(String username, char[] password)
            throws SQLException, GeneralSecurityException {

        String sql = """
            SELECT password_hash, role
            FROM users
            WHERE username = ?
            """;

        try (Connection connection = StudentDatabase.connect();
             PreparedStatement statement =
                 connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return false;
                }

                if (!"ADMIN".equals(result.getString("role"))) {
                    return false;
                }

                String[] parts =
                    result.getString("password_hash").split("\\$");

                if (parts.length != 4 ||
                    !parts[0].equals("pbkdf2")) {
                    return false;
                }

                int iterations = Integer.parseInt(parts[1]);

                if (iterations < ITERATIONS) {
                    return false;
                }

                byte[] salt =
                    Base64.getDecoder().decode(parts[2]);

                byte[] expected =
                    Base64.getDecoder().decode(parts[3]);

                byte[] actual = hash(
                    password, salt, iterations
                );

                boolean valid = MessageDigest.isEqual(
                    expected, actual
                );

                Arrays.fill(actual, (byte) 0);

                return valid;
            }
        }
    }
} 