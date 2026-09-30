package nexus.market;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {
    private List<CartItem> items;
    
    public ShoppingCart() {
        items = new ArrayList<>();
    }
    
    public void addItem(Product product, int quantity) {
        items.add(new CartItem(product, quantity));
    }
    
    public List<CartItem> getItems() { return items; }
    
    public void clear() { items.clear(); }
    
    public double getTotal() {
        return items.stream().mapToDouble(CartItem::getTotalPrice).sum();
    }
}
