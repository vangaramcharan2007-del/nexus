package nexus.finance;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import java.util.ArrayList;
import java.util.List;

public class FinanceService implements NexusListener {
    private Budget budget;
    private List<Transaction> transactions;
    
    public FinanceService() {
        budget = new Budget(10000.0); // Example initial budget
        transactions = new ArrayList<>();
        EventBus.getInstance().subscribe("PRODUCT_PURCHASED", this);
    }
    
    public void addTransaction(String description, double amount) {
        Transaction t = new Transaction(description, amount);
        transactions.add(t);
        budget.addExpense(amount);
        EventBus.getInstance().publish(new NexusEvent("TRANSACTION_ADDED", t, "Finance"));
        EventBus.getInstance().publish(new NexusEvent("BUDGET_UPDATED", budget, "Finance"));
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if ("PRODUCT_PURCHASED".equals(event.getType())) {
            double price = (Double) event.getPayload();
            addTransaction("Product Purchase", price);
        }
    }
    
    public Budget getBudget() { return budget; }
    public List<Transaction> getTransactions() { return transactions; }
}
