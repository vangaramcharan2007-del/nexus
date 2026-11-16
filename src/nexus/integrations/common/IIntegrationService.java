package nexus.integrations.common;

/**
 * Common lifecycle and health contract for all NEXUS external integrations.
 */
public interface IIntegrationService {
    /**
     * Unique identifier for the integration (e.g., "ntfy", "metabase").
     */
    String getIntegrationName();

    /**
     * Current operational status of the integration.
     */
    IntegrationStatus getStatus();

    /**
     * Whether this integration is actively enabled in settings.
     */
    boolean isEnabled();

    /**
     * Performs a non-blocking or lightweight health check.
     * @return true if the external service is reachable and responsive.
     */
    boolean checkHealth();

    /**
     * Gracefully tears down any underlying thread pools or connections.
     */
    void shutdown();
}
