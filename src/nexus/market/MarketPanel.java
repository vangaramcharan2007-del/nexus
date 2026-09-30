package nexus.market;

import javax.swing.*;
import java.awt.*;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class MarketPanel extends JPanel implements NexusListener {
    private MarketService service;
    private JList<String> productList;
    private DefaultListModel<String> listModel;
    private JLabel cartLabel;
    
    public MarketPanel(MarketService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        listModel = new DefaultListModel<>();
        for(Product p : service.getAvailableProducts()) {
            listModel.addElement(p.getName() + " - $" + p.getPrice());
        }
        
        productList = new JList<>(listModel);
        add(new JScrollPane(productList), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        JButton buyBtn = new JButton("Add to Cart");
        JButton checkoutBtn = new JButton("Checkout");
        cartLabel = new JLabel("Cart Total: $0.00");
        cartLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        
        bottomPanel.add(buyBtn);
        bottomPanel.add(cartLabel);
        bottomPanel.add(checkoutBtn);
        add(bottomPanel, BorderLayout.SOUTH);
        
        buyBtn.addActionListener(e -> {
            int idx = productList.getSelectedIndex();
            if(idx >= 0) {
                Product p = service.getAvailableProducts().get(idx);
                service.addToCart(p, 1);
            }
        });
        
        checkoutBtn.addActionListener(e -> service.checkout());
        
        EventBus.getInstance().subscribe("CART_UPDATED", this);
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if ("CART_UPDATED".equals(event.getType())) {
            SwingUtilities.invokeLater(() -> {
                ShoppingCart cart = (ShoppingCart) event.getPayload();
                cartLabel.setText(String.format("Cart Total: $%.2f", cart.getTotal()));
            });
        }
    }
}
