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
 * The NexusApplication is the main entry point and the core graphical shell of the NEXUS application.
 * It extends JFrame to provide a windowed user interface.
 * 
 * This class is responsible for:
 * 1. Bootstrapping the application (initializing services, connecting to the database).
 * 2. Assembling the User Interface (UI) by connecting GUI Panels to their respective Services.
 * 3. Handling high-level state changes, like switching from the Login screen to the Main Application.
 * 
 * By passing Services into Panels via their constructors (Dependency Injection), 
 * we separate the visual representation (the Panel) from the business logic (the Service).
 */
public class NexusApplication extends JFrame {
    
    public NexusApplication() {
        setTitle("NEXUS Application");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Initialize Tray Manager
        new nexus.common.NexusTrayManager(this);
        
        // Initialize Voice Engine & Sound Manager
        new nexus.common.SoundManager();
        
        // Initialize DatabaseService early
        nexus.database.DatabaseService databaseService = new nexus.database.DatabaseService();
        
        // Initialize Services
        DashboardService dashboardService = new DashboardService();
        FinanceService financeService = new FinanceService();
        MarketService marketService = new MarketService();
        TaskService taskService = new TaskService();
        GoalService goalService = new GoalService();
        MediaService mediaService = new MediaService();
        AnalyticsService analyticsService = new AnalyticsService();
        
        nexus.rules.RuleEngineService ruleEngine = new nexus.rules.RuleEngineService();
        ruleEngine.addRule("TASK_COMPLETED", event -> {
            // A custom IFTTT rule: Completing a task automatically gives a tiny budget bonus
            financeService.addFunds("Task Completion Bonus", 5.0);
        });
        
        nexus.realtime.WindowsMediaHook mediaHook = new nexus.realtime.WindowsMediaHook();
        mediaHook.start();
        
        // Populate UI/Services with DB data
        java.util.List<nexus.tasks.Task> loadedTasks = databaseService.loadTasks();
        if (!loadedTasks.isEmpty()) {
            taskService.setTasks(loadedTasks);
        }
        
        java.util.List<nexus.finance.Transaction> loadedTx = databaseService.loadTransactions();
        if (!loadedTx.isEmpty()) {
            financeService.setTransactions(loadedTx);
        }

        
        // Create Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        
        tabbedPane.addTab("Dashboard", new DashboardPanel(dashboardService));
        tabbedPane.addTab("Finance", new FinancePanel(financeService));
        tabbedPane.addTab("Market", new MarketPanel(marketService));
        tabbedPane.addTab("Tasks", new TaskPanel(taskService));
        tabbedPane.addTab("Goals", new GoalPanel(goalService));
        tabbedPane.addTab("Media", new MediaPanel(mediaService));
        tabbedPane.addTab("Analytics", new AnalyticsPanel(analyticsService));
        tabbedPane.addTab("Rules", new nexus.rules.RulePanel(ruleEngine));
        tabbedPane.addTab("V7 Enterprise", new nexus.startup.StartupDashboardPanel());
        
        // Setup CardLayout
        CardLayout cardLayout = new CardLayout();
        JPanel mainContent = new JPanel(cardLayout);
        
        LoginPanel loginPanel = new LoginPanel(ignored -> {
            cardLayout.show(mainContent, "MainApp");
        });
        
        // Add Activity Feed
        JPanel mainAppContainer = new JPanel(new BorderLayout());
        mainAppContainer.add(tabbedPane, BorderLayout.CENTER);
        mainAppContainer.add(new nexus.common.ActivityFeedPanel(), BorderLayout.EAST);
        
        // God Mode Terminal
        JTextField godModeTerminal = new JTextField();
        godModeTerminal.setToolTipText("God Mode Terminal: /budget <amount> or /score <amount>");
        godModeTerminal.setBackground(new Color(20, 20, 20));
        godModeTerminal.setForeground(new Color(0, 255, 0));
        godModeTerminal.setCaretColor(new Color(0, 255, 0));
        godModeTerminal.setFont(new Font("Monospaced", Font.BOLD, 14));
        godModeTerminal.addActionListener(e -> {
            String cmd = godModeTerminal.getText().trim();
            godModeTerminal.setText("");
            if (cmd.startsWith("/budget ")) {
                try {
                    double amount = Double.parseDouble(cmd.substring(8).trim());
                    nexus.common.EventBus.getInstance().publish(new nexus.common.NexusEvent("GOD_MODE_BUDGET", amount, "GodMode"));
                } catch (NumberFormatException ex) {}
            } else if (cmd.startsWith("/score ")) {
                try {
                    int score = Integer.parseInt(cmd.substring(7).trim());
                    nexus.common.EventBus.getInstance().publish(new nexus.common.NexusEvent("GOD_MODE_SCORE", score, "GodMode"));
                } catch (NumberFormatException ex) {}
            }
        });
        mainAppContainer.add(godModeTerminal, BorderLayout.SOUTH);
        
        mainContent.add(loginPanel, "Login");
        mainContent.add(mainAppContainer, "MainApp");
        
        setLayout(new BorderLayout());
        add(mainContent, BorderLayout.CENTER);
        
        // Show Login panel initially
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

