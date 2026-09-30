package nexus.analytics;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import java.util.ArrayList;
import java.util.List;

/**
 * The AnalyticsService acts as the 'Controller' or 'Business Logic' layer for its respective module.
 * 
 * In a professional software architecture (like MVC or MVVM), Services are responsible for:
 * 1. Managing data state (holding the lists of items, scores, or configurations).
 * 2. Processing business rules (e.g., verifying if an action is valid).
 * 3. Communicating with other parts of the application (publishing events to the EventBus).
 * 
 * By keeping this logic out of the GUI Panels, the code remains easy to test, maintain, and extend.
 */
public class AnalyticsService implements NexusListener {
    private NexusScore score;
    private List<Insight> insights;
    
    public AnalyticsService() {
        score = new NexusScore(100);
        insights = new ArrayList<>();
        
        EventBus.getInstance().subscribe("TASK_COMPLETED", this);
        EventBus.getInstance().subscribe("PRODUCT_PURCHASED", this);
        EventBus.getInstance().subscribe("MEDIA_COMPLETED", this);
    }
    
    private void generateInsight(String message) {
        insights.add(new Insight(message));
        EventBus.getInstance().publish(new NexusEvent("INSIGHT_GENERATED", insights, "Analytics"));
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if ("TASK_COMPLETED".equals(event.getType())) {
            score.addPoints(10);
            generateInsight("Good job! Task completed. +10 points.");
        } else if ("PRODUCT_PURCHASED".equals(event.getType())) {
            double amount = (Double) event.getPayload();
            if(amount > 500) {
                score.deductPoints(5);
                generateInsight("Large purchase detected. Watch your budget! -5 points.");
            }
        } else if ("MEDIA_COMPLETED".equals(event.getType())) {
            nexus.media.MediaItem media = (nexus.media.MediaItem) event.getPayload();
            score.addPoints(5);
            generateInsight("Finished consuming: " + media.getTitle() + ". +5 points.");
        }
        EventBus.getInstance().publish(new NexusEvent("SCORE_UPDATED", score.getScore(), "Analytics"));
    }
    
    public int getCurrentScore() { return score.getScore(); }
    public List<Insight> getInsights() { return insights; }
}
