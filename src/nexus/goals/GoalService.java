package nexus.goals;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The GoalService acts as the 'Controller' or 'Business Logic' layer for its respective module.
 * 
 * In a professional software architecture (like MVC or MVVM), Services are responsible for:
 * 1. Managing data state (holding the lists of items, scores, or configurations).
 * 2. Processing business rules (e.g., verifying if an action is valid).
 * 3. Communicating with other parts of the application (publishing events to the EventBus).
 * 
 * By keeping this logic out of the GUI Panels, the code remains easy to test, maintain, and extend.
 */
public class GoalService implements NexusListener {
    private List<Goal> goals;
    
    public GoalService() {
        goals = new ArrayList<>();
        EventBus.getInstance().subscribe("ITEM_PURCHASED", this);
    }
    
    public void addGoal(String title) {
        Goal g = new Goal(title);
        goals.add(g);
        EventBus.getInstance().publish(new NexusEvent("GOALS_UPDATED", goals, "Goals"));
    }
    
    public void addMilestoneToGoal(String goalId, String milestoneDesc) {
        for(Goal g : goals) {
            if(g.getId().equals(goalId)) {
                g.addMilestone(new Milestone(milestoneDesc));
                EventBus.getInstance().publish(new NexusEvent("GOALS_UPDATED", goals, "Goals"));
                break;
            }
        }
    }
    
    public void completeMilestone(String goalId, int milestoneIndex) {
        for(Goal g : goals) {
            if(g.getId().equals(goalId)) {
                if(milestoneIndex >= 0 && milestoneIndex < g.getMilestones().size()) {
                    g.getMilestones().get(milestoneIndex).setCompleted(true);
                    EventBus.getInstance().publish(new NexusEvent("GOALS_UPDATED", goals, "Goals"));
                    
                    if (g.getProgress() >= 1.0) {
                        EventBus.getInstance().publish(new NexusEvent("GOAL_COMPLETED", g, "Goals"));
                    }
                }
                break;
            }
        }
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if ("ITEM_PURCHASED".equals(event.getType())) {
            nexus.market.Product product = (nexus.market.Product) event.getPayload();
            String pName = product.getName().toLowerCase();
            for (Goal g : goals) {
                if (g.getTitle().toLowerCase().contains(pName) && g.getProgress() < 1.0) {
                    addMilestoneToGoal(g.getId(), "Purchased " + product.getName());
                    completeMilestone(g.getId(), g.getMilestones().size() - 1);
                }
                
                for (int i = 0; i < g.getMilestones().size(); i++) {
                    Milestone m = g.getMilestones().get(i);
                    if (!m.isCompleted() && m.getDescription().toLowerCase().contains(pName)) {
                        completeMilestone(g.getId(), i);
                    }
                }
            }
        }
    }
    
    public List<Goal> getGoals() { return goals; }
}
