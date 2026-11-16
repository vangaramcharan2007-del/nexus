package nexus.rules;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Native Event-Driven Automation Engine for NEXUS V26.
 *
 * Strict Architectural Compliance:
 * - n8n is strictly EXCLUDED. All automation is 100% native.
 * - Built natively on top of the central EventBus.
 * - Supports typed event payloads, configurable conditions, execution history,
 *   and comprehensive failure tracking.
 */
public class RuleEngineService implements NexusListener {
    
    /**
     * Audit record capturing the execution of an automated rule.
     */
    public static class ExecutionRecord {
        private final String executionId;
        private final String ruleId;
        private final String ruleName;
        private final String triggerEventType;
        private final long timestamp;
        private final boolean success;
        private final long durationMs;
        private final String errorMessage;

        public ExecutionRecord(String ruleId, String ruleName, String triggerEventType, boolean success, long durationMs, String errorMessage) {
            this.executionId = nexus.common.IdGenerator.generateId();
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.triggerEventType = triggerEventType;
            this.timestamp = System.currentTimeMillis();
            this.success = success;
            this.durationMs = durationMs;
            this.errorMessage = errorMessage;
        }

        public String getExecutionId() { return executionId; }
        public String getRuleId() { return ruleId; }
        public String getRuleName() { return ruleName; }
        public String getTriggerEventType() { return triggerEventType; }
        public long getTimestamp() { return timestamp; }
        public boolean isSuccess() { return success; }
        public long getDurationMs() { return durationMs; }
        public String getErrorMessage() { return errorMessage; }

        @Override
        public String toString() {
            return String.format("[%tT] Rule '%s' on %s: %s (%dms)", 
                timestamp, ruleName, triggerEventType, success ? "SUCCESS" : "FAILED: " + errorMessage, durationMs);
        }
    }

    /**
     * Represents an automated IF-THEN rule with a trigger, condition predicate, and action.
     */
    public static class Rule {
        private final String id;
        private final String name;
        private final String triggerEventType;
        private final Predicate<NexusEvent> condition;
        private final Consumer<NexusEvent> action;
        private volatile boolean enabled;

        public Rule(String triggerEventType, Consumer<NexusEvent> action) {
            this(nexus.common.IdGenerator.generateId(), "AutoRule-" + triggerEventType, triggerEventType, e -> true, action);
        }

        public Rule(String id, String name, String triggerEventType, Predicate<NexusEvent> condition, Consumer<NexusEvent> action) {
            this.id = id != null ? id : nexus.common.IdGenerator.generateId();
            this.name = name != null ? name : "Rule-" + triggerEventType;
            this.triggerEventType = triggerEventType;
            this.condition = condition != null ? condition : e -> true;
            this.action = action;
            this.enabled = true;
        }

        public boolean matches(NexusEvent event) {
            if (!enabled || event == null) return false;
            if (!triggerEventType.equals(event.getType())) return false;
            try {
                return condition.test(event);
            } catch (Throwable t) {
                System.err.println("[Rule] Condition evaluation error in '" + name + "': " + t.getMessage());
                return false;
            }
        }

        public void execute(NexusEvent event) {
            if (action != null) {
                action.accept(event);
            }
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getTriggerEventType() { return triggerEventType; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    private final List<Rule> rules;
    private final List<ExecutionRecord> executionHistory;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final Map<String, AtomicInteger> ruleFailures = new ConcurrentHashMap<>();

    public RuleEngineService() {
        this.rules = new CopyOnWriteArrayList<>();
        this.executionHistory = new CopyOnWriteArrayList<>();
        EventBus.getInstance().subscribeToAll(this);
    }

    public void addRule(String triggerType, Consumer<NexusEvent> action) {
        rules.add(new Rule(triggerType, action));
    }

    public void addRule(String id, String name, String triggerType, Predicate<NexusEvent> condition, Consumer<NexusEvent> action) {
        rules.add(new Rule(id, name, triggerType, condition, action));
    }

    public List<Rule> getRules() {
        return new ArrayList<>(rules);
    }

    public List<ExecutionRecord> getExecutionHistory() {
        return new ArrayList<>(executionHistory);
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public int getRuleFailureCount(String ruleId) {
        AtomicInteger count = ruleFailures.get(ruleId);
        return count != null ? count.get() : 0;
    }

    public void clearRules() {
        rules.clear();
    }

    @Override
    public void onEvent(NexusEvent event) {
        if (event == null) return;

        for (Rule r : rules) {
            if (r.matches(event)) {
                long start = System.currentTimeMillis();
                try {
                    r.execute(event);
                    long duration = System.currentTimeMillis() - start;
                    successCount.incrementAndGet();
                    recordExecution(new ExecutionRecord(r.getId(), r.getName(), event.getType(), true, duration, null));
                } catch (Throwable t) {
                    long duration = System.currentTimeMillis() - start;
                    failureCount.incrementAndGet();
                    ruleFailures.computeIfAbsent(r.getId(), k -> new AtomicInteger(0)).incrementAndGet();
                    recordExecution(new ExecutionRecord(r.getId(), r.getName(), event.getType(), false, duration, t.getMessage()));
                    System.err.println("[RuleEngineService] Error executing rule '" + r.getName() + "': " + t.getMessage());
                }
            }
        }
    }

    private void recordExecution(ExecutionRecord record) {
        if (executionHistory.size() >= 500) {
            executionHistory.remove(0);
        }
        executionHistory.add(record);
        EventBus.getInstance().publish(new NexusEvent("RULE_EXECUTED", record, "RuleEngine"));
    }
}
