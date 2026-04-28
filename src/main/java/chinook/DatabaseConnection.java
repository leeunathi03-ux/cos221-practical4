package chinook;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection
 * ------------------
 * Manages the JDBC connection to the Chinook MySQL/MariaDB database.
 * Credentials are read from environment variables (Task 5 requirement).
 *
 * CONCEPT — Why environment variables?
 *   Hardcoding passwords in source code is a security risk.
 *   If you upload to GitHub, everyone sees your password.
 *   Environment variables keep credentials outside the code.
 *
 * CONCEPT — What is JDBC?
 *   Java Database Connectivity. It's the standard Java API for
 *   talking to relational databases. The MySQL Connector/J JAR
 *   is the "driver" that implements this API for MySQL/MariaDB.
 *
 * Required environment variables:
 *   CHINOOK_DB_PROTO    e.g. jdbc:mysql
 *   CHINOOK_DB_HOST     e.g. localhost
 *   CHINOOK_DB_PORT     e.g. 3306
 *   CHINOOK_DB_NAME     e.g. u12345678_chinook
 *   CHINOOK_DB_USERNAME e.g. root
 *   CHINOOK_DB_PASSWORD e.g. yourpassword
 */
public class DatabaseConnection {

    // Read all credentials from environment variables
    private static final String PROTO    = getEnv("CHINOOK_DB_PROTO",    "jdbc:mysql");
    private static final String HOST     = getEnv("CHINOOK_DB_HOST",     "localhost");
    private static final String PORT     = getEnv("CHINOOK_DB_PORT",     "3306");
    private static final String DB_NAME  = getEnv("CHINOOK_DB_NAME",     "chinook");
    private static final String USERNAME = getEnv("CHINOOK_DB_USERNAME", "root");
    private static final String PASSWORD = getEnv("CHINOOK_DB_PASSWORD", "");

    // Build JDBC URL from parts: e.g. jdbc:mysql://localhost:3306/u12345678_chinook
   private static final String URL = PROTO + "://" + HOST + ":" + PORT + "/" + DB_NAME + "?useSSL=false";
    /**
     * Returns a new connection each time it is called.
     * ALWAYS use try-with-resources when calling this:
     *   try (Connection conn = DatabaseConnection.getConnection()) { ... }
     * This ensures the connection is closed automatically, even if an
     * exception occurs — preventing connection leaks.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    /**
     * Reads an environment variable.
     * Falls back to defaultValue if the variable is not set.
     * Prints a warning so you know which variable is missing.
     */
    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            System.err.println("WARNING: Environment variable '" + key
                + "' not set. Using default: " + defaultValue);
            return defaultValue;
        }
        return value;
    }
}
