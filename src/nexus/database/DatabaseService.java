package nexus.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import nexus.finance.Transaction;
import nexus.tasks.Task;
import nexus.tasks.TaskPriority;
import nexus.goals.Goal;

/**
 * The DatabaseService acts as the central data persistence layer for the NEXUS application.
 * It uses SQLite (via JDBC) to store and retrieve application state locally.
 * 
 * Implementing NexusListener allows this service to silently listen to the EventBus.
 * When other parts of the application fire events (e.g., "TASK_ADDED"), the DatabaseService
 * intercepts these events and automatically commits the changes to the database.
 * This completely decouples the GUI and other Services from SQL logic.
 */
public class DatabaseService implements NexusListener {
    private static final String URL = "jdbc:sqlite:nexus.db";

    public DatabaseService() {
        initDatabase();
        EventBus.getInstance().subscribeToAll(this);
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS tasks (" +
                         "id TEXT PRIMARY KEY, " +
                         "title TEXT, " +
                         "priority TEXT, " +
                         "completed INTEGER)");
                         
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                         "id TEXT PRIMARY KEY, " +
                         "description TEXT, " +
                         "amount REAL, " +
                         "date INTEGER)");
                         
            stmt.execute("CREATE TABLE IF NOT EXISTS goals (" +
                         "id TEXT PRIMARY KEY, " +
                         "title TEXT)");
                         
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT id, title, priority, completed FROM tasks";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Task t = new Task(
                    rs.getString("id"),
                    rs.getString("title"),
                    TaskPriority.valueOf(rs.getString("priority")),
                    rs.getInt("completed") == 1
                );
                tasks.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }
    
    public List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        String sql = "SELECT id, description, amount, date FROM transactions";
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Transaction t = new Transaction(
                    rs.getString("id"),
                    rs.getString("description"),
                    rs.getDouble("amount"),
                    new Date(rs.getLong("date"))
                );
                transactions.add(t);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    /**
     * This method is triggered automatically whenever an event is published to the EventBus.
     * The DatabaseService listens to everything, but only reacts to events it cares about
     * (like saving tasks or transactions).
     */
    @Override
    public void onEvent(NexusEvent event) {
        String type = event.getType();
        Object payload = event.getPayload();
        
        try (Connection conn = DriverManager.getConnection(URL)) {
            if ("TASK_ADDED".equals(type) && payload instanceof Task) {
                Task t = (Task) payload;
                // PreparedStatement prevents SQL Injection attacks and safely parses objects into SQL data types.
                String sql = "INSERT INTO tasks(id, title, priority, completed) VALUES(?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, t.getId());
                    pstmt.setString(2, t.getTitle());
                    pstmt.setString(3, t.getPriority().name());
                    pstmt.setInt(4, t.isCompleted() ? 1 : 0);
                    pstmt.executeUpdate();
                }
            } else if ("TASK_COMPLETED".equals(type) && payload instanceof Task) {
                Task t = (Task) payload;
                String sql = "UPDATE tasks SET completed = 1 WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, t.getId());
                    pstmt.executeUpdate();
                }
            } else if ("TRANSACTION_ADDED".equals(type) && payload instanceof Transaction) {
                Transaction t = (Transaction) payload;
                String sql = "INSERT INTO transactions(id, description, amount, date) VALUES(?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, t.getId());
                    pstmt.setString(2, t.getDescription());
                    pstmt.setDouble(3, t.getAmount());
                    pstmt.setLong(4, t.getDate().getTime());
                    pstmt.executeUpdate();
                }
            } else if ("GOAL_COMPLETED".equals(type) && payload instanceof Goal) {
                // Not requested to save fully, just showing listener implementation
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
