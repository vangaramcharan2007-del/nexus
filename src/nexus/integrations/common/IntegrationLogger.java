package nexus.integrations.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe audit logger for NEXUS V26 external integration operations.
 */
public class IntegrationLogger {
    public static class LogEntry {
        private final long timestamp;
        private final String service;
        private final String operation;
        private final boolean success;
        private final String details;
        private final long latencyMs;

        public LogEntry(String service, String operation, boolean success, String details, long latencyMs) {
            this.timestamp = System.currentTimeMillis();
            this.service = service;
            this.operation = operation;
            this.success = success;
            this.details = details;
            this.latencyMs = latencyMs;
        }

        public long getTimestamp() { return timestamp; }
        public String getService() { return service; }
        public String getOperation() { return operation; }
        public boolean isSuccess() { return success; }
        public String getDetails() { return details; }
        public long getLatencyMs() { return latencyMs; }

        @Override
        public String toString() {
            return String.format("[%tT] [%s] %s - %s (%dms)", 
                    timestamp, service, operation, success ? "SUCCESS" : "FAILED: " + details, latencyMs);
        }
    }

    private static final int MAX_ENTRIES = 200;
    private static final List<LogEntry> logs = new CopyOnWriteArrayList<>();

    public static void log(String service, String operation, boolean success, String details, long latencyMs) {
        if (logs.size() >= MAX_ENTRIES) {
            logs.remove(0);
        }
        logs.add(new LogEntry(service, operation, success, details, latencyMs));
    }

    public static List<LogEntry> getLogs() {
        return new ArrayList<>(logs);
    }

    public static void clear() {
        logs.clear();
    }
}
