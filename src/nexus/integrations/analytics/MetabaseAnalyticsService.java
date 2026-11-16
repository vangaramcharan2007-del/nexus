package nexus.integrations.analytics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nexus.integrations.common.HttpHelper;
import nexus.integrations.common.IIntegrationService;
import nexus.integrations.common.IntegrationLogger;
import nexus.integrations.common.IntegrationStatus;

/**
 * Metabase Self-Hosted Analytics Integration Service for NEXUS V26.
 *
 * Requirements:
 * - Support self-hosted Metabase instances.
 * - Enforces strictly read-only access to prevent mutation of business data.
 * - Models the 5 mandatory dashboards (Financial overview, Monthly spending, 
 *   Task trends, Goal progression, Productivity overview).
 * - Distinguishes verified real metrics from simulated projections.
 * - Optionality: If Metabase is unavailable or offline, NEXUS operates normally without disruption.
 */
public class MetabaseAnalyticsService implements IIntegrationService {
    private final MetabaseConfig config;
    private final ReadOnlyAnalyticsStore readOnlyStore;
    private final HttpClient httpClient;
    private final List<AnalyticsDashboardModel> dashboards;
    private volatile IntegrationStatus currentStatus = IntegrationStatus.UNAVAILABLE;

    public MetabaseAnalyticsService() {
        this(new MetabaseConfig(), new ReadOnlyAnalyticsStore(), HttpHelper.getDefaultClient());
    }

    public MetabaseAnalyticsService(MetabaseConfig config, ReadOnlyAnalyticsStore store, HttpClient httpClient) {
        this.config = config != null ? config : new MetabaseConfig();
        this.readOnlyStore = store != null ? store : new ReadOnlyAnalyticsStore();
        this.httpClient = httpClient != null ? httpClient : HttpHelper.getDefaultClient();
        this.dashboards = AnalyticsDashboardModel.getDefaultDashboards();

        if (this.config.isEnabled()) {
            checkHealth();
        } else {
            this.currentStatus = IntegrationStatus.DISABLED;
        }
    }

    @Override
    public String getIntegrationName() {
        return "metabase";
    }

    @Override
    public IntegrationStatus getStatus() {
        return currentStatus;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    public MetabaseConfig getConfig() {
        return config;
    }

    public ReadOnlyAnalyticsStore getReadOnlyStore() {
        return readOnlyStore;
    }

    public List<AnalyticsDashboardModel> getDashboards() {
        return new ArrayList<>(dashboards);
    }

    /**
     * Checks health against self-hosted Metabase health endpoint (/api/health).
     * Guarantees zero unhandled exceptions on network timeout or connection refused.
     */
    @Override
    public boolean checkHealth() {
        if (!config.isEnabled()) {
            currentStatus = IntegrationStatus.DISABLED;
            return false;
        }

        long start = System.currentTimeMillis();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(config.getHealthEndpoint()))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - start;

            if (resp.statusCode() == 200) {
                currentStatus = IntegrationStatus.HEALTHY;
                IntegrationLogger.log("metabase", "HEALTH_CHECK", true, "HTTP 200 OK", latency);
                return true;
            } else {
                currentStatus = IntegrationStatus.DEGRADED;
                IntegrationLogger.log("metabase", "HEALTH_CHECK", false, "HTTP " + resp.statusCode(), latency);
                return false;
            }

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            currentStatus = IntegrationStatus.UNAVAILABLE;
            IntegrationLogger.log("metabase", "HEALTH_CHECK", false, "Offline: " + e.getMessage(), latency);
            return false;
        }
    }

    /**
     * Generates the web navigation URL for a given dashboard ID.
     */
    public String getDashboardUrl(int dashboardId) {
        AnalyticsDashboardModel model = dashboards.stream()
                .filter(d -> d.getId() == dashboardId)
                .findFirst()
                .orElse(null);
        if (model == null) return config.getBaseUrl();
        return config.getBaseUrl().replaceAll("/+$", "") + "/dashboard/" + model.getId() + "-" + model.getSlug();
    }

    /**
     * Safely executes a read-only analytics query against the read-only SQLite connection.
     * Guarantees that write operations are rejected and cannot modify business data.
     */
    public List<Map<String, Object>> executeReadOnlyQuery(String sql) throws Exception {
        if (!ReadOnlyAnalyticsStore.isReadOnlyQuery(sql)) {
            throw new SecurityException("Mutation queries are strictly prohibited on the analytics interface: " + sql);
        }

        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = readOnlyStore.getReadOnlyConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int colCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
                results.add(row);
            }
        }
        return results;
    }

    @Override
    public void shutdown() {
        // Metabase is external service; no background daemon threads to drain
    }
}
