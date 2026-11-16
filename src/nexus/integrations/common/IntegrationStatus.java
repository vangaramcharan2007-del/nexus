package nexus.integrations.common;

/**
 * Status enumeration for external integration services in NEXUS V26.
 */
public enum IntegrationStatus {
    HEALTHY("Operational", true),
    DEGRADED("Degraded / Retrying", true),
    UNAVAILABLE("Unavailable / Offline", false),
    DISABLED("Disabled by Configuration", false);

    private final String description;
    private final boolean operational;

    IntegrationStatus(String description, boolean operational) {
        this.description = description;
        this.operational = operational;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOperational() {
        return operational;
    }
}
