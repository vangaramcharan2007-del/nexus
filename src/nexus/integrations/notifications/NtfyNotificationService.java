package nexus.integrations.notifications;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import nexus.finance.Budget;
import nexus.goals.Goal;
import nexus.integrations.common.HttpHelper;
import nexus.integrations.common.IIntegrationService;
import nexus.integrations.common.IntegrationLogger;
import nexus.integrations.common.IntegrationStatus;
import nexus.media.CinemaBooking;
import nexus.tasks.Task;

/**
 * Ntfy Push Notification Integration Service for NEXUS V26.1.
 *
 * Security & Verification Principles:
 * - Emits clear warnings when running on public unauthenticated endpoints.
 * - Non-blocking asynchronous delivery outside the Swing EDT.
 * - Retry with exponential backoff on transient network faults.
 * - Transparent delivery tracking: deliveredCount increments only upon HTTP 2xx response.
 * - Server health checks accurately represent gateway reachability, not handset delivery.
 */
public class NtfyNotificationService implements NexusListener, IIntegrationService {
    
    public static class NotificationRecord {
        private final long timestamp;
        private final String topic;
        private final String title;
        private final String message;
        private final String status; // "DELIVERED", "FAILED", "FILTERED"
        private final int attemptCount;
        private final int responseCode;

        public NotificationRecord(String topic, String title, String message, String status, int attemptCount, int responseCode) {
            this.timestamp = System.currentTimeMillis();
            this.topic = topic;
            this.title = title;
            this.message = message;
            this.status = status;
            this.attemptCount = attemptCount;
            this.responseCode = responseCode;
        }

        public long getTimestamp() { return timestamp; }
        public String getTopic() { return topic; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getStatus() { return status; }
        public int getAttemptCount() { return attemptCount; }
        public int getResponseCode() { return responseCode; }
    }

    private final NotificationConfig config;
    private final NotificationPreferenceService preferenceService;
    private final HttpClient httpClient;
    private final ExecutorService dispatchQueue;
    private final List<NotificationRecord> history = new CopyOnWriteArrayList<>();
    
    private volatile IntegrationStatus currentStatus = IntegrationStatus.HEALTHY;
    private final AtomicInteger deliveredCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private volatile long lastDeliveryTimestamp = 0;

    public NtfyNotificationService() {
        this(new NotificationConfig(), new NotificationPreferenceService(), HttpHelper.getDefaultClient());
    }

