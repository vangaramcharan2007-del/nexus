package nexus.market;

import javax.swing.*;
import java.awt.*;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class MarketPanel extends JPanel implements NexusListener {
    private MarketService service;
    private JPanel productsContainer;
    private JLabel cartLabel;
    private JButton checkoutBtn;
    
    public MarketPanel(MarketService service) {
        this.service = service;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        productsContainer = new JPanel(new GridLayout(0, 4, 15, 15));
        
        JScrollPane scrollPane = new JScrollPane(productsContainer);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel();
        checkoutBtn = new JButton("Checkout");
        cartLabel = new JLabel("Cart Total: $0.00");
        cartLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        
        bottomPanel.add(cartLabel);
        bottomPanel.add(checkoutBtn);
        add(bottomPanel, BorderLayout.SOUTH);
        
        checkoutBtn.addActionListener(e -> service.checkout());
        
        EventBus.getInstance().subscribe("CART_UPDATED", this);
        EventBus.getInstance().subscribe("BUDGET_UPDATED", this);
        EventBus.getInstance().subscribe("MARKET_UPDATED", this);
        
        updateProducts();
    }
    
    private void updateProducts() {
        productsContainer.removeAll();
        for (Product p : service.getAvailableProducts()) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            
            if (p.getImage() != null) {
                card.add(new JLabel(new ImageIcon(p.getImage())), BorderLayout.CENTER);
            } else {
                card.add(new JLabel("No Image", SwingConstants.CENTER), BorderLayout.CENTER);
            }
            
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            JLabel titleLabel = new JLabel("<html><body style='width:100px'>" + p.getName() + "</body></html>");
            titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            infoPanel.add(titleLabel);
            
            JLabel categoryLabel = new JLabel(p.getCategory() != null ? p.getCategory() : "Uncategorized");
            categoryLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.ITALIC, 10f));
            infoPanel.add(categoryLabel);
            
            JLabel priceLabel = new JLabel(String.format("$%.2f", p.getPrice()));
            priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            priceLabel.setFont(priceLabel.getFont().deriveFont(Font.BOLD));
            infoPanel.add(priceLabel);
            
            JButton buyBtn = new JButton("Add to Cart");
            buyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            buyBtn.addActionListener(e -> service.addToCart(p, 1));
            infoPanel.add(Box.createVerticalStrut(5));
            infoPanel.add(buyBtn);
            
            card.add(infoPanel, BorderLayout.SOUTH);
            productsContainer.add(card);
        }
        productsContainer.revalidate();
        productsContainer.repaint();
    }
    
    @Override
    public void onEvent(NexusEvent event) {
        if ("CART_UPDATED".equals(event.getType())) {
            SwingUtilities.invokeLater(() -> {
                ShoppingCart cart = (ShoppingCart) event.getPayload();
                if (cart.getDiscountRate() > 0) { 
                    cartLabel.setText(String.format("Cart Total: $%.2f (%.0f%% XP Discount!)", cart.getTotal(), cart.getDiscountRate() * 100)); 
                    cartLabel.setForeground(new java.awt.Color(0, 200, 100)); 
                } else { 
                    cartLabel.setText(String.format("Cart Total: $%.2f", cart.getTotal())); 
                    cartLabel.setForeground(javax.swing.UIManager.getColor("Label.foreground")); 
                }
            });
        } else if ("MARKET_UPDATED".equals(event.getType())) {
            SwingUtilities.invokeLater(this::updateProducts);
        } else if ("BUDGET_UPDATED".equals(event.getType())) {
            SwingUtilities.invokeLater(() -> {
                nexus.finance.Budget budget = (nexus.finance.Budget) event.getPayload();
                boolean canBuy = budget.getRemaining() > 0;
                checkoutBtn.setEnabled(canBuy);
                if (!canBuy) {
                    cartLabel.setText("BUDGET DEPLETED!");
                    cartLabel.setForeground(Color.RED);
                } else {
                    cartLabel.setForeground(UIManager.getColor("Label.foreground"));
                }
            });
        }
    }
}
