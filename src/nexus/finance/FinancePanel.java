package nexus.finance;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import nexus.analytics.PieChartPanel;
import nexus.common.EventBus;
import nexus.common.NexusEvent;
import nexus.common.NexusListener;

public class FinancePanel extends JPanel implements NexusListener {
    private final FinanceService service;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm");

    // UI Components
    private JLabel totalBudgetVal;
    private JLabel totalSpentVal;
    private JLabel remainingVal;
    private JLabel incomeVal;
    private JProgressBar budgetProgressBar;
    private JLabel tickerLabel;
    private PieChartPanel pieChartPanel;
    private DefaultTableModel tableModel;
    private JTable transactionTable;

    public FinancePanel(FinanceService service) {
        this.service = service;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(new Color(20, 24, 33));

        // 1. TOP HEADER (Live Ticker + KPI Cards)
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        topContainer.add(createTickerBar());
        topContainer.add(Box.createVerticalStrut(10));
        topContainer.add(createKpiCardPanel());
        topContainer.add(Box.createVerticalStrut(10));
        topContainer.add(createProgressBarPanel());

        add(topContainer, BorderLayout.NORTH);

        // 2. CENTER CONTENT (Split: Left Form & Pie Chart | Right Table)
        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 15, 0));
        centerGrid.setOpaque(false);

        // Left: Input Form + Pie Chart
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setOpaque(false);
        leftPanel.add(createInputForm(), BorderLayout.NORTH);

        pieChartPanel = new PieChartPanel();
        pieChartPanel.setPreferredSize(new Dimension(300, 240));
        pieChartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 81)), 
                "SPENDING BY CATEGORY", 
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.CYAN));
        leftPanel.add(pieChartPanel, BorderLayout.CENTER);

        centerGrid.add(leftPanel);

        // Right: Modern Transaction Table
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 81)), 
                "TRANSACTION LEDGER (REAL-TIME AUDIT)", 
                0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(147, 197, 253)));

        String[] columns = {"Date", "Description", "Category", "Amount"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        transactionTable = new JTable(tableModel);
        transactionTable.setRowHeight(30);
        transactionTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        transactionTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.setDefaultRenderer(Object.class, new TransactionCellRenderer());

        JScrollPane tableScroll = new JScrollPane(transactionTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        rightPanel.add(tableScroll, BorderLayout.CENTER);

        centerGrid.add(rightPanel);

        add(centerGrid, BorderLayout.CENTER);

        // EventBus subscriptions
        EventBus.getInstance().subscribe("TRANSACTION_ADDED", this);
        EventBus.getInstance().subscribe("BUDGET_UPDATED", this);
        EventBus.getInstance().subscribe("TRANSACTIONS_UPDATED", this);
        EventBus.getInstance().subscribe("CRYPTO_RATES_UPDATED", this);

        // Initial view update
        updateView();
        updateTicker(service.getCryptoPrices());
    }

    private JPanel createTickerBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(new Color(30, 41, 59));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        JLabel title = new JLabel("● LIVE COINGECKO CRYPTO FEED:");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(new Color(52, 211, 153));

        tickerLabel = new JLabel("Connecting to decentralized price feed...");
        tickerLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        tickerLabel.setForeground(Color.WHITE);

        JButton refreshBtn = new JButton("🔄 Refresh Feed");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        refreshBtn.setBackground(new Color(59, 130, 246));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            tickerLabel.setText("Fetching live rates from CoinGecko...");
            service.refreshCryptoRates();
        });

        bar.add(title, BorderLayout.WEST);
        bar.add(tickerLabel, BorderLayout.CENTER);
        bar.add(refreshBtn, BorderLayout.EAST);
        return bar;
    }

    private JPanel createKpiCardPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 0));
        panel.setOpaque(false);

        totalBudgetVal = new JLabel("$0.00");
        totalSpentVal = new JLabel("$0.00");
        remainingVal = new JLabel("$0.00");
        incomeVal = new JLabel("$0.00");

        panel.add(createCard("TOTAL BUDGET", totalBudgetVal, new Color(59, 130, 246)));
        panel.add(createCard("TOTAL SPENT", totalSpentVal, new Color(239, 68, 68)));
        panel.add(createCard("REMAINING", remainingVal, new Color(16, 185, 129)));
        panel.add(createCard("TOTAL INCOME", incomeVal, new Color(168, 85, 247)));

        return panel;
    }

    private JPanel createCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(new Color(156, 163, 175));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(Color.WHITE);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createProgressBarPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setOpaque(false);

        JLabel label = new JLabel("Budget Utilization:");
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(new Color(209, 213, 219));

        budgetProgressBar = new JProgressBar(0, 100);
        budgetProgressBar.setValue(0);
        budgetProgressBar.setStringPainted(true);
        budgetProgressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        budgetProgressBar.setForeground(new Color(16, 185, 129));
        budgetProgressBar.setPreferredSize(new Dimension(200, 18));

        panel.add(label, BorderLayout.WEST);
        panel.add(budgetProgressBar, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createInputForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 41, 59));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 81)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField descField = new JTextField(12);
        JTextField amountField = new JTextField(8);
        JComboBox<TransactionCategory> catCombo = new JComboBox<>(TransactionCategory.values());
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Expense (-)", "Income (+)"});
        JButton addBtn = new JButton("Record");
        addBtn.setBackground(new Color(16, 185, 129));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addBtn.setFocusPainted(false);

        // Row 1: Description
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7;
        panel.add(descField, gbc);

        // Row 2: Amount
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        panel.add(new JLabel("Amount ($):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7;
        panel.add(amountField, gbc);

        // Row 3: Category
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.7;
        panel.add(catCombo, gbc);

        // Row 4: Type
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        panel.add(new JLabel("Flow Type:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.7;
        panel.add(typeCombo, gbc);

        // Row 5: Action Button
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 4, 2, 4);
        panel.add(addBtn, gbc);

        addBtn.addActionListener(e -> {
            String desc = descField.getText().trim();
            String amtStr = amountField.getText().trim();
            if (desc.isEmpty() || amtStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please provide description and amount.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                double amt = Double.parseDouble(amtStr);
                if (amt <= 0) {
                    JOptionPane.showMessageDialog(this, "Amount must be positive.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                TransactionCategory cat = (TransactionCategory) catCombo.getSelectedItem();
                boolean isExpense = typeCombo.getSelectedIndex() == 0;
                if (isExpense) {
                    service.addTransaction(desc, amt, cat);
                } else {
                    service.addFunds(desc, amt, cat);
                }
                descField.setText("");
                amountField.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number for amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private void updateView() {
        Budget b = service.getBudget();
        totalBudgetVal.setText(String.format("$%,.2f", b.getTotalBudget()));
        totalSpentVal.setText(String.format("$%,.2f", b.getCurrentSpent()));
        remainingVal.setText(String.format("$%,.2f", b.getRemaining()));

        // Calculate total income
        double totalInc = 0;
        for (Transaction t : service.getTransactions()) {
            if (t.isIncome()) {
                totalInc += t.getAbsoluteAmount();
            }
        }
        incomeVal.setText(String.format("$%,.2f", totalInc));

        // Update progress bar
        int percent = (int) Math.min(100, Math.max(0, (b.getCurrentSpent() / b.getTotalBudget()) * 100));
        budgetProgressBar.setValue(percent);
        budgetProgressBar.setString(percent + "% Used");
        if (percent > 90) {
            budgetProgressBar.setForeground(new Color(239, 68, 68)); // Red
        } else if (percent > 70) {
            budgetProgressBar.setForeground(new Color(245, 158, 11)); // Amber
        } else {
            budgetProgressBar.setForeground(new Color(16, 185, 129)); // Green
        }

        // Update Pie Chart
        Map<String, Double> breakdown = service.getCategoryBreakdown();
        pieChartPanel.setData(breakdown);

        // Update Table
        tableModel.setRowCount(0);
        for (Transaction t : service.getTransactions()) {
            String dateStr = dateFormat.format(t.getDate());
            String catName = t.getCategory() != null ? t.getCategory().getDisplayName() : "General";
            String amtStr = (t.isIncome() ? "+$" : "-$") + String.format("%,.2f", t.getAbsoluteAmount());
            tableModel.addRow(new Object[]{dateStr, t.getDescription(), catName, amtStr});
        }
    }

    private void updateTicker(List<CryptoPrice> prices) {
        if (prices == null || prices.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < prices.size(); i++) {
            CryptoPrice cp = prices.get(i);
            String sign = cp.getChange24h() >= 0 ? "▲ +" : "▼ ";
            sb.append(String.format("%s: $%,.2f (%s%.2f%%)", cp.getSymbol(), cp.getPriceUSD(), sign, cp.getChange24h()));
            if (i < prices.size() - 1) {
                sb.append("   |   ");
            }
        }
        tickerLabel.setText(sb.toString());
    }

    @Override
    public void onEvent(NexusEvent event) {
        SwingUtilities.invokeLater(() -> {
            if ("TRANSACTION_ADDED".equals(event.getType()) 
                    || "BUDGET_UPDATED".equals(event.getType())
                    || "TRANSACTIONS_UPDATED".equals(event.getType())) {
                updateView();
            } else if ("CRYPTO_RATES_UPDATED".equals(event.getType())) {
                @SuppressWarnings("unchecked")
                List<CryptoPrice> rates = (List<CryptoPrice>) event.getPayload();
                updateTicker(rates);
            }
        });
    }

    // Custom cell renderer for transaction amounts and category colors
    private static class TransactionCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (!isSelected) {
                if (row % 2 == 0) {
                    c.setBackground(new Color(24, 30, 42));
                } else {
                    c.setBackground(new Color(30, 38, 52));
                }
            }

            if (col == 3 && value != null) {
                String str = value.toString();
                if (str.startsWith("+")) {
                    c.setForeground(new Color(52, 211, 153)); // Emerald green for income
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(new Color(248, 113, 113)); // Soft red for expense
                }
            } else if (col == 2) {
                c.setForeground(new Color(147, 197, 253)); // Soft cyan for category
            } else {
                c.setForeground(new Color(229, 231, 235));
            }
            return c;
        }
    }
}
