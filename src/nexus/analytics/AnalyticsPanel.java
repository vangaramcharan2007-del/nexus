package nexus.analytics;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class AnalyticsPanel extends JPanel implements NexusListener {
    private AnalyticsService service;
    private JLabel scoreLabel;
    private JTextArea insightsArea;
    
    public AnalyticsPanel(AnalyticsService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        scoreLabel = new JLabel("Nexus Score: " + service.getCurrentScore());
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        scoreLabel.setHorizontalAlignment(SwingConstants.CENTER);
        scoreLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        add(scoreLabel, BorderLayout.NORTH);
        
        insightsArea = new JTextArea();
        insightsArea.setEditable(false);
        add(new JScrollPane(insightsArea), BorderLayout.CENTER);
        
        EventBus.getInstance().subscribe("SCORE_UPDATED", this);
        EventBus.getInstance().subscribe("INSIGHT_GENERATED", this);
        
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
            if ("SCORE_UPDATED".equals(event.getType())) {
                scoreLabel.setText("Nexus Score: " + event.getPayload());
            } else if ("INSIGHT_GENERATED".equals(event.getType())) {
                @SuppressWarnings("unchecked")
                List<Insight> insights = (List<Insight>) event.getPayload();
                updateInsightsView(insights);
            }
        });
    }
}
