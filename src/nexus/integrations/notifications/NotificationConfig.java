package nexus.integrations.notifications;

import nexus.integrations.common.IntegrationConfig;

/**
 * Configuration for ntfy push notifications in NEXUS V26.
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
        this.topic = IntegrationConfig.get("nexus.ntfy.topic", "nexus-alerts-" + System.getProperty("user.name", "core"));
        this.authToken = IntegrationConfig.get("nexus.ntfy.token", "");
        this.enabled = IntegrationConfig.getBoolean("nexus.ntfy.enabled", true);
        this.maxRetries = IntegrationConfig.getInt("nexus.ntfy.maxRetries", 3);
        this.initialRetryDelayMs = IntegrationConfig.getInt("nexus.ntfy.retryDelayMs", 500);
    }

    public NotificationConfig(String serverUrl, String topic, String authToken, boolean enabled) {
        this.serverUrl = serverUrl != null ? serverUrl : "https://ntfy.sh";
        this.topic = topic != null ? topic : "nexus-alerts";
        this.authToken = authToken != null ? authToken : "";
        this.enabled = enabled;
        this.maxRetries = 3;
        this.initialRetryDelayMs = 500;
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
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public long getInitialRetryDelayMs() { return initialRetryDelayMs; }
    public void setInitialRetryDelayMs(long initialRetryDelayMs) { this.initialRetryDelayMs = initialRetryDelayMs; }

    public String getPublishEndpoint() {
        String base = serverUrl.replaceAll("/+$", "");
        String t = topic.replaceAll("^/+", "");
        return base + "/" + t;
    }
}
