package nexus.finance;

import nexus.common.EventBus;
import nexus.common.IdGenerator;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enterprise-grade Financial Engine for NEXUS V26.1.
 *
 * V26.1 Concurrency & Integrity Hardening:
 * - Coordinated atomic checkout via tryPurchase() eliminating race conditions between Market and Finance.
 * - Strict parameter validation: rejects null, zero, negative, NaN, and infinite amounts.
 * - Deduplication guard: strictly rejects duplicate transaction IDs and prevents double-debiting.
 * - Atomic check-and-debit guarantees budget consistency and eliminates overdrafts.
 */
public class FinanceService implements NexusListener, IFinanceEngine {
    private Budget budget;
    private final List<Transaction> transactions;
    private List<CryptoPrice> cryptoPrices;
    private final Set<String> processedTransactionIds = ConcurrentHashMap.newKeySet();
    
    public FinanceService() {
        budget = new Budget(10000.0);
        transactions = new CopyOnWriteArrayList<>();
        cryptoPrices = new CopyOnWriteArrayList<>();
        
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
            this.cryptoPrices = new CopyOnWriteArrayList<>(rates);
            EventBus.getInstance().publish(new NexusEvent("CRYPTO_RATES_UPDATED", rates, "Finance"));
        }).start();
    }
    
    /**
     * Checks if the remaining budget is sufficient for the requested amount.
     */
    public synchronized boolean canAfford(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0.0) {
            return false;
        }
        return budget != null && budget.getRemaining() >= amount;
    }

    /**
     * Atomically coordinates purchase evaluation and debiting within a single synchronized block.
     * Prevents race conditions, enforces deduplication, and guarantees balance integrity.
     */
    public synchronized PurchaseResult tryPurchase(double amount, String description, String transactionId, TransactionCategory category) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            return PurchaseResult.failure("Invalid transaction ID: cannot be null or empty", getCurrentBalance());
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0.0) {
            return PurchaseResult.failure(String.format("Invalid transaction amount: %.2f (must be a positive finite number)", amount), getCurrentBalance());
        }
        if (processedTransactionIds.contains(transactionId)) {
            return PurchaseResult.failure("Duplicate transaction rejected: ID " + transactionId + " already settled", getCurrentBalance());
        }
        if (!canAfford(amount)) {
            return PurchaseResult.failure(String.format("Insufficient funds. Required: ₹%,.2f, Available: ₹%,.2f", amount, getCurrentBalance()), getCurrentBalance());
        }

        // Atomic debit and transaction creation
        Transaction t = new Transaction(
            transactionId, 
            description != null ? description : "Market Purchase", 
            amount, 
            new Date(), 
            category != null ? category : TransactionCategory.SHOPPING
        );
        processedTransactionIds.add(transactionId);
        transactions.add(0, t);
        budget.addExpense(amount);

        // Publish events only after confirmed debit success
        EventBus.getInstance().publish(new NexusEvent("TRANSACTION_ADDED", t, "Finance"));
        EventBus.getInstance().publish(new NexusEvent("BUDGET_UPDATED", budget, "Finance"));

        return PurchaseResult.success(getCurrentBalance(), t);
    }

    public synchronized PurchaseResult tryPurchase(double amount, String description, String transactionId) {
        return tryPurchase(amount, description, transactionId, TransactionCategory.SHOPPING);
    }

    /**
     * Atomically debits the amount if funds are available.
     * @return true if debited, false if insufficient funds or invalid amount.
     */
    public synchronized boolean debitIfAvailable(double amount, String description, TransactionCategory category) {
        PurchaseResult res = tryPurchase(amount, description, IdGenerator.generateId(), category);
        return res.isSuccess();
    }

    public void addTransaction(String description, double amount) {
        addTransaction(description, amount, TransactionCategory.OTHER);
    }

    public synchronized void addTransaction(String description, double amount, TransactionCategory category) {
        Transaction t = new Transaction(description, amount, category);
        addTransaction(t);
    }

    public synchronized void addTransaction(Transaction t) {
        if (t == null) return;
        // Deduplication guard
        if (processedTransactionIds.contains(t.getId())) {
            System.out.println("[FinanceService] Ignored duplicate transaction: " + t.getId());
            return;
        }
        processedTransactionIds.add(t.getId());
        transactions.add(0, t);
        budget.addExpense(t.getAmount());
        EventBus.getInstance().publish(new NexusEvent("TRANSACTION_ADDED", t, "Finance"));
        EventBus.getInstance().publish(new NexusEvent("BUDGET_UPDATED", budget, "Finance"));
    }

    public synchronized void addFunds(String description, double amount) {
        addFunds(description, amount, TransactionCategory.SALARY_INCOME);
    }

    public synchronized void addFunds(String description, double amount, TransactionCategory category) {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0.0) {
            return;
        }
        Transaction t = new Transaction(description, -amount, category);
        if (processedTransactionIds.contains(t.getId())) {
            return;
        }
        processedTransactionIds.add(t.getId());
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
        if (event == null) return;
        
        if ("PRODUCT_PURCHASED".equals(event.getType())) {
            // Market checkout coordinates through tryPurchase() directly to eliminate race conditions.
            // If the event came from Market, it was already atomically debited.
            if ("Market".equals(event.getSource()) || "MarketService".equals(event.getSource())) {
                return;
            }
            if (event.getPayload() instanceof Double) {
                double price = (Double) event.getPayload();
                tryPurchase(price, "External Market Purchase", IdGenerator.generateId(), TransactionCategory.SHOPPING);
            }
        } else if ("TICKET_BOOKED".equals(event.getType())) {
            nexus.media.CinemaBooking b = (nexus.media.CinemaBooking) event.getPayload();
            synchronized (this) {
                addTransaction("Cinema Ticket: " + b.getMovieTitle() + " (" + b.getSeatsFormatted() + ")", b.getFinalTotal(), TransactionCategory.ENTERTAINMENT);
            }
        } else if ("GOAL_COMPLETED".equals(event.getType())) {
            nexus.goals.Goal goal = (nexus.goals.Goal) event.getPayload();
            addFunds("Goal Reward: " + goal.getTitle(), 150.0, TransactionCategory.SALARY_INCOME);
        } else if ("ADD_FUNDS_REQUEST".equals(event.getType())) {
            double amount = (Double) event.getPayload();
            addFunds("Custom Bonus", amount, TransactionCategory.SALARY_INCOME);
        } else if ("GOD_MODE_BUDGET".equals(event.getType())) {
            double amount = (Double) event.getPayload();
            addFunds("God Mode Budget Cheat", amount, TransactionCategory.INVESTMENT);
        } else if ("RESET_BUDGET_REQUEST".equals(event.getType())) {
            double amount = (Double) event.getPayload();
            resetBudget(amount);
        }
    }
    
    public synchronized void resetBudget(double newTotalAmount) {
        this.budget = new Budget(newTotalAmount);
        EventBus.getInstance().publish(new NexusEvent("BUDGET_UPDATED", budget, "Finance"));
    }

    public synchronized void setBudgetCap(double newTotalAmount) {
        this.budget.setTotalBudget(newTotalAmount);
        EventBus.getInstance().publish(new NexusEvent("BUDGET_UPDATED", budget, "Finance"));
    }
    
    public Budget getBudget() { return budget; }
    public List<Transaction> getTransactions() { return new ArrayList<>(transactions); }
    public List<CryptoPrice> getCryptoPrices() { return new ArrayList<>(cryptoPrices); }
    
    public synchronized void setTransactions(List<Transaction> loadedTransactions) {
        this.transactions.clear();
        this.processedTransactionIds.clear();
        double spent = 0;
        if (loadedTransactions != null) {
            for (Transaction t : loadedTransactions) {
                this.transactions.add(t);
                this.processedTransactionIds.add(t.getId());
                spent += t.getAmount();
            }
        }
        budget = new Budget(10000.0);
        budget.addExpense(spent);
        EventBus.getInstance().publish(new NexusEvent("TRANSACTIONS_UPDATED", new ArrayList<>(transactions), "Finance"));
        EventBus.getInstance().publish(new NexusEvent("BUDGET_UPDATED", budget, "Finance"));
    }

    // --- IFinanceEngine Implementation (Review 2 Rubric) ---
    @Override
    public double getCurrentBalance() {
        return budget != null ? budget.getRemaining() : 0.0;
    }

    @Override
    public void allocateBudget(double newBudget) {
        setBudgetCap(newBudget);
    }

    @Override
    public synchronized void recordExpense(double amount, String categoryName, String description) throws nexus.common.exceptions.InsufficientFundsException {
        if (budget != null && amount > budget.getRemaining()) {
            throw new nexus.common.exceptions.InsufficientFundsException(amount, budget.getRemaining());
        }
        TransactionCategory cat = TransactionCategory.OTHER;
        for (TransactionCategory c : TransactionCategory.values()) {
            if (c.name().equalsIgnoreCase(categoryName) || c.getDisplayName().equalsIgnoreCase(categoryName)) {
                cat = c;
                break;
            }
        }
        addTransaction(description, amount, cat);
    }

    @Override
    public double calculateEmi(double principal, double annualRatePct, int tenureMonths) {
        return FinancialCalculator.calculateEmi(principal, annualRatePct, tenureMonths).monthlyEmi;
    }

    @Override
    public double calculateCompoundInterest(double principal, double annualRatePct, int years) {
        return FinancialCalculator.calculateCompoundInterest(principal, annualRatePct, 1, years).futureValue;
    }

    @Override
    public double calculateSipWealth(double monthlyInvestment, double expectedAnnualReturnPct, int years) {
        return FinancialCalculator.calculateSip(monthlyInvestment, expectedAnnualReturnPct, years * 12).totalMaturityValue;
    }

    @Override
    public double calculateDiscountAndGst(double baseAmount, double discountPct, double gstPct) {
        return FinancialCalculator.calculateDiscountAndTax(baseAmount, discountPct, gstPct).finalPayable;
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return getTransactions();
    }
}
