package nexus.finance;

public class Budget {
    private double totalBudget;
    private double currentSpent;

    public Budget(double totalBudget) {
        this.totalBudget = totalBudget;
        this.currentSpent = 0.0;
    }

    public void addExpense(double amount) {
        this.currentSpent += amount;
    }

    public double getRemaining() {
        return totalBudget - currentSpent;
    }
    
    public double getTotalBudget() { return totalBudget; }
    public double getCurrentSpent() { return currentSpent; }
}
