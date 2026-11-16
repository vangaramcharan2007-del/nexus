package nexus.finance;

/**
 * Result model representing the outcome of a coordinated financial purchase.
 */
public class PurchaseResult {
    private final boolean success;
    private final String message;
    private final double remainingBalance;
    private final Transaction transaction;

    public PurchaseResult(boolean success, String message, double remainingBalance, Transaction transaction) {
        this.success = success;
        this.message = message != null ? message : "";
        this.remainingBalance = remainingBalance;
        this.transaction = transaction;
    }

    public static PurchaseResult success(double remainingBalance, Transaction transaction) {
        return new PurchaseResult(true, "Purchase approved and settled.", remainingBalance, transaction);
    }

    public static PurchaseResult failure(String message, double remainingBalance) {
        return new PurchaseResult(false, message, remainingBalance, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public double getRemainingBalance() {
        return remainingBalance;
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
