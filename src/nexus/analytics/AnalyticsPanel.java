package nexus.analytics;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import nexus.finance.Budget;

public class AnalyticsPanel extends JPanel implements NexusListener {
    private AnalyticsService service;
    private JLabel scoreLabel;
    private JTextArea insightsArea;
    private AnimatedBarChart chart;
    
    
    // Maintain state for chart data
    private Map<String, Double> chartData = new LinkedHashMap<>();
    
    public AnalyticsPanel(AnalyticsService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        scoreLabel = new JLabel("Nexus XP: " + service.getCurrentScore());
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(scoreLabel, BorderLayout.NORTH);
        
        chart = new AnimatedBarChart();
        
        // Initialize chart data
        chartData.put("Total XP", (double) service.getCurrentScore());
        chartData.put("Budget Limit", 10000.0); // Default or mocked if unavailable at startup
        chartData.put("Spent", 0.0);
        chart.setData(chartData);
                    
        
        insightsArea = new JTextArea();
        insightsArea.setEditable(false);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chart, new JScrollPane(insightsArea));
        splitPane.setResizeWeight(0.6); // 60% chart, 40% insights
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);
        
        EventBus.getInstance().subscribe("XP_UPDATED", this);
        EventBus.getInstance().subscribe("INSIGHT_GENERATED", this);
        EventBus.getInstance().subscribe("BUDGET_UPDATED", this);
        
        updateInsightsView(service.getInsights());
    }
    
    private void updateInsightsView(List<Insight> insights) {
        StringBuilder sb = new StringBuilder();
        for(Insight i : insights) {
            sb.append(i.getTimestamp()).append("\n").append(i.getMessage()).append("\n\n");
        }
        insightsArea.setText(sb.toString());
    }

    @Override
    public void onEvent(NexusEvent event) {
        SwingUtilities.invokeLater(() -> {
            if ("XP_UPDATED".equals(event.getType())) {
                Object payload = event.getPayload();
                scoreLabel.setText("Nexus XP: " + payload);
                if (payload instanceof Integer) {
                    chartData.put("Total XP", ((Integer) payload).doubleValue());
                    chart.setData(chartData);
                    
                } else if (payload instanceof Double) {
                    chartData.put("Total XP", (Double) payload);
                    chart.setData(chartData);
                    
                }
            } else if ("BUDGET_UPDATED".equals(event.getType())) {
                Object payload = event.getPayload();
                if (payload instanceof Budget) {
                    Budget budget = (Budget) payload;
                    chartData.put("Budget Limit", budget.getTotalBudget());
                    chartData.put("Spent", budget.getCurrentSpent());
                    chart.setData(chartData);
                    
                }
            } else if ("INSIGHT_GENERATED".equals(event.getType())) {
                @SuppressWarnings("unchecked")
                List<Insight> insights = (List<Insight>) event.getPayload();
                updateInsightsView(insights);
            }
        });
    }
}