    public NtfyNotificationService(NotificationConfig config, NotificationPreferenceService prefService, HttpClient httpClient) {
        this.config = config != null ? config : new NotificationConfig();
        this.preferenceService = prefService != null ? prefService : new NotificationPreferenceService();
        this.httpClient = httpClient != null ? httpClient : HttpHelper.getDefaultClient();
        
        this.dispatchQueue = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "nexus-ntfy-dispatcher");
            t.setDaemon(true);
            return t;
        });

        if (this.config.isPublicServerWithoutAuth()) {
            System.err.println(this.config.getSecurityWarning());
        }

        if (this.config.isEnabled()) {
            registerSubscriptions();
        } else {
            this.currentStatus = IntegrationStatus.DISABLED;
        }
    }

    private void registerSubscriptions() {
        EventBus.getInstance().subscribe("TASK_COMPLETED", this);
        EventBus.getInstance().subscribe("GOAL_COMPLETED", this);
        EventBus.getInstance().subscribe("BUDGET_UPDATED", this);
        EventBus.getInstance().subscribe("PRODUCT_PURCHASED", this);
        EventBus.getInstance().subscribe("TICKET_BOOKED", this);
    }

    @Override
    public String getIntegrationName() {
        return "ntfy";
    }

    @Override
    public IntegrationStatus getStatus() {
        return currentStatus;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    public NotificationConfig getConfig() {
        return config;
    }

    public NotificationPreferenceService getPreferenceService() {
        return preferenceService;
    }

    public List<NotificationRecord> getHistory() {
        return new ArrayList<>(history);
    }

    public int getDeliveredCount() {
        return deliveredCount.get();
    }

    public int getFailedCount() {
        return failedCount.get();
    }

    public long getLastDeliveryTimestamp() {
        return lastDeliveryTimestamp;
    }

    /**
     * Probes the ntfy server endpoint to verify gateway reachability.
     * Note: A successful health probe verifies HTTP connectivity to the ntfy server;
     * it does not constitute proof of delivery to an end-user mobile handset.
     */
    @Override
    public boolean checkHealth() {
        if (!config.isEnabled()) {
            currentStatus = IntegrationStatus.DISABLED;
            return false;
        }
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(config.getServerUrl()))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            boolean ok = resp.statusCode() >= 200 && resp.statusCode() < 400;
            currentStatus = ok ? IntegrationStatus.HEALTHY : IntegrationStatus.DEGRADED;
            return ok;
        } catch (Exception e) {
            currentStatus = IntegrationStatus.UNAVAILABLE;
            return false;
        }
    }

    @Override
    public void shutdown() {
        EventBus.getInstance().unsubscribeFromAll(this);
        dispatchQueue.shutdown();
        try {
            if (!dispatchQueue.awaitTermination(3, TimeUnit.SECONDS)) {
                dispatchQueue.shutdownNow();
            }
        } catch (InterruptedException e) {
            dispatchQueue.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void onEvent(NexusEvent event) {
        if (event == null || !config.isEnabled()) return;
        
        String type = event.getType();
        if (!preferenceService.isEventEnabled(type)) {
            return;
        }

        NotificationPayload payload = buildPayload(event);
        if (payload != null && payload.getPriority() >= preferenceService.getMinPriority()) {
            sendNotificationAsync(payload);
        }
    }

    private NotificationPayload buildPayload(NexusEvent event) {
        String type = event.getType();
        Object p = event.getPayload();
        
        if ("TASK_COMPLETED".equals(type) && p instanceof Task) {
            return NotificationPayload.fromTaskCompleted((Task) p);
        } else if ("GOAL_COMPLETED".equals(type) && p instanceof Goal) {
            return NotificationPayload.fromGoalCompleted((Goal) p);
        } else if ("BUDGET_UPDATED".equals(type) && p instanceof Budget) {
            return NotificationPayload.fromBudgetUpdated((Budget) p);
        } else if ("PRODUCT_PURCHASED".equals(type) && p instanceof Double) {
            return NotificationPayload.fromProductPurchased((Double) p);
        } else if ("TICKET_BOOKED".equals(type) && p instanceof CinemaBooking) {
            return NotificationPayload.fromTicketBooked((CinemaBooking) p);
        }
        return null;
    }

    /**
     * Dispatches notification asynchronously on the background daemon thread pool.
     */
    public void sendNotificationAsync(NotificationPayload payload) {
        if (dispatchQueue.isShutdown()) return;
        dispatchQueue.submit(() -> sendNotificationSync(payload));
    }

    /**
     * Executes the actual HTTP publish with exponential backoff retry.
     */
    public boolean sendNotificationSync(NotificationPayload payload) {
        long start = System.currentTimeMillis();
        String endpoint = config.getPublishEndpoint();

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Title", payload.getTitle())
                    .header("Priority", String.valueOf(payload.getPriority()))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.getMessage()));

            if (!payload.getTags().isEmpty()) {
                builder.header("Tags", payload.getTagsHeader());
            }

            if (config.getAuthToken() != null && !config.getAuthToken().trim().isEmpty()) {
                builder.header("Authorization", "Bearer " + config.getAuthToken().trim());
            }

            HttpRequest request = builder.build();

            // Execute with exponential backoff retry
            HttpResponse<String> response = HttpHelper.executeWithRetry(
                    httpClient, request, config.getMaxRetries(), config.getInitialRetryDelayMs());

            long latency = System.currentTimeMillis() - start;

            if (response != null && response.statusCode() >= 200 && response.statusCode() < 400) {
                deliveredCount.incrementAndGet();
                lastDeliveryTimestamp = System.currentTimeMillis();
                currentStatus = IntegrationStatus.HEALTHY;
                history.add(new NotificationRecord(config.getTopic(), payload.getTitle(), payload.getMessage(), "DELIVERED", 1, response.statusCode()));
                IntegrationLogger.log("ntfy", "PUBLISH " + payload.getTitle(), true, "HTTP " + response.statusCode(), latency);
                return true;
            } else {
                int code = response != null ? response.statusCode() : 0;
                failedCount.incrementAndGet();
                currentStatus = IntegrationStatus.DEGRADED;
                history.add(new NotificationRecord(config.getTopic(), payload.getTitle(), payload.getMessage(), "FAILED", config.getMaxRetries(), code));
                IntegrationLogger.log("ntfy", "PUBLISH " + payload.getTitle(), false, "HTTP " + code, latency);
                return false;
            }

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            failedCount.incrementAndGet();
            currentStatus = IntegrationStatus.UNAVAILABLE;
            history.add(new NotificationRecord(config.getTopic(), payload.getTitle(), payload.getMessage(), "FAILED", config.getMaxRetries(), -1));
            IntegrationLogger.log("ntfy", "PUBLISH " + payload.getTitle(), false, e.getMessage(), latency);
            System.err.println("[NtfyNotificationService] Non-fatal delivery failure: " + e.getMessage());
            return false;
        }
    }
}
