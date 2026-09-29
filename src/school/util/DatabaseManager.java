package school.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton database manager used as the single entry point for JDBC connections.
 */
public final class DatabaseManager {

    private static final String DEFAULT_URL =
        "jdbc:h2:./data/school;DATABASE_TO_LOWER=TRUE";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private DatabaseManager() {
    }

    public static DatabaseManager getInstance() {
        return INSTANCE;
    }

    public String getDatabaseUrl() {
        return System.getProperty("school.db.url", DEFAULT_URL);
    }

    public Connection getConnection() throws SQLException {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (Exception e) {
            throw new SQLException("Could not create data folder.", e);
        }

        return DriverManager.getConnection(
            getDatabaseUrl(),
            USER,
            PASSWORD
        );
    }
}
