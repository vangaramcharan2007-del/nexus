package nexus;

import javax.swing.*;
import java.awt.*;
import nexus.dashboard.DashboardPanel;
import nexus.dashboard.DashboardService;
import nexus.finance.FinancePanel;
import nexus.finance.FinanceService;
import nexus.market.MarketPanel;
import nexus.market.MarketService;
import nexus.tasks.TaskPanel;
import nexus.tasks.TaskService;
import nexus.goals.GoalPanel;
import nexus.goals.GoalService;
import nexus.media.MediaPanel;
import nexus.media.MediaService;
import nexus.analytics.AnalyticsPanel;
import nexus.analytics.AnalyticsService;
import nexus.auth.LoginPanel;

/**
 * NEXUS Enterprise Application Shell - Production Architecture for Review 2.
 * 
 * Core Capabilities:
 * 1. EventBus Pub/Sub decoupled reactive event hub.
 * 2. SQLite single-threaded asynchronous persistent database (Tasks, Transactions, Goals, Media, Bookings).
 * 3. Real-Time live API integration:
 *    - CoinGecko Decentralized Crypto Feed (BTC, ETH, SOL with INR/USD conversion)
 *    - TVMaze & IMDb live entertainment metadata search
 *    - Open-Meteo SRM University Kattankulathur Campus live sensor telemetry
 *    - FakeStore live e-commerce catalog
 * 4. BookMyShow Cinema Box Office Engine:
 *    - Interactive 60-seat visual allocation matrix (Executive, Club, VIP)
 *    - Real-time seat tally, student promo discounts (SRMSTUDENT, NEXUS50), and digital ticket pass generator
 * 5. Advanced Financial Suite:
 *    - Loan EMI Calculator (E = P·r·(1+r)^n / ((1+r)^n - 1))
 *    - Compound Interest & Wealth Growth Calculator (A = P(1 + r/n)^nt)
 *    - Systematic Investment Plan (SIP) Calculator
 *    - Discount & GST/Tax Net Engine
 * 6. Strategic Goals with deadline tracking, milestones, and progress bars.
 */
public class NexusApplication extends JFrame {
    
    public NexusApplication() {
        setTitle("NEXUS Enterprise Platform - Review 2 Edition");
        setSize(1100, 740);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Initialize Tray Manager & Voice Engine
        new nexus.common.NexusTrayManager(this);
        new nexus.common.SoundManager();
        
        // Initialize DatabaseService early
        nexus.database.DatabaseService databaseService = new nexus.database.DatabaseService();
        
        // Initialize Core Services
        DashboardService dashboardService = new DashboardService();
        FinanceService financeService = new FinanceService();
        MarketService marketService = new MarketService();
        TaskService taskService = new TaskService();
        GoalService goalService = new GoalService();
        MediaService mediaService = new MediaService();
        AnalyticsService analyticsService = new AnalyticsService();
        
        nexus.realtime.WindowsMediaHook mediaHook = new nexus.realtime.WindowsMediaHook();
        mediaHook.start();
        
        // Populate UI/Services with persistent SQLite DB data
        java.util.List<nexus.tasks.Task> loadedTasks = databaseService.loadTasks();
        if (!loadedTasks.isEmpty()) {
            taskService.setTasks(loadedTasks);
        }
        
        java.util.List<nexus.finance.Transaction> loadedTx = databaseService.loadTransactions();
        if (!loadedTx.isEmpty()) {
            financeService.setTransactions(loadedTx);
        }

        java.util.List<nexus.goals.Goal> loadedGoals = databaseService.loadGoals();
        if (!loadedGoals.isEmpty()) {
            goalService.setGoals(loadedGoals);
        }

        java.util.List<nexus.media.MediaItem> loadedMedia = databaseService.loadMedia();
        if (!loadedMedia.isEmpty()) {
            mediaService.setMediaItems(loadedMedia);
        }

        java.util.List<nexus.media.CinemaBooking> loadedBookings = databaseService.loadBookings();
        if (!loadedBookings.isEmpty()) {
            mediaService.setBookings(loadedBookings);
        }
        
        // Create Production Tabs (Clean, professional, zero fluff)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        tabbedPane.addTab("Dashboard", new DashboardPanel(dashboardService));
        tabbedPane.addTab("Finance & Calculators", new FinancePanel(financeService));
        tabbedPane.addTab("Cinema & Box Office", new MediaPanel(mediaService));
        tabbedPane.addTab("Live Market", new MarketPanel(marketService));
        tabbedPane.addTab("Tasks", new TaskPanel(taskService));
        tabbedPane.addTab("Strategic Goals", new GoalPanel(goalService));
        tabbedPane.addTab("Analytics", new AnalyticsPanel(analyticsService));
        
        // Setup CardLayout with Login
        CardLayout cardLayout = new CardLayout();
        JPanel mainContent = new JPanel(cardLayout);
        
        LoginPanel loginPanel = new LoginPanel(ignored -> {
            cardLayout.show(mainContent, "MainApp");
        });
        
        // Main Container with Activity Feed
        JPanel mainAppContainer = new JPanel(new BorderLayout());
        mainAppContainer.add(tabbedPane, BorderLayout.CENTER);
        mainAppContainer.add(new nexus.common.ActivityFeedPanel(), BorderLayout.EAST);
        
        // God Mode Terminal (Direct event trigger for demonstrations)
        JTextField godModeTerminal = new JTextField();
        godModeTerminal.setToolTipText("God Mode Terminal: /budget <amount> or /score <amount>");
        godModeTerminal.setBackground(new Color(20, 20, 20));
        godModeTerminal.setForeground(new Color(0, 255, 0));
        godModeTerminal.setCaretColor(new Color(0, 255, 0));
        godModeTerminal.setFont(new Font("Monospaced", Font.BOLD, 13));
        godModeTerminal.addActionListener(e -> {
            String cmd = godModeTerminal.getText().trim();
            godModeTerminal.setText("");
            if (cmd.startsWith("/budget ")) {
                try {
                    double amount = Double.parseDouble(cmd.substring(8).trim());
                    nexus.common.EventBus.getInstance().publish(new nexus.common.NexusEvent("GOD_MODE_BUDGET", amount, "GodMode"));
                } catch (NumberFormatException ignored) {}
            } else if (cmd.startsWith("/score ")) {
                try {
                    int score = Integer.parseInt(cmd.substring(7).trim());
                    nexus.common.EventBus.getInstance().publish(new nexus.common.NexusEvent("GOD_MODE_SCORE", score, "GodMode"));
                } catch (NumberFormatException ignored) {}
            }
        });
        mainAppContainer.add(godModeTerminal, BorderLayout.SOUTH);
        
        mainContent.add(loginPanel, "Login");
        mainContent.add(mainAppContainer, "MainApp");
        
        setLayout(new BorderLayout());
        add(mainContent, BorderLayout.CENTER);
        
        // Show Login initially
        cardLayout.show(mainContent, "Login");
        
        nexus.common.EventBus.getInstance().subscribeToAll(new nexus.common.NexusListener() {
            @Override
            public void onEvent(nexus.common.NexusEvent event) {
                if ("NAVIGATION_REQUEST".equals(event.getType())) {
                    String targetTab = (String) event.getPayload();
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        if (tabbedPane.getTitleAt(i).equalsIgnoreCase(targetTab)) {
                            tabbedPane.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });
    }
}
