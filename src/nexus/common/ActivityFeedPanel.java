package nexus.common;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ActivityFeedPanel extends JPanel implements NexusListener {
    private JTextArea feedArea;

    public ActivityFeedPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(250, 0));
        setBorder(BorderFactory.createTitledBorder("Live Activity Feed"));

        feedArea = new JTextArea();
        feedArea.setEditable(false);
        feedArea.setLineWrap(true);
        feedArea.setWrapStyleWord(true);
        feedArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(feedArea);
        add(scrollPane, BorderLayout.CENTER);

        // Subscribe to interesting events
        EventBus.getInstance().subscribe("TASK_COMPLETED", this);
        EventBus.getInstance().subscribe("GOAL_COMPLETED", this);
        EventBus.getInstance().subscribe("PRODUCT_PURCHASED", this);
        EventBus.getInstance().subscribe("MEDIA_COMPLETED", this);
        EventBus.getInstance().subscribe("MEDIA_ADDED", this);
        EventBus.getInstance().subscribe("INSIGHT_GENERATED", this);
        EventBus.getInstance().subscribe("USER_SWITCHED", this);
        EventBus.getInstance().subscribe("TICKET_BOOKED", this);
    }

    private void logEvent(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        SwingUtilities.invokeLater(() -> {
            feedArea.append("[" + time + "] " + message + "\n\n");
            feedArea.setCaretPosition(feedArea.getDocument().getLength());
        });
    }

    @Override
    public void onEvent(NexusEvent event) {
        if (event == null) return;
        String type = event.getType();
        Object payload = event.getPayload();
        
        if ("TASK_COMPLETED".equals(type)) {
            if (payload instanceof nexus.tasks.Task t) {
                logEvent("Task Completed: " + t.getTitle());
            } else {
                logEvent("Task Completed");
            }
        } else if ("GOAL_COMPLETED".equals(type)) {
            if (payload instanceof nexus.goals.Goal g) {
                logEvent("Goal Achieved! " + g.getTitle() + " (Rewarded +$150)");
            }
        } else if ("PRODUCT_PURCHASED".equals(type)) {
            logEvent("Market Purchase: $" + payload);
        } else if ("MEDIA_COMPLETED".equals(type)) {
            if (payload instanceof nexus.media.MediaItem m) {
                logEvent("Media Finished: " + m.getTitle());
            }
        } else if ("MEDIA_ADDED".equals(type)) {
            if (payload instanceof nexus.media.MediaItem m) {
                logEvent("New Media Added: " + m.getTitle());
            }
        } else if ("INSIGHT_GENERATED".equals(type)) {
            if (payload instanceof java.util.List<?> insights && !insights.isEmpty()) {
                Object last = insights.get(insights.size() - 1);
                if (last instanceof nexus.analytics.Insight insight) {
                    logEvent("Insight: " + insight.getMessage());
                }
            }
        } else if ("USER_SWITCHED".equals(type)) {
            if (payload instanceof nexus.auth.UserProfile p) {
                logEvent("Active Profile: " + p.getAvatar() + " " + p.getName() + " (" + p.getRole() + ")");
            }
        } else if ("TICKET_BOOKED".equals(type)) {
            if (payload instanceof nexus.media.CinemaBooking b) {
                logEvent("🎟️ Ticket Booked: " + b.getMovieTitle() + " Seats: " + b.getSeatsFormatted() + " Paid via: " + b.getPaymentMethod());
            }
        }
    }
}
