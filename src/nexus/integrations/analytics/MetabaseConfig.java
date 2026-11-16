package nexus.integrations.analytics;

import nexus.integrations.common.IntegrationConfig;

/**
 * Configuration for self-hosted Metabase analytics in NEXUS V26.
 */
public class MetabaseConfig {
    private String baseUrl;
    private String apiKey;
    private String readOnlyDbPath;
    private boolean enabled;
    private String siteName;

    public MetabaseConfig() {
        this.baseUrl = IntegrationConfig.get("nexus.metabase.baseUrl", "http://localhost:3000");
        this.apiKey = IntegrationConfig.get("nexus.metabase.apiKey", "");
        this.readOnlyDbPath = IntegrationConfig.get("nexus.metabase.dbPath", "nexus.db");
        this.enabled = IntegrationConfig.getBoolean("nexus.metabase.enabled", true);
        this.siteName = IntegrationConfig.get("nexus.metabase.siteName", "NEXUS Executive Intelligence");
    }

    public MetabaseConfig(String baseUrl, String apiKey, boolean enabled) {
        this.baseUrl = baseUrl != null ? baseUrl : "http://localhost:3000";
        this.apiKey = apiKey != null ? apiKey : "";
        this.readOnlyDbPath = "nexus.db";
        this.enabled = enabled;
        this.siteName = "NEXUS Executive Intelligence";
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getReadOnlyDbPath() { return readOnlyDbPath; }
    public void setReadOnlyDbPath(String readOnlyDbPath) { this.readOnlyDbPath = readOnlyDbPath; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getSiteName() { return siteName; }

    public String getHealthEndpoint() {
        return baseUrl.replaceAll("/+$", "") + "/api/health";
    }
}
