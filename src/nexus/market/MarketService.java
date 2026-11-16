package nexus.market;

import nexus.common.EventBus;
import nexus.common.IdGenerator;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;
import nexus.finance.FinanceService;
import nexus.finance.PurchaseResult;
import nexus.finance.TransactionCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Campus Store Market Service for NEXUS V26.1.
 *
 * V26.1 Concurrency & Financial Integrity Hardening:
 * - Coordinates checkout atomically via FinanceService.tryPurchase() to eliminate race conditions.
 * - Prevents overdraft purchases exceeding budget cap under concurrent conditions.
 * - Rollback mechanism: Preserves cart and emits CHECKOUT_FAILED if funds are insufficient or transaction is rejected.
 * - Clears cart ONLY after confirmed financial success.
 * - Preserves existing XP discount system (1,000 XP = 5%, 5,000 XP = 15%).
 */
public class MarketService implements NexusListener {
    private List<Product> availableProducts;
    private ShoppingCart cart;
    private boolean loading;
    private FinanceService financeService;
    
    public MarketService() {
        this(null);
    }

    public MarketService(FinanceService financeService) {
        this.financeService = financeService;
        this.availableProducts = new ArrayList<>();
        this.cart = new ShoppingCart();
        this.loading = false;
        
        EventBus.getInstance().subscribe("XP_UPDATED", this);

        // Fetch initial Amazon products
        searchMarket("laptop");
    }

    public void setFinanceService(FinanceService financeService) {
        this.financeService = financeService;
    }
    
    public FinanceService getFinanceService() {
        return financeService;
    }

    public void searchMarket(String query) {
        loading = true;
        EventBus.getInstance().publish(new NexusEvent("MARKET_LOADING", true, "Market"));
        new Thread(() -> {
            List<Product> scrapedProducts = nexus.realtime.WebMarketScraper.scrapeMarket(query);
            availableProducts.clear();
            availableProducts.addAll(scrapedProducts);
            loading = false;
            EventBus.getInstance().publish(new NexusEvent("MARKET_UPDATED", null, "Market"));
        }).start();
    }
    
    public List<Product> getAvailableProducts() { return availableProducts; }
    public boolean isLoading() { return loading; }
    
    public void addToCart(Product product, int quantity) {
        cart.addItem(product, quantity);
        EventBus.getInstance().publish(new NexusEvent("CART_UPDATED", cart, "Market"));
    }
    
    /**
     * Executes checkout with atomic financial coordination and strict fail-closed security.
     * The cart is cleared ONLY after confirmed financial settlement.
     * If the finance service is unavailable or rejected, the cart remains intact and CHECKOUT_FAILED is emitted.
     */
    public synchronized void checkout() {
        double total = cart.getTotal();
        if (total <= 0.0 || cart.getItems().isEmpty()) {
            return;
        }

        // Fail-Closed Security: FinanceService must be present and approve the transaction
        if (financeService == null) {
            String errorMsg = "Checkout failed: Finance service is unavailable (fail-closed protection).";
            System.err.println("[MarketService] " + errorMsg);
            EventBus.getInstance().publish(new NexusEvent("CHECKOUT_FAILED", errorMsg, "Market"));
            return; // Cart is strictly preserved
        }

        String txId = "MKT-" + IdGenerator.generateId();
        String desc = String.format("Campus Store Order (%d items)", cart.getItems().size());
        PurchaseResult result;

        try {
            // Atomic check and debit directly in FinanceService
            result = financeService.tryPurchase(total, desc, txId, TransactionCategory.SHOPPING);
        } catch (Throwable t) {
            String errorMsg = "Checkout failed due to payment processing error: " + t.getMessage();
            System.err.println("[MarketService] " + errorMsg);
            EventBus.getInstance().publish(new NexusEvent("CHECKOUT_FAILED", errorMsg, "Market"));
            return; // Cart is strictly preserved
        }

        if (result == null || !result.isSuccess()) {
            String errorMsg = (result != null && result.getMessage() != null) ? result.getMessage() : "Payment rejected by finance service.";
            System.err.println("[MarketService] Checkout rejected: " + errorMsg);
            EventBus.getInstance().publish(new NexusEvent("CHECKOUT_FAILED", errorMsg, "Market"));
            return; // Cart is NOT cleared; transaction rolled back
        }

        // Confirmed financial success: notify subsystems, clear cart
        EventBus.getInstance().publish(new NexusEvent("PRODUCT_PURCHASED", total, "Market"));
        for (CartItem item : cart.getItems()) {
            EventBus.getInstance().publish(new NexusEvent("ITEM_PURCHASED", item.getProduct(), "Market"));
        }
        cart.clear();
        EventBus.getInstance().publish(new NexusEvent("CART_UPDATED", cart, "Market"));
    }
    
    public ShoppingCart getCart() { return cart; }

    @Override
    public void onEvent(NexusEvent event) {
        if (event == null) return;
        
        if ("XP_UPDATED".equals(event.getType()) && event.getPayload() instanceof Integer) {
            int score = (Integer) event.getPayload();
            double discount = 0.0;
            // Preserved XP Discount tiers:
            // 1,000 XP = 5% discount
            // 5,000 XP = 15% discount
            if (score >= 5000) {
                discount = 0.15;
            } else if (score >= 1000) {
                discount = 0.05;
            }
            
            cart.setDiscountRate(discount);
            EventBus.getInstance().publish(new NexusEvent("CART_UPDATED", cart, "Market"));
        }
    }
}
