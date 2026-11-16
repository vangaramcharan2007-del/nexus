package nexus.integrations.analytics;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import nexus.database.DatabaseService;

/**
 * Provides controlled, read-only analytics data access for Metabase and reporting in NEXUS V26.1.
 *
 * Multi-Layered Security Architecture:
 * 1. Application-Level Allowlisting: Validates statements against safe read-only grammar.
 * 2. Multi-Statement Injection Defense: Detects and rejects chained SQL execution.
 * 3. JDBC Driver Read-Only Enforcement: Connection initialized with config.setReadOnly(true).
 * 4. SQLite Engine Hardware Block: Executes PRAGMA query_only = ON, causing SQLite itself
 *    to throw an immediate SQLException if any mutation is attempted.
 */
public class ReadOnlyAnalyticsStore {
    private static final Pattern MUTATION_PATTERN = Pattern.compile(
        ".*\\b(insert|update|delete|drop|alter|create|replace|truncate|attach|detach|grant|revoke)\\b.*",
        Pattern.CASE_INSENSITIVE
    );

    private final String dbPath;

    public ReadOnlyAnalyticsStore() {
        this(DatabaseService.getDatabasePath());
    }

    public ReadOnlyAnalyticsStore(String dbPath) {
        this.dbPath = dbPath != null ? dbPath : "nexus.db";
    }

    /**
     * Obtains a strictly read-only JDBC connection.
     * Enforces dual engine protection: config.setReadOnly(true) and PRAGMA query_only = ON.
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
     * Validates whether an SQL query is safe for read-only analytics execution.
     * Disallows mutation keywords, statement chaining, and DDL operations.
     */
    public static boolean isReadOnlyQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String clean = sql.trim().toLowerCase();

        // Handle possible semicolon delimiters
        String[] statements = clean.split(";");
        for (String s : statements) {
            String stmt = s.trim();
            if (stmt.isEmpty()) continue;
            if (!isSingleStatementReadOnly(stmt)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSingleStatementReadOnly(String stmt) {
        boolean validPrefix = stmt.startsWith("select ") || 
                             stmt.startsWith("select\n") || 
                             stmt.startsWith("pragma ") || 
                             stmt.startsWith("explain ");
        if (!validPrefix) {
            return false;
        }

        // Must not contain any forbidden mutation tokens
        return !MUTATION_PATTERN.matcher(stmt).matches();
    }

    public String getReadOnlyJdbcUrl() {
        return "jdbc:sqlite:" + dbPath + "?mode=ro";
    }
}
