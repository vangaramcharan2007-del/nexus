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
        
        // Initialize Voice Engine
        
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
        
        // Setup CardLayout
        CardLayout cardLayout = new CardLayout();
        JPanel mainContent = new JPanel(cardLayout);
        
        LoginPanel loginPanel = new LoginPanel(ignored -> {
            cardLayout.show(mainContent, "MainApp");
        });
        
        mainContent.add(loginPanel, "Login");
        mainContent.add(tabbedPane, "MainApp");
        
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

