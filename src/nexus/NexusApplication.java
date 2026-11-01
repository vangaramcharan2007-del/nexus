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
    private nexus.copilot.AiAssistantDialog aiAssistantDialog;
    
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
                if ("OPEN_AI_ASSISTANT".equals(event.getType())) {
                    SwingUtilities.invokeLater(() -> {
                        if (aiAssistantDialog == null) {
                            aiAssistantDialog = new nexus.copilot.AiAssistantDialog(NexusApplication.this);
                        }
                        aiAssistantDialog.setVisible(true);
                    });
                } else if ("NAVIGATION_REQUEST".equals(event.getType())) {
                    String targetTab = (String) event.getPayload();
                    if (targetTab.contains("Cloud") || targetTab.contains("P8")) {
                        showExaminerAuditDialog(0);
                    } else if (targetTab.contains("Mobile") || targetTab.contains("P9")) {
                        showExaminerAuditDialog(1);
                    } else if (targetTab.contains("Copilot") || targetTab.contains("P10")) {
                        showExaminerAuditDialog(2);
                    } else if (targetTab.contains("Web3") || targetTab.contains("P12")) {
                        showExaminerAuditDialog(3);
                    } else {
                        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                            if (tabbedPane.getTitleAt(i).toLowerCase().contains(targetTab.toLowerCase())) {
                                tabbedPane.setSelectedIndex(i);
                                break;
                            }
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

        JLabel brand = new JLabel("⚡ NEXUS 7.0");
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

        // Right Quick Profile Switcher Dropdown
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JLabel switchLabel = new JLabel("Switch Account:");
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

        JButton aiBtn = new JButton("✨ AI Copilot");
        aiBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        aiBtn.setBackground(new Color(16, 185, 129));
        aiBtn.setForeground(Color.WHITE);
        aiBtn.setFocusPainted(false);
        aiBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        aiBtn.addActionListener(e -> {
            if (aiAssistantDialog == null) {
                aiAssistantDialog = new nexus.copilot.AiAssistantDialog(this);
            }
            aiAssistantDialog.setVisible(true);
        });

        JButton auditBtn = new JButton("🎓 Examiner Review & Architecture ⚙️");
        auditBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        auditBtn.setBackground(new Color(245, 158, 11)); // Amber/Gold accent
        auditBtn.setForeground(Color.BLACK);
        auditBtn.setFocusPainted(false);
        auditBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        auditBtn.addActionListener(e -> showExaminerAuditDialog(0));

        rightPanel.add(aiBtn);
        rightPanel.add(Box.createHorizontalStrut(8));
        rightPanel.add(auditBtn);
        rightPanel.add(Box.createHorizontalStrut(8));
        rightPanel.add(switchLabel);
        rightPanel.add(profileCombo);

        bar.add(leftPanel, BorderLayout.WEST);
        bar.add(rightPanel, BorderLayout.EAST);
        return bar;
    }

    private void showExaminerAuditDialog(int initialSubtab) {
        JDialog dlg = new JDialog(this, "NEXUS Enterprise Architecture & Review 2 Audit Console", true);
        dlg.setSize(1080, 680);
        dlg.setLocationRelativeTo(this);

        JTabbedPane auditTabs = new JTabbedPane();
        auditTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        auditTabs.addTab("⚡ Cloud Microservices & Kafka Mesh (Phase 8)", new nexus.cloud.CloudMeshPanel());
        auditTabs.addTab("📱 Mobile 15s Dynamic QR Engine (Phase 9)", new nexus.mobile.MobilePassPanel());
        auditTabs.addTab("🧠 Autonomous AI Copilot & LSTM (Phase 10)", new nexus.copilot.AiCopilotPanel());
        auditTabs.addTab("🌐 Web3 zk-SNARK & Grade Signer (Phase 12)", new nexus.web3.Web3ProtocolPanel());

        if (initialSubtab >= 0 && initialSubtab < auditTabs.getTabCount()) {
            auditTabs.setSelectedIndex(initialSubtab);
        }

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setBackground(new Color(15, 23, 42));
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JLabel title = new JLabel("🎓 REVIEW 2 ENTERPRISE ARCHITECTURE & AUDIT CONSOLE");
        title.setFont(new Font("Consolas", Font.BOLD, 15));
        title.setForeground(new Color(251, 191, 36)); // Gold
        JLabel subtitle = new JLabel("Designed for Faculty Evaluation & Systems Audit • Student: Ram Charan Vanga");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(new Color(148, 163, 184));
        topBar.add(title, BorderLayout.NORTH);
        topBar.add(subtitle, BorderLayout.SOUTH);

        wrapper.add(topBar, BorderLayout.NORTH);
        wrapper.add(auditTabs, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close Audit Console");
        closeBtn.setBackground(new Color(51, 65, 85));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.addActionListener(e -> dlg.dispose());
        JPanel btm = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btm.setOpaque(false);
        btm.add(closeBtn);
        wrapper.add(btm, BorderLayout.SOUTH);

        dlg.setContentPane(wrapper);
        dlg.setVisible(true);
    }
}
