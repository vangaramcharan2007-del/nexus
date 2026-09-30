package nexus.market;

import nexus.common.EventBus;
import nexus.common.NexusEvent;
import java.util.ArrayList;
import java.util.List;

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
