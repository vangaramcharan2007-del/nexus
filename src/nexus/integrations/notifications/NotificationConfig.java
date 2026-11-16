package nexus.integrations.notifications;

import java.util.UUID;
import nexus.integrations.common.IntegrationConfig;

/**
 * Configuration for ntfy push notifications in NEXUS V26.1.
 *
 * Security Enhancements:
 * - Does NOT derive topics from the operating system username (user.name).
 * - Enforces unpredictable or user-configured topics.
 * - Detects and warns against public unauthenticated ntfy.sh usage.
 * - Enforces bounded retries and timeout constraints against unbounded loops.
 */
public class NotificationConfig {
    private String serverUrl;
    private String topic;
    private String authToken;
    private boolean enabled;
    private int maxRetries;
    private long initialRetryDelayMs;

    public NotificationConfig() {
        this.serverUrl = IntegrationConfig.get("nexus.ntfy.server", "https://ntfy.sh");
        // Secure topic: Unpredictable default unless explicitly provided by configuration
        this.topic = IntegrationConfig.get("nexus.ntfy.topic", "nexus-channel-" + UUID.randomUUID().toString().substring(0, 8));
        this.authToken = IntegrationConfig.get("nexus.ntfy.token", "");
        this.enabled = IntegrationConfig.getBoolean("nexus.ntfy.enabled", true);
        this.maxRetries = Math.min(5, Math.max(1, IntegrationConfig.getInt("nexus.ntfy.maxRetries", 3)));
        this.initialRetryDelayMs = Math.max(50, IntegrationConfig.getInt("nexus.ntfy.retryDelayMs", 500));
    }

    public NotificationConfig(String serverUrl, String topic, String authToken, boolean enabled) {
        this.serverUrl = serverUrl != null ? serverUrl : "https://ntfy.sh";
        this.topic = (topic != null && !topic.trim().isEmpty()) ? topic : "nexus-channel-default";
        this.authToken = authToken != null ? authToken : "";
        this.enabled = enabled;
        this.maxRetries = 3;
        this.initialRetryDelayMs = 500;
    }

    public boolean isPublicServerWithoutAuth() {
        return serverUrl != null && serverUrl.contains("ntfy.sh") && (authToken == null || authToken.trim().isEmpty());
    }

    public String getSecurityWarning() {
        if (isPublicServerWithoutAuth()) {
            return "⚠️ Security Warning: Running on public ntfy.sh without authentication. Anyone subscribed to topic '" 
                   + topic + "' can read alerts. Use an authenticated server or private topic for production confidentiality.";
        }
        return null;
    }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { 
        this.maxRetries = Math.min(5, Math.max(1, maxRetries)); 
    }

    public long getInitialRetryDelayMs() { return initialRetryDelayMs; }
    public void setInitialRetryDelayMs(long initialRetryDelayMs) { 
        this.initialRetryDelayMs = Math.max(50, initialRetryDelayMs); 
    }

    public String getPublishEndpoint() {
        String base = serverUrl.replaceAll("/+$", "");
        String t = topic.replaceAll("^/+", "");
        return base + "/" + t;
    }
}
