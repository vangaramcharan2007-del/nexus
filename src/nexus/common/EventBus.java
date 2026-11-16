package nexus.common;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The EventBus is the central nervous system of the NEXUS application.
 * It provides a decoupled, publish-subscribe (Pub/Sub) communication mechanism 
 * across the entire application architecture.
 *
 * V26 Core Reliability Enhancements:
 * - Thread-safe subscriber collections using ConcurrentHashMap and CopyOnWriteArrayList.
 * - Strict deduplication preventing identical listeners from multiple registrations.
 * - Complete listener lifecycle management (unsubscribe and unsubscribeFromAll).
 * - Per-listener exception isolation ensuring one failing subscriber cannot halt dispatch.
 * - Diagnostic inspection and clean reset capabilities for test suites.
 */
public class EventBus {
    // Singleton instance ensures we only have one event hub application-wide.
    private static final EventBus instance = new EventBus();
    
    // Maps specific event types to a thread-safe list of listeners.
    private final Map<String, CopyOnWriteArrayList<NexusListener>> listeners = new ConcurrentHashMap<>();
    
    // Listeners that wish to receive every event published on the bus.
    private final CopyOnWriteArrayList<NexusListener> globalListeners = new CopyOnWriteArrayList<>();

    // Private constructor prevents external instantiation (Singleton pattern).
    private EventBus() {}

    /**
     * Retrieves the single global instance of the EventBus.
     * @return The singleton EventBus instance.
     */
    public static EventBus getInstance() {
        return instance;
    }

    /**
     * Subscribes a listener to a specific type of event.
     * Duplicate registrations of the same listener for the same event type are ignored.
     * 
     * @param eventType The string identifier of the event (e.g., "TASK_ADDED").
     * @param listener The component that wants to be notified.
     */
    public void subscribe(String eventType, NexusListener listener) {
        if (eventType == null || listener == null) {
            return;
        }
        CopyOnWriteArrayList<NexusListener> list = listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
        // Prevent duplicate subscriptions
        if (!list.contains(listener)) {
            list.add(listener);
        }
    }
    
    /**
     * Subscribes a listener to all events flowing through the bus.
     * Duplicate registrations are ignored.
     * 
     * @param listener The component that wants to be notified of everything.
     */
    public void subscribeToAll(NexusListener listener) {
        if (listener == null) {
            return;
        }
        if (!globalListeners.contains(listener)) {
            globalListeners.add(listener);
        }
    }

    /**
     * Unsubscribes a listener from a specific event type.
     * 
     * @param eventType The event type to unsubscribe from.
     * @param listener The listener to remove.
     * @return true if the listener was removed, false otherwise.
     */
    public boolean unsubscribe(String eventType, NexusListener listener) {
        if (eventType == null || listener == null) {
            return false;
        }
        List<NexusListener> list = listeners.get(eventType);
        if (list != null) {
            return list.remove(listener);
        }
        return false;
    }

    /**
     * Unsubscribes a listener from all specific event channels and the global channel.
     * 
     * @param listener The listener to completely detach.
     */
    public void unsubscribeFromAll(NexusListener listener) {
        if (listener == null) {
            return;
        }
        globalListeners.remove(listener);
        for (List<NexusListener> list : listeners.values()) {
            list.remove(listener);
        }
    }

    /**
     * Publishes an event to the bus, distributing it to all interested listeners.
     * Ensures strict exception isolation so a throwing listener does not affect others.
     * 
     * @param event The event payload containing type and associated data.
     */
    public void publish(NexusEvent event) {
        if (event == null) {
            return;
        }

        // Step 1: Specific event listeners
        List<NexusListener> eventListeners = listeners.get(event.getType());
        if (eventListeners != null) {
            for (NexusListener listener : eventListeners) {
                try {
                    listener.onEvent(event);
                } catch (Throwable t) {
                    System.err.println("[EventBus] Isolated exception in listener (" + 
                        listener.getClass().getSimpleName() + ") for event " + event.getType() + ": " + t.getMessage());
                }
            }
        }
        
        // Step 2: Global listeners
        for (NexusListener listener : globalListeners) {
            try {
                listener.onEvent(event);
            } catch (Throwable t) {
                System.err.println("[EventBus] Isolated exception in global listener (" + 
                    listener.getClass().getSimpleName() + ") for event " + event.getType() + ": " + t.getMessage());
            }
        }
    }

    /**
     * Returns the count of active listeners registered for a specific event type.
     */
    public int getSubscriberCount(String eventType) {
        List<NexusListener> list = listeners.get(eventType);
        return list != null ? list.size() : 0;
    }

    /**
     * Returns the count of global listeners.
     */
    public int getGlobalSubscriberCount() {
        return globalListeners.size();
    }

    /**
     * Clears all listeners. Useful for clean test harness isolation.
     */
    public void reset() {
        listeners.clear();
        globalListeners.clear();
    }
}
