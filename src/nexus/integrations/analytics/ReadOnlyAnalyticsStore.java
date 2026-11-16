package nexus.integrations.analytics;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nexus.database.DatabaseService;

/**
 * Provides controlled, read-only analytics data access for Metabase and reporting in NEXUS V26.1.1.
 *
 * Multi-Layered Security Architecture:
 * 1. Application-Level Allowlisting: Validates statements against safe read-only grammar.
 *    Supports standard SELECT queries, Common Table Expressions (WITH ... SELECT ...), and EXPLAIN.
 * 2. Multi-Statement Injection Defense: Detects and rejects chained SQL execution.
 * 3. Safe PRAGMA Whitelist: Only strictly safe read-only introspection PRAGMAs allowed without assignment.
 * 4. JDBC Driver Read-Only Enforcement: Connection initialized with config.setReadOnly(true).
 * 5. SQLite Engine Hardware Block: Executes PRAGMA query_only = ON, causing SQLite itself
 *    to throw an immediate SQLException if any mutation is attempted.
 */
public class ReadOnlyAnalyticsStore {
    // Dangerous DDL/DML mutation keywords that are strictly prohibited anywhere in analytics queries
    private static final Pattern FORBIDDEN_MUTATIONS = Pattern.compile(
        "\\b(insert|update|delete|drop|alter|create|replace|truncate|attach|detach|grant|revoke|vacuum|reindex|load_extension)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Whitelist of read-only PRAGMAs permitted for schema inspection
    private static final Set<String> ALLOWED_PRAGMAS = new HashSet<>(Arrays.asList(
        "table_info", "table_xinfo", "index_list", "index_info", 
        "foreign_key_list", "database_list", "collation_list", 
        "compile_options", "query_only", "user_version"
    ));

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
     * Rejects mutations, statement chaining, forbidden PRAGMAs, and non-query statements.
     */
    public static boolean isReadOnlyQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }

        // Step 1: Strip SQL comments to normalize text
        String stripped = stripComments(sql).trim();
        if (stripped.isEmpty()) {
            return false;
        }

        // Step 2: Multi-Statement Chaining Defense
        // Allow at most one trailing semicolon; reject multiple statements separated by semicolons
        String[] parts = stripped.split(";");
        int nonEmptyCount = 0;
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                nonEmptyCount++;
            }
        }
        if (nonEmptyCount != 1) {
            return false; // Multi-statement or empty query rejected
        }

        String singleStmt = parts[0].trim();
        String lower = singleStmt.toLowerCase();

        // Step 3: Reject forbidden DDL / DML mutation tokens anywhere in statement
        Matcher mutationMatcher = FORBIDDEN_MUTATIONS.matcher(lower);
        if (mutationMatcher.find()) {
            return false;
        }

        // Step 4: Validate Statement Prefix
        // Safe prefixes: "select", "with" (CTEs), "explain", or safe "pragma"
        if (lower.startsWith("select ") || lower.startsWith("select\n") || lower.startsWith("select\t") || lower.startsWith("select(") || lower.equals("select")) {
            return true;
        }

        if (lower.startsWith("with ") || lower.startsWith("with\n") || lower.startsWith("with\t")) {
            // Common Table Expressions (CTEs) must contain a subsequent SELECT
            return lower.contains("select");
        }

        if (lower.startsWith("explain ") || lower.startsWith("explain\n") || lower.startsWith("explain\t")) {
            return lower.contains("select");
        }

        if (lower.startsWith("pragma ") || lower.startsWith("pragma\t")) {
            return isSafePragma(singleStmt);
        }

        return false;
    }

    /**
     * Validates that a PRAGMA statement is strictly read-only schema inspection.
     * Rejects any PRAGMA with an assignment (=) or non-whitelisted target.
     */
    private static boolean isSafePragma(String pragmaStmt) {
        // Assignments (e.g. PRAGMA writable_schema = ON) are strictly prohibited
        if (pragmaStmt.contains("=")) {
            return false;
        }

        String trimmed = pragmaStmt.trim();
        // Extract pragma name e.g. "PRAGMA table_info(tasks)" -> "table_info"
        String remainder = trimmed.substring(6).trim(); // Skip "pragma"
        int parenIdx = remainder.indexOf('(');
        int spaceIdx = remainder.indexOf(' ');
        
        int endIdx = remainder.length();
        if (parenIdx != -1) endIdx = Math.min(endIdx, parenIdx);
        if (spaceIdx != -1) endIdx = Math.min(endIdx, spaceIdx);

        String pragmaName = remainder.substring(0, endIdx).trim().toLowerCase();
        return ALLOWED_PRAGMAS.contains(pragmaName);
    }

    /**
     * Strips SQL line comments (-- ...) and block comments (/* ... * /).
     */
    private static String stripComments(String sql) {
        // Strip block comments
        String noBlock = sql.replaceAll("/\\*.*?\\*/", " ");
        // Strip line comments
        return noBlock.replaceAll("--.*", " ");
    }

    public String getReadOnlyJdbcUrl() {
        return "jdbc:sqlite:" + dbPath + "?mode=ro";
    }
}
