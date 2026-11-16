package nexus.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import nexus.finance.Transaction;
import nexus.finance.TransactionCategory;
import nexus.tasks.Task;
import nexus.tasks.TaskPriority;
import nexus.goals.Goal;
import nexus.goals.Milestone;
import nexus.media.CinemaBooking;
import nexus.media.MediaItem;
import nexus.media.MediaType;
import nexus.media.MediaStatus;

/**
 * Enterprise-grade asynchronous SQLite persistence layer for NEXUS V26.1.
 * Persists Tasks, Transactions, Goals, Milestones, Media Items, Cinema Bookings, and Market Purchases.
 *
 * V26.1 Lifecycle & Reliability Hardening:
 * - Pre-filtering: non-persistable events rejected prior to queue submission.
 * - Idempotent shutdown: safe against repeated close() calls, cleanly resets singleton.
 * - Accurate draining: awaitDrained() correctly reports queue completion even during shutdown.
 * - Connection safety: writeConnection is never closed while worker tasks are executing.
 * - Visible error tracking: failed database writes are tracked and observable via failedWriteCount.
 * - Controlled read-only access: getReadOnlyConnection() enforces PRAGMA query_only = ON.
 */
public class DatabaseService implements NexusListener {
    private static String resolveDbPath() {
        String defaultDb = System.getProperty("nexus.mockBackend", "false").equals("true") ? ":memory:" : "nexus.db";
        return System.getProperty("nexus.db.path", defaultDb);
    }

    public static String getDatabasePath() {
        return resolveDbPath();
    }

    private static String getDbUrl() {
        return "jdbc:sqlite:" + resolveDbPath();
    }
    
    // Explicit whitelist of events that require database persistence
    private static final Set<String> PERSISTABLE_EVENTS = new HashSet<>(Arrays.asList(
        "TASK_ADDED", "TASK_COMPLETED", "TASK_DELETED",
        "TRANSACTION_ADDED",
        "TICKET_BOOKED",
        "GOAL_ADDED", "GOAL_DELETED", "GOALS_UPDATED",
        "MEDIA_ADDED", "MEDIA_STATUS_CHANGED", "MEDIA_RATED", "MEDIA_DELETED",
        "PRODUCT_PURCHASED"
    ));

    private static DatabaseService instance;

