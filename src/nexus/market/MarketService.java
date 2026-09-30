package nexus.market;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The MarketService acts as the 'Controller' or 'Business Logic' layer for its respective module.
 * 
 * In a professional software architecture (like MVC or MVVM), Services are responsible for:
 * 1. Managing data state (holding the lists of items, scores, or configurations).
 * 2. Processing business rules (e.g., verifying if an action is valid).
 * 3. Communicating with other parts of the application (publishing events to the EventBus).
 * 
 * By keeping this logic out of the GUI Panels, the code remains easy to test, maintain, and extend.
 */
public class MarketService {
    private List<Product> availableProducts;
    private ShoppingCart cart;
    
    public MarketService() {
        availableProducts = new ArrayList<>();
        cart = new ShoppingCart();
        
        // Seed some products
        availableProducts.add(new Product("Laptop", 1200.00));
        availableProducts.add(new Product("Mouse", 25.50));
        availableProducts.add(new Product("Keyboard", 75.00));
    }
    
    public List<Product> getAvailableProducts() { return availableProducts; }
    
    public void addToCart(Product product, int quantity) {
        cart.addItem(product, quantity);
        EventBus.getInstance().publish(new NexusEvent("CART_UPDATED", cart, "Market"));
    }
    
    public void checkout() {
        double total = cart.getTotal();
        if (total > 0) {
            EventBus.getInstance().publish(new NexusEvent("PRODUCT_PURCHASED", total, "Market"));
            for (CartItem item : cart.getItems()) {
                EventBus.getInstance().publish(new NexusEvent("ITEM_PURCHASED", item.getProduct(), "Market"));
            }
            cart.clear();
            EventBus.getInstance().publish(new NexusEvent("CART_UPDATED", cart, "Market"));
        }
    }
    
    public ShoppingCart getCart() { return cart; }
}
