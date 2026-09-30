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

public class NexusApplication extends JFrame {
    
    public NexusApplication() {
        setTitle("NEXUS Application");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Initialize Voice Engine
        
        // Initialize Services
        DashboardService dashboardService = new DashboardService();
        FinanceService financeService = new FinanceService();
        MarketService marketService = new MarketService();
        TaskService taskService = new TaskService();
        GoalService goalService = new GoalService();
        MediaService mediaService = new MediaService();
        AnalyticsService analyticsService = new AnalyticsService();
        
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
    }
}