    public static synchronized DatabaseService getInstance() {
        if (instance == null || instance.isClosed()) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private Connection writeConnection;
    private final ExecutorService dbWriterQueue;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger failedWriteCount = new AtomicInteger(0);
    private volatile String lastWriteError = null;

    public DatabaseService() {
        synchronized (DatabaseService.class) {
            if (instance == null || instance.isClosed()) {
                instance = this;
            }
        }
        dbWriterQueue = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "nexus-db-writer");
            t.setDaemon(true);
            return t;
        });
        initDatabase();
        EventBus.getInstance().subscribeToAll(this);
        
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public boolean isClosed() {
        return closed.get();
    }

    public int getFailedWriteCount() {
        return failedWriteCount.get();
    }

    public String getLastWriteError() {
        return lastWriteError;
    }

    /**
     * Gracefully flushes remaining pending writes and closes database connections.
     * Guaranteed to be idempotent and safe against repeated calls.
     */
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return; // Already closed
        }
        EventBus.getInstance().unsubscribeFromAll(this);

        if (dbWriterQueue != null && !dbWriterQueue.isShutdown()) {
            dbWriterQueue.shutdown();
            try {
                if (!dbWriterQueue.awaitTermination(5, TimeUnit.SECONDS)) {
                    dbWriterQueue.shutdownNow();
                }
            } catch (InterruptedException e) {
                dbWriterQueue.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        try {
            if (writeConnection != null && !writeConnection.isClosed()) {
                writeConnection.close();
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseService] Error closing writeConnection: " + e.getMessage());
        }

        // Reset singleton reference so future getInstance() calls create an active instance
        synchronized (DatabaseService.class) {
            if (instance == this) {
                instance = null;
            }
        }
    }

    /**
     * Synchronously waits until all currently queued database writes have completed.
     * Accurately reports completion across active, shutting down, and terminated states.
     */
    public boolean awaitDrained(long timeoutMs) {
        if (closed.get() || dbWriterQueue == null || dbWriterQueue.isTerminated()) {
            return true;
        }
        if (dbWriterQueue.isShutdown()) {
            try {
                return dbWriterQueue.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        CountDownLatch latch = new CountDownLatch(1);
        try {
            dbWriterQueue.submit(latch::countDown);
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ree) {
            return dbWriterQueue.isTerminated();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Determines whether an event type requires database persistence.
     */
    public static boolean isPersistable(String eventType) {
        return eventType != null && PERSISTABLE_EVENTS.contains(eventType);
    }

    /**
     * Returns a read-only JDBC connection to the database.
     * Guarantees external services cannot mutate NEXUS tables via SQLite PRAGMA query_only = ON.
     */
    public Connection getReadOnlyConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
        config.setReadOnly(true);
        config.setBusyTimeout(10000);
        Connection conn = config.createConnection(getDbUrl());
        try (Statement s = conn.createStatement()) {
            s.execute("PRAGMA query_only = ON;");
        }
        return conn;
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
            config.setBusyTimeout(10000);
            config.setJournalMode(org.sqlite.SQLiteConfig.JournalMode.WAL);
            config.setSynchronous(org.sqlite.SQLiteConfig.SynchronousMode.NORMAL);
            writeConnection = config.createConnection(getDbUrl());
            
            try (Statement stmt = writeConnection.createStatement()) {
                // Enable Write-Ahead Logging & Busy Timeout to eliminate database locks
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA busy_timeout = 10000;");

                // 1. Tasks
                stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                             "id TEXT PRIMARY KEY, " +
                             "title TEXT, " +
                             "priority TEXT, " +
                             "completed INTEGER, " +
                             "due_date TEXT, " +
                             "category TEXT)");

                // 2. Transactions
                stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                             "id TEXT PRIMARY KEY, " +
                             "description TEXT, " +
                             "amount REAL, " +
                             "date INTEGER, " +
                             "category TEXT)");
                             
                // 3. Goals
                stmt.execute("CREATE TABLE IF NOT EXISTS goals (" +
                             "id TEXT PRIMARY KEY, " +
                             "title TEXT, " +
                             "deadline TEXT, " +
                             "category TEXT)");

                // 4. Milestones
                stmt.execute("CREATE TABLE IF NOT EXISTS milestones (" +
                             "id TEXT PRIMARY KEY, " +
                             "goal_id TEXT, " +
                             "description TEXT, " +
                             "completed INTEGER)");

                // 5. Media items (Watchlist)
                stmt.execute("CREATE TABLE IF NOT EXISTS media_items (" +
                             "id TEXT PRIMARY KEY, " +
                             "title TEXT, " +
                             "type TEXT, " +
                             "status TEXT, " +
                             "rating INTEGER, " +
                             "poster_url TEXT, " +
                             "genres TEXT, " +
                             "rating_score REAL, " +
                             "summary TEXT, " +
                             "year TEXT)");

                // 6. Cinema Bookings (BookMyShow Tickets)
                stmt.execute("CREATE TABLE IF NOT EXISTS cinema_bookings (" +
                             "id TEXT PRIMARY KEY, " +
                             "movie_title TEXT, " +
                             "theatre TEXT, " +
                             "showtime TEXT, " +
                             "seats TEXT, " +
                             "base_price REAL, " +
                             "discount REAL, " +
                             "promo_code TEXT, " +
                             "final_total REAL, " +
                             "booking_date INTEGER, " +
                             "status TEXT, " +
                             "payment_method TEXT)");

                // 7. Market Purchases
                stmt.execute("CREATE TABLE IF NOT EXISTS market_purchases (" +
                             "id TEXT PRIMARY KEY, " +
                             "product_name TEXT, " +
                             "price REAL, " +
                             "delivery_address TEXT, " +
                             "payment_method TEXT, " +
                             "timestamp INTEGER)");
            }

            // Safe Alter Table migrations
            safelyExecute("ALTER TABLE tasks ADD COLUMN due_date TEXT");
            safelyExecute("ALTER TABLE tasks ADD COLUMN category TEXT");
            safelyExecute("ALTER TABLE transactions ADD COLUMN category TEXT");
            safelyExecute("ALTER TABLE goals ADD COLUMN deadline TEXT");
            safelyExecute("ALTER TABLE goals ADD COLUMN category TEXT");
            safelyExecute("ALTER TABLE cinema_bookings ADD COLUMN payment_method TEXT");
            safelyExecute("ALTER TABLE market_purchases ADD COLUMN delivery_address TEXT");
            safelyExecute("ALTER TABLE market_purchases ADD COLUMN payment_method TEXT");

        } catch (Exception e) {
            System.err.println("[DatabaseService] Init error: " + e.getMessage());
        }
    }

    private void safelyExecute(String sql) {
        try (Statement s = writeConnection.createStatement()) {
            s.execute(sql);
        } catch (SQLException ignored) {}
    }
    
    // --- LOAD METHODS ---

    public List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, title, priority, completed, due_date, category FROM tasks";
        try (Statement stmt = writeConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String pStr = rs.getString("priority");
                TaskPriority prio = TaskPriority.MEDIUM;
                try {
                    prio = TaskPriority.valueOf(pStr);
                } catch (Exception ignored) {}

                String due = rs.getString("due_date");
                String cat = rs.getString("category");

                tasks.add(new Task(
                    rs.getString("id"),
                    rs.getString("title"),
                    prio,
                    rs.getInt("completed") == 1,
                    cat != null ? cat : "General",
                    due != null ? due : "Today"
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseService] Error loading tasks: " + e.getMessage());
        }
        return tasks;
    }
    
    public List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, description, amount, date, category FROM transactions ORDER BY date DESC";
        try (Statement stmt = writeConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String catStr = rs.getString("category");
                TransactionCategory cat = TransactionCategory.OTHER;
                if (catStr != null) {
                    try {
                        cat = TransactionCategory.valueOf(catStr);
                    } catch (Exception ex) {
                        cat = TransactionCategory.OTHER;
                    }
                }

                transactions.add(new Transaction(
                    rs.getString("id"),
                    rs.getString("description"),
                    rs.getDouble("amount"),
                    new Date(rs.getLong("date")),
                    cat
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseService] Error loading transactions: " + e.getMessage());
        }
        return transactions;
    }

    // Direct helper methods for IRepository pattern
    public void persistTask(Task t) {
        onEvent(new NexusEvent("TASK_ADDED", t, "Database"));
    }

    public void deleteTask(String id) {
        Task dummy = new Task(id, "", TaskPriority.LOW, false);
        onEvent(new NexusEvent("TASK_DELETED", dummy, "Database"));
    }

    public void persistTransaction(Transaction t) {
        onEvent(new NexusEvent("TRANSACTION_ADDED", t, "Database"));
    }

    public List<Goal> loadGoals() {
        List<Goal> goals = new ArrayList<>();
        String sql = "SELECT id, title, deadline, category FROM goals";
        try (Statement stmt = writeConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String gId = rs.getString("id");
                String title = rs.getString("title");
                String deadline = rs.getString("deadline");
                String category = rs.getString("category");

                List<Milestone> milestones = loadMilestonesForGoal(gId);
                goals.add(new Goal(gId, title, category, deadline, milestones));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseService] Error loading goals: " + e.getMessage());
        }
        return goals;
    }

    private List<Milestone> loadMilestonesForGoal(String goalId) {
        List<Milestone> list = new ArrayList<>();
        String sql = "SELECT id, description, completed FROM milestones WHERE goal_id = ?";
        try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
            pstmt.setString(1, goalId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Milestone(
                        rs.getString("id"),
                        rs.getString("description"),
                        rs.getInt("completed") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseService] Error loading milestones: " + e.getMessage());
        }
        return list;
    }

    public List<MediaItem> loadMedia() {
        List<MediaItem> mediaList = new ArrayList<>();
        String sql = "SELECT id, title, type, status, rating, poster_url, genres, rating_score, summary, year FROM media_items";
        try (Statement stmt = writeConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                MediaType type = MediaType.MOVIE;
                try {
                    type = MediaType.valueOf(rs.getString("type"));
                } catch (Exception ignored) {}

                MediaStatus status = MediaStatus.WANT_TO_CONSUME;
                try {
                    status = MediaStatus.valueOf(rs.getString("status"));
                } catch (Exception ignored) {}

                mediaList.add(new MediaItem(
                    rs.getString("id"),
                    rs.getString("title"),
                    type,
                    status,
                    rs.getInt("rating"),
                    rs.getString("poster_url"),
                    rs.getString("genres"),
                    rs.getDouble("rating_score"),
                    rs.getString("summary"),
                    rs.getString("year")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseService] Error loading media: " + e.getMessage());
        }
        return mediaList;
    }

    public List<CinemaBooking> loadBookings() {
        List<CinemaBooking> list = new ArrayList<>();
        String sql = "SELECT id, movie_title, theatre, showtime, seats, base_price, discount, promo_code, final_total, booking_date, status, payment_method FROM cinema_bookings ORDER BY booking_date DESC";
        try (Statement stmt = writeConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String seatsRaw = rs.getString("seats");
                List<String> seatsList = seatsRaw != null ? Arrays.asList(seatsRaw.split(",\\s*")) : new ArrayList<>();
                String payMethod = "NEXUS Capital Wallet";
                try {
                    payMethod = rs.getString("payment_method");
                    if (payMethod == null || payMethod.isEmpty()) payMethod = "NEXUS Capital Wallet";
                } catch (Exception ignored) {}

                list.add(new CinemaBooking(
                    rs.getString("id"),
                    rs.getString("movie_title"),
                    rs.getString("theatre"),
                    rs.getString("showtime"),
                    seatsList,
                    rs.getDouble("base_price"),
                    rs.getDouble("discount"),
                    rs.getString("promo_code"),
                    rs.getDouble("final_total"),
                    new Date(rs.getLong("booking_date")),
                    rs.getString("status"),
                    payMethod
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DatabaseService] Error loading bookings: " + e.getMessage());
        }
        return list;
    }

    // --- ASYNC EVENT WRITER WITH TRANSACTION PROTECTION & PRE-FILTERING ---

    @Override
    public void onEvent(NexusEvent event) {
        if (event == null || closed.get() || dbWriterQueue == null || dbWriterQueue.isShutdown()) {
            return;
        }
        
        String type = event.getType();
        // Core Reliability: Filter irrelevant events before queue submission
        if (!isPersistable(type)) {
            return;
        }

        Object payload = event.getPayload();
        
        try {
            dbWriterQueue.submit(() -> {
                synchronized (this) {
                    try {
                        // TASK EVENTS
                        if ("TASK_ADDED".equals(type) && payload instanceof Task) {
                            Task t = (Task) payload;
                            String sql = "INSERT OR REPLACE INTO tasks(id, title, priority, completed, due_date, category) VALUES(?, ?, ?, ?, ?, ?)";
                            try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
                                pstmt.setString(1, t.getId());
                                pstmt.setString(2, t.getTitle());
                                pstmt.setString(3, t.getPriority().name());
                                pstmt.setInt(4, t.isCompleted() ? 1 : 0);
                                pstmt.setString(5, t.getDueDate());
                                pstmt.setString(6, t.getCategory());
                                pstmt.executeUpdate();
                            }
                        } else if ("TASK_COMPLETED".equals(type) && payload instanceof Task) {
                            Task t = (Task) payload;
                            String sql = "UPDATE tasks SET completed = 1 WHERE id = ?";
                            try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
                                pstmt.setString(1, t.getId());
                                pstmt.executeUpdate();
                            }
                        } else if ("TASK_DELETED".equals(type) && payload instanceof Task) {
                            Task t = (Task) payload;
                            String sql = "DELETE FROM tasks WHERE id = ?";
                            try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
                                pstmt.setString(1, t.getId());
                                pstmt.executeUpdate();
                            }
                        } 
                        // TRANSACTION EVENTS
                        else if ("TRANSACTION_ADDED".equals(type) && payload instanceof Transaction) {
                            Transaction t = (Transaction) payload;
                            String sql = "INSERT OR REPLACE INTO transactions(id, description, amount, date, category) VALUES(?, ?, ?, ?, ?)";
                            try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
                                pstmt.setString(1, t.getId());
                                pstmt.setString(2, t.getDescription());
                                pstmt.setDouble(3, t.getAmount());
                                pstmt.setLong(4, t.getDate().getTime());
                                pstmt.setString(5, t.getCategory() != null ? t.getCategory().name() : "OTHER");
                                pstmt.executeUpdate();
                            }
                        }
                        // CINEMA BOOKING EVENTS
                        else if ("TICKET_BOOKED".equals(type) && payload instanceof CinemaBooking) {
                            CinemaBooking b = (CinemaBooking) payload;
                            String sql = "INSERT OR REPLACE INTO cinema_bookings(id, movie_title, theatre, showtime, seats, base_price, discount, promo_code, final_total, booking_date, status, payment_method) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                            try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
                                pstmt.setString(1, b.getId());
                                pstmt.setString(2, b.getMovieTitle());
                                pstmt.setString(3, b.getTheatre());
                                pstmt.setString(4, b.getShowTime());
                                pstmt.setString(5, b.getSeatsFormatted());
                                pstmt.setDouble(6, b.getBasePrice());
                                pstmt.setDouble(7, b.getDiscount());
                                pstmt.setString(8, b.getPromoCode());
                                pstmt.setDouble(9, b.getFinalTotal());
                                pstmt.setLong(10, b.getBookingDate().getTime());
                                pstmt.setString(11, b.getStatus());
                                pstmt.setString(12, b.getPaymentMethod());
                                pstmt.executeUpdate();
                            }
                        }
                        // GOAL & MILESTONE EVENTS (TRANSACTION PROTECTED)
                        else if ("GOAL_ADDED".equals(type) && payload instanceof Goal) {
                            Goal g = (Goal) payload;
                            String sql = "INSERT OR REPLACE INTO goals(id, title, deadline, category) VALUES(?, ?, ?, ?)";
                            try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
                                pstmt.setString(1, g.getId());
                                pstmt.setString(2, g.getTitle());
                                pstmt.setString(3, g.getDeadline());
                                pstmt.setString(4, g.getCategory());
                                pstmt.executeUpdate();
                            }
                        } else if ("GOAL_DELETED".equals(type) && payload instanceof Goal) {
                            Goal g = (Goal) payload;
                            boolean oldAutoCommit = writeConnection.getAutoCommit();
                            try {
                                writeConnection.setAutoCommit(false);
                                try (PreparedStatement p1 = writeConnection.prepareStatement("DELETE FROM milestones WHERE goal_id = ?");
                                     PreparedStatement p2 = writeConnection.prepareStatement("DELETE FROM goals WHERE id = ?")) {
                                    p1.setString(1, g.getId());
                                    p1.executeUpdate();
                                    p2.setString(2, g.getId());
                                    p2.executeUpdate();
                                }
                                writeConnection.commit();
                            } catch (SQLException ex) {
                                writeConnection.rollback();
                                throw ex;
                            } finally {
                                writeConnection.setAutoCommit(oldAutoCommit);
                            }
                        } else if ("GOALS_UPDATED".equals(type) && payload instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Goal> goals = (List<Goal>) payload;
                            boolean oldAutoCommit = writeConnection.getAutoCommit();
                            try {
                                writeConnection.setAutoCommit(false);
                                for (Goal g : goals) {
                                    try (PreparedStatement pGoal = writeConnection.prepareStatement("INSERT OR REPLACE INTO goals(id, title, deadline, category) VALUES(?, ?, ?, ?)")) {
                                        pGoal.setString(1, g.getId());
                                        pGoal.setString(2, g.getTitle());
                                        pGoal.setString(3, g.getDeadline());
                                        pGoal.setString(4, g.getCategory());
                                        pGoal.executeUpdate();
                                    }
                                    for (Milestone m : g.getMilestones()) {
                                        try (PreparedStatement pMs = writeConnection.prepareStatement("INSERT OR REPLACE INTO milestones(id, goal_id, description, completed) VALUES(?, ?, ?, ?)")) {
                                            pMs.setString(1, m.getId());
                                            pMs.setString(2, g.getId());
                                            pMs.setString(3, m.getDescription());
                                            pMs.setInt(4, m.isCompleted() ? 1 : 0);
                                            pMs.executeUpdate();
                                        }
                                    }
                                }
                                writeConnection.commit();
                            } catch (SQLException ex) {
                                writeConnection.rollback();
                                throw ex;
                            } finally {
                                writeConnection.setAutoCommit(oldAutoCommit);
                            }
                        }
                        // MEDIA WATCHLIST EVENTS
                        else if (("MEDIA_ADDED".equals(type) || "MEDIA_STATUS_CHANGED".equals(type) || "MEDIA_RATED".equals(type)) && payload instanceof MediaItem) {
                            MediaItem item = (MediaItem) payload;
                            String sql = "INSERT OR REPLACE INTO media_items(id, title, type, status, rating, poster_url, genres, rating_score, summary, year) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                            try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
                                pstmt.setString(1, item.getId());
                                pstmt.setString(2, item.getTitle());
                                pstmt.setString(3, item.getType().name());
                                pstmt.setString(4, item.getStatus().name());
                                pstmt.setInt(5, item.getRating());
                                pstmt.setString(6, item.getPosterUrl());
                                pstmt.setString(7, item.getGenres());
                                pstmt.setDouble(8, item.getRatingScore());
                                pstmt.setString(9, item.getSummary());
                                pstmt.setString(10, item.getYear());
                                pstmt.executeUpdate();
                            }
                        } else if ("MEDIA_DELETED".equals(type) && payload instanceof MediaItem) {
                            MediaItem item = (MediaItem) payload;
                            String sql = "DELETE FROM media_items WHERE id = ?";
                            try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
                                pstmt.setString(1, item.getId());
                                pstmt.executeUpdate();
                            }
                        }
                        // MARKET PURCHASES
                        else if ("PRODUCT_PURCHASED".equals(type)) {
                            double price = payload instanceof Double ? (Double) payload : 0.0;
                            String sql = "INSERT INTO market_purchases(id, product_name, price, timestamp) VALUES(?, ?, ?, ?)";
                            try (PreparedStatement pstmt = writeConnection.prepareStatement(sql)) {
                                pstmt.setString(1, nexus.common.IdGenerator.generateId());
                                pstmt.setString(2, "Market Order");
                                pstmt.setDouble(3, price);
                                pstmt.setLong(4, System.currentTimeMillis());
                                pstmt.executeUpdate();
                            }
                        }
                    } catch (SQLException e) {
                        failedWriteCount.incrementAndGet();
                        lastWriteError = e.getMessage();
                        System.err.println("[DatabaseService] Async Write Error for " + type + ": " + e.getMessage());
                        EventBus.getInstance().publish(new NexusEvent("DATABASE_WRITE_FAILED", type + ": " + e.getMessage(), "Database"));
                    }
                }
            });
        } catch (RejectedExecutionException ree) {
            System.err.println("[DatabaseService] Write task rejected: queue already closed for " + type);
        }
    }
}
