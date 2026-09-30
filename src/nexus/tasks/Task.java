package nexus.tasks;

import nexus.common.IdGenerator;

public class Task {
    private String id;
    private String title;
    private TaskPriority priority;
    private boolean completed;
    
    public Task(String title, TaskPriority priority) {
        this.id = IdGenerator.generateId();
        this.title = title;
        this.priority = priority;
        this.completed = false;
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public TaskPriority getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    
    public void setCompleted(boolean completed) { this.completed = completed; }
}
