package nexus.integrations;

import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import nexus.integrations.analytics.AnalyticsDashboardModel;
import nexus.integrations.analytics.MetabaseAnalyticsService;
import nexus.integrations.common.IntegrationLogger;
import nexus.integrations.common.IntegrationStatus;
import nexus.integrations.notifications.NotificationPayload;
import nexus.integrations.notifications.NtfyNotificationService;

/**
 * Visual control center for NEXUS V26.1 External Integrations (ntfy & Metabase).
 * Displays real-time operational health, connection telemetry, public topic security alerts,
 * and transparent separation between local analytics and external BI platforms.
 */
public class IntegrationsPanel extends JPanel {
    private final NtfyNotificationService ntfyService;
    private final MetabaseAnalyticsService metabaseService;

    private JLabel ntfyStatusLabel;
    private JLabel ntfySecurityWarningLabel;
    private JLabel metabaseStatusLabel;
    private JLabel ntfyMetricsLabel;
    private JTextArea auditLogArea;

    public IntegrationsPanel(NtfyNotificationService ntfyService, MetabaseAnalyticsService metabaseService) {
        this.ntfyService = ntfyService;
        this.metabaseService = metabaseService;

        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 25, 20, 25));
        setBackground(new Color(24, 27, 32));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel titleLabel = new JLabel("🔗 External Integrations & Telemetry");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(240, 243, 246));

        JLabel subLabel = new JLabel("V26.1 Enterprise Service Bus: ntfy Push Notifications & Optional Metabase Analytics Layer");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(new Color(139, 148, 158));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subLabel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Center Cards: ntfy on Left, Metabase on Right
        JPanel cardsGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        cardsGrid.setOpaque(false);

        cardsGrid.add(createNtfyCard());
        cardsGrid.add(createMetabaseCard());

        // Bottom Section: Audit Log & Diagnostics
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);
        JLabel auditTitle = new JLabel("Integration Telemetry & Event Stream (Verified Responses)");
        auditTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        auditTitle.setForeground(new Color(201, 209, 217));

        auditLogArea = new JTextArea(6, 60);
        auditLogArea.setEditable(false);
        auditLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        auditLogArea.setBackground(new Color(13, 17, 23));
        auditLogArea.setForeground(new Color(88, 166, 255));
        auditLogArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(auditLogArea);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(48, 54, 61)));

        bottomPanel.add(auditTitle, BorderLayout.NORTH);
        bottomPanel.add(scroll, BorderLayout.CENTER);

        JPanel centerContainer = new JPanel(new BorderLayout(0, 15));
        centerContainer.setOpaque(false);
        centerContainer.add(cardsGrid, BorderLayout.CENTER);
        centerContainer.add(bottomPanel, BorderLayout.SOUTH);

        add(centerContainer, BorderLayout.CENTER);

        refreshState();
    }

    private JPanel createNtfyCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(33, 38, 45));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(48, 54, 61), 1, true),
                new EmptyBorder(15, 18, 15, 18)
        ));

        JLabel title = new JLabel("📢 ntfy Push Notifications");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(240, 243, 246));
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        ntfyStatusLabel = new JLabel("Status: Checking...");
        ntfyStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        card.add(ntfyStatusLabel);
        card.add(Box.createVerticalStrut(6));

        String server = ntfyService != null ? ntfyService.getConfig().getServerUrl() : "https://ntfy.sh";
        String topic = ntfyService != null ? ntfyService.getConfig().getTopic() : "nexus-alerts";
        JLabel endpointLabel = new JLabel("Target Endpoint: " + server + "/" + topic);
        endpointLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        endpointLabel.setForeground(new Color(139, 148, 158));
        card.add(endpointLabel);
        card.add(Box.createVerticalStrut(6));

        ntfyMetricsLabel = new JLabel("Delivered: 0 | Failed: 0");
        ntfyMetricsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ntfyMetricsLabel.setForeground(new Color(139, 148, 158));
        card.add(ntfyMetricsLabel);
        card.add(Box.createVerticalStrut(6));

        ntfySecurityWarningLabel = new JLabel("");
        ntfySecurityWarningLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ntfySecurityWarningLabel.setForeground(new Color(227, 179, 65));
        card.add(ntfySecurityWarningLabel);
        card.add(Box.createVerticalStrut(12));

        JButton sendTestBtn = new JButton("⚡ Dispatch Manual Test Notification");
        sendTestBtn.setBackground(new Color(35, 134, 54));
        sendTestBtn.setForeground(Color.WHITE);
        sendTestBtn.setFocusPainted(false);
        sendTestBtn.addActionListener(e -> {
            if (ntfyService != null) {
                NotificationPayload p = new NotificationPayload(
                        "NEXUS V26.1 Telemetry Ping",
                        "Manual diagnostic test notification from NEXUS Enterprise Console.",
                        3,
                        java.util.Arrays.asList("satellite", "bell")
                );
                ntfyService.sendNotificationAsync(p);
                Timer t = new Timer(800, ev -> refreshState());
                t.setRepeats(false);
                t.start();
            }
        });
        card.add(sendTestBtn);

        return card;
    }

    private JPanel createMetabaseCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(33, 38, 45));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(48, 54, 61), 1, true),
                new EmptyBorder(15, 18, 15, 18)
        ));

        JLabel title = new JLabel("📊 Metabase Optional Analytics Layer");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(240, 243, 246));
        card.add(title);
        card.add(Box.createVerticalStrut(10));

        metabaseStatusLabel = new JLabel("Status: Checking...");
        metabaseStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        card.add(metabaseStatusLabel);
        card.add(Box.createVerticalStrut(6));

        String url = metabaseService != null ? metabaseService.getConfig().getBaseUrl() : "http://localhost:3000";
        JLabel urlLabel = new JLabel("Base URL: " + url);
        urlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        urlLabel.setForeground(new Color(139, 148, 158));
        card.add(urlLabel);
        card.add(Box.createVerticalStrut(6));

        JLabel roBadge = new JLabel("🔒 Database Protection: PRAGMA query_only = ON (Mutations Blocked)");
        roBadge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        roBadge.setForeground(new Color(63, 185, 80));
        card.add(roBadge);
        card.add(Box.createVerticalStrut(10));

        JLabel dashTitle = new JLabel("Analytics Specifications (NEXUS Local Analytics):");
        dashTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dashTitle.setForeground(new Color(201, 209, 217));
        card.add(dashTitle);
        card.add(Box.createVerticalStrut(4));

        if (metabaseService != null) {
            for (AnalyticsDashboardModel dm : metabaseService.getDashboards()) {
                JLabel dLabel = new JLabel("• " + dm.getName() + " (" + dm.getCategory() + ")");
                dLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                dLabel.setForeground(new Color(139, 148, 158));
                card.add(dLabel);
            }
        }
        card.add(Box.createVerticalStrut(10));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnRow.setOpaque(false);

        JButton checkBtn = new JButton("🔄 Re-Check Health");
        checkBtn.setBackground(new Color(31, 111, 235));
        checkBtn.setForeground(Color.WHITE);
        checkBtn.setFocusPainted(false);
        checkBtn.addActionListener(e -> {
            new Thread(() -> {
                if (metabaseService != null) metabaseService.checkHealth();
                if (ntfyService != null) ntfyService.checkHealth();
                SwingUtilities.invokeLater(this::refreshState);
            }).start();
        });
        btnRow.add(checkBtn);

        JButton infoBtn = new JButton("ℹ️ Setup Guide");
        infoBtn.setBackground(new Color(48, 54, 61));
        infoBtn.setForeground(Color.WHITE);
        infoBtn.setFocusPainted(false);
        infoBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, 
                MetabaseAnalyticsService.getSetupInstructions(), 
                "Metabase External Setup Guide", 
                JOptionPane.INFORMATION_MESSAGE);
        });
        btnRow.add(infoBtn);

        card.add(btnRow);

        return card;
    }

    public void refreshState() {
        if (ntfyService != null) {
            IntegrationStatus s = ntfyService.getStatus();
            ntfyStatusLabel.setText("Gateway Reachability: " + s.getDescription());
            ntfyStatusLabel.setForeground(s.isOperational() ? new Color(63, 185, 80) : new Color(248, 81, 73));
            ntfyMetricsLabel.setText(String.format("Delivered: %d (HTTP 2xx Verified) | Failed: %d",
                    ntfyService.getDeliveredCount(), ntfyService.getFailedCount()));

            if (ntfyService.getConfig().isPublicServerWithoutAuth()) {
                ntfySecurityWarningLabel.setText("<html>⚠️ <b>Security Notice:</b> Public topic in use without auth. Configure private topic for security.</html>");
            } else {
                ntfySecurityWarningLabel.setText("🔒 Private / Authenticated Channel Active.");
                ntfySecurityWarningLabel.setForeground(new Color(63, 185, 80));
            }
        }

        if (metabaseService != null) {
            IntegrationStatus s = metabaseService.getStatus();
            if (s == IntegrationStatus.HEALTHY) {
                metabaseStatusLabel.setText("Status: Server Reachable (Optional BI Monitoring)");
                metabaseStatusLabel.setForeground(new Color(63, 185, 80));
            } else if (s == IntegrationStatus.DISABLED) {
                metabaseStatusLabel.setText("Status: Disabled by Configuration");
                metabaseStatusLabel.setForeground(new Color(139, 148, 158));
            } else {
                metabaseStatusLabel.setText("Status: Offline / Not Connected (NEXUS Local Analytics Active)");
                metabaseStatusLabel.setForeground(new Color(227, 179, 65));
            }
        }

        // Refresh audit log
        StringBuilder sb = new StringBuilder();
        List<IntegrationLogger.LogEntry> logs = IntegrationLogger.getLogs();
        int start = Math.max(0, logs.size() - 8);
        for (int i = start; i < logs.size(); i++) {
            sb.append(logs.get(i).toString()).append("\n");
        }
        if (sb.length() == 0) {
            sb.append("[Ready] No external operations recorded yet. Event listeners armed.\n");
        }
        auditLogArea.setText(sb.toString());
    }
}
