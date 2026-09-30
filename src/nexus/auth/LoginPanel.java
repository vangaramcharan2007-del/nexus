package nexus.auth;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class LoginPanel extends JPanel {
    private final Consumer<Void> onLoginSuccess;

    public LoginPanel(Consumer<Void> onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
        setLayout(new GridLayout(1, 2));

        // Left Panel - Dark Gradient
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(15, 15, 25);
                Color color2 = new Color(45, 40, 70);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        leftPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 0;
        gbcLeft.insets = new Insets(10, 10, 10, 10);
        
        JLabel titleLabel = new JLabel("NEXUS");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 72));
        titleLabel.setForeground(new Color(240, 240, 255));
        leftPanel.add(titleLabel, gbcLeft);
        
        gbcLeft.gridy = 1;
        JLabel subtitleLabel = new JLabel("The Future of Intelligence");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        subtitleLabel.setForeground(new Color(180, 180, 220));
        leftPanel.add(subtitleLabel, gbcLeft);

        // Right Panel - Login Form
        JPanel rightPanel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 30, 10, 30);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        
        JLabel loginTitle = new JLabel("Welcome Back");
        loginTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        loginTitle.setHorizontalAlignment(SwingConstants.CENTER);
        rightPanel.add(loginTitle, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(40, 30, 10, 30);
        JTextField usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(280, 45));
        usernameField.putClientProperty("JComponent.roundRect", true);
        usernameField.putClientProperty("JTextField.placeholderText", "Username");
        usernameField.putClientProperty("JTextField.showClearButton", true);
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 15));
        rightPanel.add(usernameField, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(10, 30, 30, 30);
        JPasswordField passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(280, 45));
        passwordField.putClientProperty("JComponent.roundRect", true);
        passwordField.putClientProperty("JTextField.placeholderText", "Password");
        passwordField.putClientProperty("JTextField.showClearButton", true);
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 15));
        rightPanel.add(passwordField, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(10, 30, 10, 30);
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        loginButton.setForeground(Color.WHITE);
        
        Color buttonColor = new Color(50, 100, 255);
        Color hoverColor = new Color(70, 130, 255);
        
        loginButton.setBackground(buttonColor);
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setPreferredSize(new Dimension(280, 50));
        
        loginButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (loginButton.isEnabled()) {
                    loginButton.setBackground(hoverColor);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (loginButton.isEnabled()) {
                    loginButton.setBackground(buttonColor);
                }
            }
        });
        
        loginButton.addActionListener(e -> {
            loginButton.setText("Authenticating...");
            loginButton.setEnabled(false);
            loginButton.setBackground(buttonColor);
            
            Timer timer = new Timer(1000, evt -> {
                onLoginSuccess.accept(null);
                // Reset in case they log out
                loginButton.setText("Login");
                loginButton.setEnabled(true);
                usernameField.setText("");
                passwordField.setText("");
            });
            timer.setRepeats(false);
            timer.start();
        });
        
        rightPanel.add(loginButton, gbc);
        
        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.add(rightPanel);

        add(leftPanel);
        add(rightWrapper);
    }
}
