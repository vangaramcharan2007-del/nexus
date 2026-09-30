package nexus.goals;

import nexus.common.IdGenerator;
import java.util.ArrayList;
import java.util.List;

public class Goal {
    private String id;
    private String title;
    private List<Milestone> milestones;
    
    public Goal(String title) {
        this.id = IdGenerator.generateId();
        this.title = title;
        this.milestones = new ArrayList<>();
    }
    
    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<Milestone> getMilestones() { return milestones; }
    
    public void addMilestone(Milestone m) {
        milestones.add(m);
    }
    
    public double getProgress() {
        if(milestones.isEmpty()) return 0.0;
        long completedCount = milestones.stream().filter(Milestone::isCompleted).count();
        return (double) completedCount / milestones.size();
    }
}
