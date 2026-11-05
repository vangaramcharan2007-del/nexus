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
 * Author: Ram Charan Vanga (Reg No: RA2511027010164)
 * Department of Computer Science & Engineering
 * SRM Institute of Science and Technology, Kattankulathur
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
        setTitle("NEXUS Enterprise Platform - V11.0 Real-Time Cinema Edition");
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
        
        // Create Production Tabs (Clean, consumer-friendly, high UX)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        tabbedPane.addTab("🏠 Dashboard", new DashboardPanel(dashboardService));
        tabbedPane.addTab("🎬 Cinema & Shows", new MediaPanel(mediaService));
        tabbedPane.addTab("🛒 Campus Store", new MarketPanel(marketService));
        tabbedPane.addTab("💳 Smart Finance", new FinancePanel(financeService));
        tabbedPane.addTab("📋 Tasks", new TaskPanel(taskService));
        tabbedPane.addTab("🎯 Strategic Goals", new GoalPanel(goalService));
        tabbedPane.addTab("📊 Executive Analytics", new AnalyticsPanel(analyticsService));
        
        // Setup CardLayout with Login
        CardLayout cardLayout = new CardLayout();
        JPanel mainContent = new JPanel(cardLayout);
        
        LoginPanel loginPanel = new LoginPanel(ignored -> {
            cardLayout.show(mainContent, "MainApp");
        });
        
        // Main Container with Activity Feed & Top Account Header Bar
        JPanel mainAppContainer = new JPanel(new BorderLayout());
        mainAppContainer.add(createAccountHeaderBar(), BorderLayout.NORTH);
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
                if ("NAVIGATION_REQUEST".equals(event.getType()) && event.getPayload() instanceof String targetTab) {
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        if (tabbedPane.getTitleAt(i).toLowerCase().contains(targetTab.toLowerCase())) {
                            tabbedPane.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });
    }

    private JPanel createAccountHeaderBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(new Color(15, 23, 42));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(51, 65, 85)),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        // Left Branding & Active User Card
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel brand = new JLabel("⚡ NEXUS 11.0");
        brand.setFont(new Font("Consolas", Font.BOLD, 15));
        brand.setForeground(new Color(0, 255, 255));

        JLabel sep = new JLabel(" | ");
        sep.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sep.setForeground(new Color(71, 85, 105));

        nexus.auth.UserProfile current = nexus.auth.AccountService.getInstance().getCurrentProfile();
        JLabel userAvatar = new JLabel(current != null ? current.getAvatar() : "👨‍💻");
        userAvatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        JLabel userName = new JLabel(current != null ? current.getName() : "Ram Charan Vanga");
        userName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userName.setForeground(Color.WHITE);

        JLabel userRole = new JLabel(current != null ? "(" + current.getRole() + ")" : "");
        userRole.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        userRole.setForeground(new Color(148, 163, 184));

        JLabel tierChip = new JLabel(current != null ? " " + current.getTier() + " " : "");
        tierChip.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tierChip.setOpaque(true);
        tierChip.setBackground(new Color(30, 58, 138));
        tierChip.setForeground(new Color(147, 197, 253));
        tierChip.setBorder(BorderFactory.createLineBorder(new Color(59, 130, 246), 1));

        leftPanel.add(brand);
        leftPanel.add(sep);
        leftPanel.add(userAvatar);
        leftPanel.add(userName);
        leftPanel.add(userRole);
        leftPanel.add(tierChip);

        // Right Quick Profile Switcher Dropdown (Clean, non-overwhelming core controls)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel coreBadge = new JLabel(" REALTIME V11 ");
        coreBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        coreBadge.setOpaque(true);
        coreBadge.setBackground(new Color(16, 185, 129, 40));
        coreBadge.setForeground(new Color(52, 211, 153));
        coreBadge.setBorder(BorderFactory.createLineBorder(new Color(16, 185, 129), 1));

        JLabel switchLabel = new JLabel("Active Profile:");
        switchLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        switchLabel.setForeground(new Color(148, 163, 184));

        JComboBox<nexus.auth.UserProfile> profileCombo = new JComboBox<>(
                nexus.auth.AccountService.getInstance().getProfiles().toArray(new nexus.auth.UserProfile[0]));
        profileCombo.setFont(new Font("Segoe UI", Font.BOLD, 11));
        profileCombo.setBackground(new Color(30, 41, 59));
        profileCombo.setForeground(Color.WHITE);
        profileCombo.setPreferredSize(new Dimension(240, 28));

        profileCombo.addActionListener(e -> {
            nexus.auth.UserProfile sel = (nexus.auth.UserProfile) profileCombo.getSelectedItem();
            if (sel != null) {
                nexus.auth.AccountService.getInstance().switchProfile(sel);
            }
        });

        // Listen for USER_SWITCHED to sync UI
        nexus.common.EventBus.getInstance().subscribe("USER_SWITCHED", new nexus.common.NexusListener() {
            @Override
            public void onEvent(nexus.common.NexusEvent evt) {
                SwingUtilities.invokeLater(() -> {
                    nexus.auth.UserProfile p = (nexus.auth.UserProfile) evt.getPayload();
                    if (p != null) {
                        userAvatar.setText(p.getAvatar());
                        userName.setText(p.getName());
                        userRole.setText("(" + p.getRole() + ")");
                        tierChip.setText(" " + p.getTier() + " ");
                        if (profileCombo.getSelectedItem() != p) {
                            profileCombo.setSelectedItem(p);
                        }
                    }
                });
            }
        });

        rightPanel.add(coreBadge);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(switchLabel);
        rightPanel.add(profileCombo);

        bar.add(leftPanel, BorderLayout.WEST);
        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }
}
