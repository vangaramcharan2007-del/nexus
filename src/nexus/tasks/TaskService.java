package nexus.tasks;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The TaskService acts as the 'Controller' or 'Business Logic' layer for its respective module.
 * 
 * In a professional software architecture (like MVC or MVVM), Services are responsible for:
 * 1. Managing data state (holding the lists of items, scores, or configurations).
 * 2. Processing business rules (e.g., verifying if an action is valid).
 * 3. Communicating with other parts of the application (publishing events to the EventBus).
 * 
 * By keeping this logic out of the GUI Panels, the code remains easy to test, maintain, and extend.
 */
public class TaskService implements NexusListener {
    private List<Task> tasks;
    
    public TaskService() {
        tasks = new ArrayList<>();
        // Seed initial task
        addTask("Setup Environment", TaskPriority.HIGH);
        EventBus.getInstance().subscribe("ITEM_PURCHASED", this);
    }
    
    public void addTask(String title, TaskPriority priority) {
        Task t = new Task(title, priority);
        tasks.add(t);
        EventBus.getInstance().publish(new NexusEvent("TASK_ADDED", t, "Tasks"));
        EventBus.getInstance().publish(new NexusEvent("TASKS_UPDATED", tasks, "Tasks"));
    }
    
    public void completeTask(String id) {
        for (Task t : tasks) {
            if (t.getId().equals(id)) {
                t.setCompleted(true);
                EventBus.getInstance().publish(new NexusEvent("TASK_COMPLETED", t, "Tasks"));
                EventBus.getInstance().publish(new NexusEvent("TASKS_UPDATED", tasks, "Tasks"));
                break;
            }
        }
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if ("ITEM_PURCHASED".equals(event.getType())) {
            nexus.market.Product product = (nexus.market.Product) event.getPayload();
            String pName = product.getName().toLowerCase();
            for (Task t : tasks) {
                if (!t.isCompleted() && t.getTitle().toLowerCase().contains(pName)) {
                    completeTask(t.getId());
                }
            }
        }
    }
    
    public List<Task> getTasks() { return tasks; }
    
    public void setTasks(List<Task> loadedTasks) {
        this.tasks = loadedTasks;
        EventBus.getInstance().publish(new NexusEvent("TASKS_UPDATED", tasks, "Tasks"));
    }
}
