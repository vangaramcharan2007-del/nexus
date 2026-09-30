package nexus.finance;

import javax.swing.*;
import java.awt.*;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class FinancePanel extends JPanel implements NexusListener {
    private FinanceService service;
    private JLabel budgetLabel;
    private JTextArea txArea;

    public FinancePanel(FinanceService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        budgetLabel = new JLabel("Budget info...");
        budgetLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        budgetLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(budgetLabel, BorderLayout.NORTH);
        
        txArea = new JTextArea();
        txArea.setEditable(false);
        add(new JScrollPane(txArea), BorderLayout.CENTER);
        
        EventBus.getInstance().subscribe("TRANSACTION_ADDED", this);
        EventBus.getInstance().subscribe("BUDGET_UPDATED", this);
        
        updateView();
    }
    
    private void updateView() {
        Budget b = service.getBudget();
        budgetLabel.setText(String.format("Total: $%.2f | Spent: $%.2f | Remaining: $%.2f", 
                b.getTotalBudget(), b.getCurrentSpent(), b.getRemaining()));
                
        StringBuilder sb = new StringBuilder();
        for(Transaction t : service.getTransactions()) {
            sb.append(t.getDate()).append(" - ").append(t.getDescription())
              .append(": $").append(t.getAmount()).append("\n");
        }
        txArea.setText(sb.toString());
    }

    @Override
    public void onEvent(NexusEvent event) {
        SwingUtilities.invokeLater(() -> updateView());
    }
}
