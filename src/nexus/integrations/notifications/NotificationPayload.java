package nexus.integrations.notifications;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nexus.finance.Budget;
import nexus.goals.Goal;
import nexus.media.CinemaBooking;
import nexus.tasks.Task;

/**
 * Encapsulates a push notification payload for ntfy in NEXUS V26.
 *
 * Security:
 * - Automatically scrubs sensitive financial numbers, card patterns, passwords, and tokens.
 * - Enforces title and body length limits.
 */
public class NotificationPayload {
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(?i)(password|token|secret|apiKey)\\s*[:=]\\s*\\S+");

    private final String title;
    private final String message;
    private final int priority; // 1 (min) to 5 (urgent)
    private final List<String> tags;
    private final long timestamp;

    public NotificationPayload(String title, String message, int priority, List<String> tags) {
        this.title = sanitize(title, 80);
        this.message = sanitize(message, 1000);
        this.priority = Math.max(1, Math.min(5, priority));
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public static String sanitize(String input, int maxLength) {
        if (input == null) return "";
        // Scrub credit card numbers
        String cleaned = CREDIT_CARD_PATTERN.matcher(input).replaceAll("[REDACTED_CARD]");
        // Scrub secrets
        cleaned = SECRET_PATTERN.matcher(cleaned).replaceAll("$1=[REDACTED]");
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength - 3) + "...";
        }
        return cleaned.trim();
    }

    public static NotificationPayload fromTaskCompleted(Task task) {
        String msg = task != null ? "Task marked completed: " + task.getTitle() : "Task completed.";
        return new NotificationPayload("Task Completed", msg, 3, Arrays.asList("white_check_mark", "clipboard"));
    }

    public static NotificationPayload fromGoalCompleted(Goal goal) {
        String msg = goal != null ? "Strategic milestone reached: " + goal.getTitle() : "Goal completed.";
        return new NotificationPayload("Goal Achieved", msg, 4, Arrays.asList("tada", "trophy"));
    }

    public static NotificationPayload fromBudgetUpdated(Budget budget) {
        String msg = budget != null ? String.format("Current remaining balance: ₹%,.2f", budget.getRemaining()) : "Budget updated.";
        return new NotificationPayload("Budget Balance", msg, 2, Arrays.asList("chart_with_upwards_trend", "wallet"));
    }

    public static NotificationPayload fromProductPurchased(double amount) {
        String msg = String.format("Campus store purchase processed: ₹%,.2f", amount);
        return new NotificationPayload("Order Settled", msg, 3, Arrays.asList("shopping_cart", "package"));
    }

    public static NotificationPayload fromTicketBooked(CinemaBooking booking) {
        String msg = booking != null ? 
            String.format("Cinema ticket confirmed: %s at %s (%s)", booking.getMovieTitle(), booking.getTheatre(), booking.getSeatsFormatted()) :
            "Cinema booking confirmed.";
        return new NotificationPayload("Ticket Booked", msg, 3, Arrays.asList("ticket", "movie_camera"));
    }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public int getPriority() { return priority; }
    public List<String> getTags() { return new ArrayList<>(tags); }
    public long getTimestamp() { return timestamp; }

    public String getTagsHeader() {
        return String.join(",", tags);
    }
}
