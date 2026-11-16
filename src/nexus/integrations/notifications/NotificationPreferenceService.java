package nexus.integrations.notifications;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user-configurable notification preferences in NEXUS V26.
 */
public class NotificationPreferenceService {
    private final Map<String, Boolean> eventPreferences = new ConcurrentHashMap<>();
    private volatile boolean muted = false;
    private volatile int minPriority = 1;

    public NotificationPreferenceService() {
        // Defaults: all core business events enabled
        eventPreferences.put("TASK_COMPLETED", true);
        eventPreferences.put("GOAL_COMPLETED", true);
        eventPreferences.put("BUDGET_UPDATED", true);
        eventPreferences.put("PRODUCT_PURCHASED", true);
        eventPreferences.put("TICKET_BOOKED", true);
    }

    public boolean isEventEnabled(String eventType) {
        if (muted) return false;
        return eventPreferences.getOrDefault(eventType, false);
    }

    public void setEventEnabled(String eventType, boolean enabled) {
        eventPreferences.put(eventType, enabled);
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public int getMinPriority() {
        return minPriority;
    }

    public void setMinPriority(int minPriority) {
        this.minPriority = minPriority;
    }

    public Map<String, Boolean> getAllPreferences() {
        return new ConcurrentHashMap<>(eventPreferences);
    }
}
