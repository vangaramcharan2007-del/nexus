package nexus.tasks;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import java.util.ArrayList;
import java.util.List;

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
}
