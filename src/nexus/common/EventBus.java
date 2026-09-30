package nexus.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventBus {
    private static final EventBus instance = new EventBus();
    private final Map<String, List<NexusListener>> listeners = new ConcurrentHashMap<>();
    private final List<NexusListener> globalListeners = new ArrayList<>();

    private EventBus() {}

    public static EventBus getInstance() {
        return instance;
    }

    public void subscribe(String eventType, NexusListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }
    
    public void subscribeToAll(NexusListener listener) {
        globalListeners.add(listener);
    }

    public void publish(NexusEvent event) {
        List<NexusListener> eventListeners = listeners.get(event.getType());
        if (eventListeners != null) {
            for (NexusListener listener : eventListeners) {
                listener.onEvent(event);
            }
        }
        for (NexusListener listener : globalListeners) {
            listener.onEvent(event);
        }
    }
}
