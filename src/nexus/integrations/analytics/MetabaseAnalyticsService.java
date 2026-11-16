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
 * Metabase Monitoring & Local Analytics Export Service for NEXUS V26.1.
 *
 * Honest Architectural Boundary:
 * - Monitors availability of external self-hosted Metabase instances via /api/health.
 * - Provides read-only data specifications and models for Metabase database connectors.
 * - Executes analytics queries strictly through ReadOnlyAnalyticsStore (PRAGMA query_only = ON).
 * - Distinguishes local NEXUS SQLite metrics from external Metabase dashboards.
 * - Complete local-first autonomy: NEXUS operates 100% normally if Metabase is offline.
 */
public class MetabaseAnalyticsService implements IIntegrationService {
    private final MetabaseConfig config;
    private final ReadOnlyAnalyticsStore readOnlyStore;
    private final HttpClient httpClient;
    private final List<AnalyticsDashboardModel> dashboards;
    private volatile IntegrationStatus currentStatus = IntegrationStatus.UNAVAILABLE;
    private volatile boolean authenticatedSession = false;

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

    public boolean isAuthenticatedSession() {
        return authenticatedSession;
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
     * Does not claim external database synchronization or data retrieval if only health is detected.
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
                IntegrationLogger.log("metabase", "HEALTH_CHECK", true, "HTTP 200 OK (Server online)", latency);
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
     * Generates navigation URL for a dashboard specification.
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
     * Safely executes an allowlisted read-only analytics query.
     * Enforces dual-layer security: application validation and SQLite PRAGMA query_only = ON.
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

    /**
     * Returns documented setup instructions for configuring an external Metabase instance.
     */
    public static String getSetupInstructions() {
        return "To connect an external Metabase instance to NEXUS:\n" +
               "1. Run Metabase with SQLite driver support:\n" +
               "   docker run -d -p 3000:3000 -v /path/to/nexus.db:/data/nexus.db:ro --name metabase metabase/metabase\n" +
               "2. In Metabase Admin > Databases > Add Database, select 'SQLite'.\n" +
               "3. Configure filename as '/data/nexus.db' in Read-Only mode.\n" +
               "4. Import the 5 pre-defined SQL models from AnalyticsDashboardModel into Metabase Cards.";
    }

    @Override
    public void shutdown() {
        // No background thread pools to terminate
    }
}
