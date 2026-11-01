package nexus.finance;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import java.util.*;

public class FinanceService implements NexusListener {
    private Budget budget;
    private List<Transaction> transactions;
    private List<CryptoPrice> cryptoPrices;
    
    public FinanceService() {
        budget = new Budget(10000.0);
        transactions = new ArrayList<>();
        cryptoPrices = new ArrayList<>();
        
        EventBus.getInstance().subscribe("PRODUCT_PURCHASED", this);
        EventBus.getInstance().subscribe("GOAL_COMPLETED", this);
        EventBus.getInstance().subscribe("ADD_FUNDS_REQUEST", this);
        EventBus.getInstance().subscribe("GOD_MODE_BUDGET", this);
        EventBus.getInstance().subscribe("TICKET_BOOKED", this);

        // Fetch live crypto rates asynchronously
        refreshCryptoRates();
    }

    public void refreshCryptoRates() {
        new Thread(() -> {
            List<CryptoPrice> rates = RealtimeFinanceService.fetchLiveCryptoRates();
            this.cryptoPrices = rates;
            EventBus.getInstance().publish(new NexusEvent("CRYPTO_RATES_UPDATED", rates, "Finance"));
        }).start();
    }
    
    public void addTransaction(String description, double amount) {
        addTransaction(description, amount, TransactionCategory.OTHER);
    }

    public void addTransaction(String description, double amount, TransactionCategory category) {
        Transaction t = new Transaction(description, amount, category);
        transactions.add(0, t);
        budget.addExpense(amount);
        EventBus.getInstance().publish(new NexusEvent("TRANSACTION_ADDED", t, "Finance"));
        EventBus.getInstance().publish(new NexusEvent("BUDGET_UPDATED", budget, "Finance"));
    }

    public void addFunds(String description, double amount) {
        addFunds(description, amount, TransactionCategory.SALARY_INCOME);
    }

    public void addFunds(String description, double amount, TransactionCategory category) {
        Transaction t = new Transaction(description, -amount, category);
        transactions.add(0, t);
        budget.addExpense(-amount);
        EventBus.getInstance().publish(new NexusEvent("TRANSACTION_ADDED", t, "Finance"));
        EventBus.getInstance().publish(new NexusEvent("BUDGET_UPDATED", budget, "Finance"));
    }

    public Map<String, Double> getCategoryBreakdown() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Transaction t : transactions) {
            if (t.getAmount() > 0) {
                String catName = t.getCategory() != null ? t.getCategory().getDisplayName() : "Other";
                map.put(catName, map.getOrDefault(catName, 0.0) + t.getAmount());
            }
        }
        return map;
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if ("PRODUCT_PURCHASED".equals(event.getType())) {
            double price = (Double) event.getPayload();
            addTransaction("Market Purchase", price, TransactionCategory.SHOPPING);
        } else if ("TICKET_BOOKED".equals(event.getType())) {
            nexus.media.CinemaBooking b = (nexus.media.CinemaBooking) event.getPayload();
            addTransaction("Cinema Ticket: " + b.getMovieTitle() + " (" + b.getSeatsFormatted() + ")", b.getFinalTotal(), TransactionCategory.ENTERTAINMENT);
        } else if ("GOAL_COMPLETED".equals(event.getType())) {
            nexus.goals.Goal goal = (nexus.goals.Goal) event.getPayload();
            addFunds("Goal Reward: " + goal.getTitle(), 150.0, TransactionCategory.SALARY_INCOME);
        } else if ("ADD_FUNDS_REQUEST".equals(event.getType())) {
            double amount = (Double) event.getPayload();
            addFunds("Custom Bonus", amount, TransactionCategory.SALARY_INCOME);
        } else if ("GOD_MODE_BUDGET".equals(event.getType())) {
            double amount = (Double) event.getPayload();
            addFunds("God Mode Budget Cheat", amount, TransactionCategory.INVESTMENT);
        }
    }
    
    public Budget getBudget() { return budget; }
    public List<Transaction> getTransactions() { return transactions; }
    public List<CryptoPrice> getCryptoPrices() { return cryptoPrices; }
    
    public void setTransactions(List<Transaction> loadedTransactions) {
        this.transactions = loadedTransactions;
        double spent = 0;
        for (Transaction t : loadedTransactions) {
            spent += t.getAmount();
        }
        budget = new Budget(10000.0);
        budget.addExpense(spent);
        EventBus.getInstance().publish(new NexusEvent("TRANSACTIONS_UPDATED", transactions, "Finance"));
        EventBus.getInstance().publish(new NexusEvent("BUDGET_UPDATED", budget, "Finance"));
    }
}
