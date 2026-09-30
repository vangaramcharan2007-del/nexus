package nexus.dashboard;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import nexus.finance.Budget;
import java.util.HashMap;
import java.util.Map;

public class DashboardService implements NexusListener {
    private Map<String, Object> summaryData;
    
    public DashboardService() {
        summaryData = new HashMap<>();
        summaryData.put("score", 100);
        summaryData.put("budget_remaining", 10000.0);
        
        EventBus.getInstance().subscribe("SCORE_UPDATED", this);
        EventBus.getInstance().subscribe("BUDGET_UPDATED", this);
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if ("SCORE_UPDATED".equals(event.getType())) {
            summaryData.put("score", event.getPayload());
            EventBus.getInstance().publish(new NexusEvent("DASHBOARD_UPDATED", summaryData, "Dashboard"));
        } else if ("BUDGET_UPDATED".equals(event.getType())) {
            Budget b = (Budget) event.getPayload();
            summaryData.put("budget_remaining", b.getRemaining());
            EventBus.getInstance().publish(new NexusEvent("DASHBOARD_UPDATED", summaryData, "Dashboard"));
        }
    }
    
    public Map<String, Object> getSummaryData() { return summaryData; }
}
