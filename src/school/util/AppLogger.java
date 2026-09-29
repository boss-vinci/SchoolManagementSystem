package school.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class AppLogger {

    private static final Logger LOGGER =
        Logger.getLogger("SchoolManagementSystem");

    private static final Path LOG_DIRECTORY = Path.of("logs");
    private static final Path LOG_FILE =
        LOG_DIRECTORY.resolve("school-management.log");

    static {
        configure();
    }

    private AppLogger() {
    }

    private static void configure() {

        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);

        try {
            Files.createDirectories(LOG_DIRECTORY);

            FileHandler fileHandler = new FileHandler(
                LOG_FILE.toString(),
                true
            );

            fileHandler.setLevel(Level.ALL);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);

        } catch (IOException e) {
            System.err.println(
                "Could not initialize application log file: " +
                e.getMessage()
            );
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.WARNING);
        consoleHandler.setFormatter(new SimpleFormatter());
        LOGGER.addHandler(consoleHandler);
    }

    public static Path getLogPath() {
        return LOG_FILE;
    }

    private static void flushHandlers() {
        for (Handler handler : LOGGER.getHandlers()) {
            handler.flush();
        }
    }

    public static void info(String event, String message) {
        LOGGER.log(
            Level.INFO,
            "[{0}] {1}",
            new Object[]{event, message}
        );
        flushHandlers();
    }

    public static void warning(String event, String message) {
        LOGGER.log(
            Level.WARNING,
            "[{0}] {1}",
            new Object[]{event, message}
        );
        flushHandlers();
    }

    public static void error(
            String event,
            String message,
            Throwable throwable) {

        LOGGER.log(
            Level.SEVERE,
            "[" + event + "] " + message,
            throwable
        );
        flushHandlers();
    }
}
