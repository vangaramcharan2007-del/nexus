package nexus.dashboard;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class DashboardPanel extends JPanel implements NexusListener {
    private DashboardService service;
    private JLabel scoreLabel;
    private JLabel budgetLabel;
    
    public DashboardPanel(DashboardService service) {
        this.service = service;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        NeuralVisualizerPanel visualizerPanel = new NeuralVisualizerPanel();
        visualizerPanel.setPreferredSize(new Dimension(800, 400));
        add(visualizerPanel, BorderLayout.CENTER);

        JPanel statsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        
        scoreLabel = new JLabel("Score: 100", SwingConstants.CENTER);
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        statsPanel.add(scoreLabel);
        
        budgetLabel = new JLabel("Budget Remaining: $10000.00", SwingConstants.CENTER);
        budgetLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        statsPanel.add(budgetLabel);
        
        add(statsPanel, BorderLayout.SOUTH);
        
        EventBus.getInstance().subscribe("DASHBOARD_UPDATED", this);
        updateView(service.getSummaryData());
    }
    
    private void updateView(Map<String, Object> data) {
        scoreLabel.setText("Score: " + data.get("score"));
        budgetLabel.setText(String.format("Budget Remaining: $%.2f", (Double)data.get("budget_remaining")));
    }

    @Override
    public void onEvent(NexusEvent event) {
        if("DASHBOARD_UPDATED".equals(event.getType())) {
            SwingUtilities.invokeLater(() -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) event.getPayload();
                updateView(data);
            });
        }
    }
}
