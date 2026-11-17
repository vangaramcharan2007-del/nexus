package nexus.tasks;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Task Management Service for NEXUS V26.
 *
 * V26 Core Reliability Enhancements:
 * - Eliminates sample task duplication on restart.
 * - Seeds default sample tasks ONLY when database is completely empty.
 * - Uses thread-safe CopyOnWriteArrayList for concurrent task manipulation.
 * - Preserves existing user tasks across restarts.
 */
public class TaskService implements NexusListener {
    private final List<Task> tasks;
    
    public TaskService() {
        tasks = new CopyOnWriteArrayList<>();
        
        // Note: Default tasks are no longer unconditionally added here.
        // Seeding happens via seedDefaultsIfEmpty() only if the database is empty.
        
        EventBus.getInstance().subscribe("ITEM_PURCHASED", this);
        EventBus.getInstance().subscribe("MEDIA_ADDED", this);
        EventBus.getInstance().subscribe("MEDIA_COMPLETED", this);
    }

    /**
     * Seeds initial default tasks only if the task list is completely empty.
     */
    public synchronized void seedDefaultsIfEmpty() {
        if (tasks.isEmpty()) {
            addTask("Prepare Review 3 Software Demo & Viva", TaskPriority.HIGH, "Academic", "Tomorrow");
            addTask("Verify SQLite WAL Concurrency & ACID Guards", TaskPriority.HIGH, "Project", "Today");
            addTask("Audit EventBus & Merkle Tree Ledger", TaskPriority.MEDIUM, "Security", "Completed");
        }
    }
    
    public void addTask(String title, TaskPriority priority) {
        addTask(title, priority, "General", "Today");
    }

    public void addTask(String title, TaskPriority priority, String category, String dueDate) {
        Task t = new Task(title, priority, category, dueDate);
        tasks.add(0, t);
        EventBus.getInstance().publish(new NexusEvent("TASK_ADDED", t, "Tasks"));
        EventBus.getInstance().publish(new NexusEvent("TASKS_UPDATED", new ArrayList<>(tasks), "Tasks"));
    }
    
    public void completeTask(String id) {
        if (id == null) return;
        for (Task t : tasks) {
            if (t.getId().equals(id)) {
                t.setCompleted(true);
                EventBus.getInstance().publish(new NexusEvent("TASK_COMPLETED", t, "Tasks"));
                EventBus.getInstance().publish(new NexusEvent("TASKS_UPDATED", new ArrayList<>(tasks), "Tasks"));
                break;
            }
        }
    }

    public void deleteTask(String id) {
        if (id == null) return;
        Task removed = null;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(id)) {
                removed = tasks.remove(i);
                break;
            }
        }
        if (removed != null) {
            EventBus.getInstance().publish(new NexusEvent("TASK_DELETED", removed, "Tasks"));
            EventBus.getInstance().publish(new NexusEvent("TASKS_UPDATED", new ArrayList<>(tasks), "Tasks"));
        }
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if (event == null) return;
        
        if ("ITEM_PURCHASED".equals(event.getType()) && event.getPayload() instanceof nexus.market.Product) {
            nexus.market.Product product = (nexus.market.Product) event.getPayload();
            String pName = product.getName().toLowerCase();
            for (Task t : tasks) {
                if (!t.isCompleted() && t.getTitle().toLowerCase().contains(pName)) {
                    completeTask(t.getId());
                }
            }
        } else if ("MEDIA_ADDED".equals(event.getType()) && event.getPayload() instanceof nexus.media.MediaItem) {
            nexus.media.MediaItem media = (nexus.media.MediaItem) event.getPayload();
            String prefix = media.getType() == nexus.media.MediaType.BOOK ? "Read" : "Watch";
            addTask(prefix + ": " + media.getTitle(), TaskPriority.MEDIUM, "Media", "Upcoming");
        } else if ("MEDIA_COMPLETED".equals(event.getType()) && event.getPayload() instanceof nexus.media.MediaItem) {
            nexus.media.MediaItem media = (nexus.media.MediaItem) event.getPayload();
            String mediaTitle = media.getTitle().toLowerCase();
            for (Task t : tasks) {
                if (!t.isCompleted() && t.getTitle().toLowerCase().contains(mediaTitle)) {
                    completeTask(t.getId());
                    break;
                }
            }
        }
    }
    
    public List<Task> getTasks() {
        return new ArrayList<>(tasks);
    }
    
    public synchronized void setTasks(List<Task> loadedTasks) {
        if (loadedTasks != null && !loadedTasks.isEmpty()) {
            this.tasks.clear();
            this.tasks.addAll(loadedTasks);
            EventBus.getInstance().publish(new NexusEvent("TASKS_UPDATED", new ArrayList<>(tasks), "Tasks"));
        } else {
            seedDefaultsIfEmpty();
        }
    }
}
