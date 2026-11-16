package nexus.integrations.analytics;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import nexus.database.DatabaseService;

/**
 * Provides controlled, read-only analytics data access for Metabase in NEXUS V26.
 *
 * Security Principle:
 * Under NO circumstances is Metabase or any external reporting engine allowed
 * to execute write, update, delete, or schema-altering SQL statements against
 * NEXUS business databases.
 */
public class ReadOnlyAnalyticsStore {
    private final String dbPath;

    public ReadOnlyAnalyticsStore() {
        this(DatabaseService.getDatabasePath());
    }

    public ReadOnlyAnalyticsStore(String dbPath) {
        this.dbPath = dbPath != null ? dbPath : "nexus.db";
    }

    /**
     * Obtains a strictly read-only JDBC connection.
     * Guaranteed by SQLite PRAGMA query_only = ON.
     */
    public Connection getReadOnlyConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC Driver not found", e);
        }

        org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
        config.setReadOnly(true);
        config.setBusyTimeout(10000);
        Connection conn = config.createConnection("jdbc:sqlite:" + dbPath);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA query_only = ON;");
        }
        return conn;
    }

    /**
     * Validates that an SQL query is strictly read-only (SELECT statements only).
     */
    public static boolean isReadOnlyQuery(String sql) {
        if (sql == null) return false;
        String clean = sql.trim().toLowerCase();
        if (clean.startsWith("select") || clean.startsWith("pragma") || clean.startsWith("explain")) {
            return !clean.contains("insert") && 
                   !clean.contains("update") && 
                   !clean.contains("delete") && 
                   !clean.contains("drop") && 
                   !clean.contains("alter") && 
                   !clean.contains("create");
        }
        return false;
    }

    public String getReadOnlyJdbcUrl() {
        return "jdbc:sqlite:" + dbPath + "?mode=ro";
    }
}
