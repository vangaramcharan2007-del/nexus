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

    // UI Components for Tab 1
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
        setLayout(new BorderLayout());
        setBackground(new Color(20, 24, 33));

        JTabbedPane financeTabs = new JTabbedPane();
        financeTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Tab 1: Ledger & Live Dashboard
        financeTabs.addTab("📊 Budget & Live Ledger", createBudgetLedgerPanel());

        // Tab 2: Advanced Financial Calculators & Formulas
        financeTabs.addTab("🧮 Financial Calculators & Planning Suite", createCalculatorsPanel());

        add(financeTabs, BorderLayout.CENTER);

        // EventBus subscriptions
        EventBus.getInstance().subscribe("TRANSACTION_ADDED", this);
        EventBus.getInstance().subscribe("BUDGET_UPDATED", this);
        EventBus.getInstance().subscribe("TRANSACTIONS_UPDATED", this);
        EventBus.getInstance().subscribe("CRYPTO_RATES_UPDATED", this);

        updateView();
        updateTicker(service.getCryptoPrices());
    }

    // --- TAB 1: BUDGET & LEDGER ---
    private JPanel createBudgetLedgerPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.setBackground(new Color(20, 24, 33));

        // Top Ticker + KPI Cards + Progress
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        topContainer.add(createTickerBar());
        topContainer.add(Box.createVerticalStrut(8));
        topContainer.add(createKpiCardPanel());
        topContainer.add(Box.createVerticalStrut(8));
        topContainer.add(createProgressBarPanel());

        panel.add(topContainer, BorderLayout.NORTH);

        // Center Split (Form + Pie Chart on Left | Table on Right)
        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        centerGrid.setOpaque(false);

        // Left
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setOpaque(false);
        leftPanel.add(createInputForm(), BorderLayout.NORTH);

        pieChartPanel = new PieChartPanel();
        pieChartPanel.setPreferredSize(new Dimension(300, 230));
        pieChartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 81)), 
                "SPENDING BY CATEGORY", 
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.CYAN));
        leftPanel.add(pieChartPanel, BorderLayout.CENTER);

        centerGrid.add(leftPanel);

        // Right
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setOpaque(false);
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(55, 65, 81)), 
                "REAL-TIME FINANCIAL LEDGER & AUDIT TRAIL", 
                0, 0, new Font("Segoe UI", Font.BOLD, 12), new Color(147, 197, 253)));

        String[] columns = {"Date", "Description", "Category", "Amount"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        transactionTable = new JTable(tableModel);
        transactionTable.setRowHeight(28);
        transactionTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        transactionTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        transactionTable.setDefaultRenderer(Object.class, new TransactionCellRenderer());

        JScrollPane tableScroll = new JScrollPane(transactionTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        rightPanel.add(tableScroll, BorderLayout.CENTER);

        centerGrid.add(rightPanel);

        panel.add(centerGrid, BorderLayout.CENTER);
        return panel;
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

        JButton refreshBtn = new JButton("🔄 Sync Rates");
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
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 0));
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
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLabel.setForeground(new Color(156, 163, 175));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
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
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField descField = new JTextField(12);
        JTextField amountField = new JTextField(8);
        JComboBox<TransactionCategory> catCombo = new JComboBox<>(TransactionCategory.values());
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Expense (-)", "Income (+)"});
        JButton addBtn = new JButton("+ Record Entry");
        addBtn.setBackground(new Color(16, 185, 129));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        addBtn.setFocusPainted(false);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        panel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7;
        panel.add(descField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Amount ($):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(amountField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(catCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Flow:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(typeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.insets = new Insets(6, 4, 2, 4);
        panel.add(addBtn, gbc);

        addBtn.addActionListener(e -> {
            String desc = descField.getText().trim();
            String amtStr = amountField.getText().trim();
            if (desc.isEmpty() || amtStr.isEmpty()) return;
            try {
                double amt = Double.parseDouble(amtStr);
                if (amt <= 0) return;
                TransactionCategory cat = (TransactionCategory) catCombo.getSelectedItem();
                boolean isExpense = typeCombo.getSelectedIndex() == 0;
                if (isExpense) {
                    service.addTransaction(desc, amt, cat);
                } else {
                    service.addFunds(desc, amt, cat);
                }
                descField.setText("");
                amountField.setText("");
            } catch (NumberFormatException ignored) {}
        });

        return panel;
    }

    // --- TAB 2: ADVANCED FINANCIAL CALCULATORS ---
    private JPanel createCalculatorsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 12, 12));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        panel.add(createEmiCalculatorCard());
        panel.add(createCompoundInterestCard());
        panel.add(createSipCalculatorCard());
        panel.add(createDiscountTaxCard());

        return panel;
    }

    // Calculator 1: Loan EMI
    private JPanel createEmiCalculatorCard() {
        JPanel card = createCalcCardTemplate("🏛️ LOAN EMI CALCULATOR (Formula: E = P·r·(1+r)^n / ((1+r)^n - 1))");

        JTextField principalField = new JTextField("50000", 8);
        JTextField rateField = new JTextField("8.5", 5);
        JTextField tenureField = new JTextField("36", 5);

        JLabel emiResultLabel = new JLabel("Monthly EMI: $0.00");
        emiResultLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        emiResultLabel.setForeground(new Color(52, 211, 153));

        JLabel totalResultLabel = new JLabel("Total Interest: $0.00  |  Total Payable: $0.00");
        totalResultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        totalResultLabel.setForeground(new Color(203, 213, 225));

        JButton calcBtn = new JButton("Calculate EMI");
        calcBtn.setBackground(new Color(59, 130, 246));
        calcBtn.setForeground(Color.WHITE);
        calcBtn.addActionListener(e -> {
            try {
                double p = Double.parseDouble(principalField.getText().trim());
                double r = Double.parseDouble(rateField.getText().trim());
                int n = Integer.parseInt(tenureField.getText().trim());
                FinancialCalculator.EmiResult res = FinancialCalculator.calculateEmi(p, r, n);
                emiResultLabel.setText(String.format("Monthly EMI: $%,.2f", res.monthlyEmi));
                totalResultLabel.setText(String.format("Interest: $%,.2f  |  Total: $%,.2f", res.totalInterest, res.totalAmount));
            } catch (Exception ex) {
                emiResultLabel.setText("Invalid Inputs");
            }
        });

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        inputs.setOpaque(false);
        inputs.add(new JLabel("Principal ($):"));
        inputs.add(principalField);
        inputs.add(new JLabel("Rate (% p.a):"));
        inputs.add(rateField);
        inputs.add(new JLabel("Tenure (Months):"));
        inputs.add(tenureField);
        inputs.add(calcBtn);

        card.add(inputs, BorderLayout.CENTER);

        JPanel res = new JPanel(new GridLayout(2, 1, 2, 2));
        res.setOpaque(false);
        res.add(emiResultLabel);
        res.add(totalResultLabel);
        card.add(res, BorderLayout.SOUTH);

        return card;
    }

    // Calculator 2: Compound Interest
    private JPanel createCompoundInterestCard() {
        JPanel card = createCalcCardTemplate("📈 COMPOUND WEALTH GROWTH (Formula: A = P(1 + r/n)^(nt))");

        JTextField principalField = new JTextField("10000", 8);
        JTextField rateField = new JTextField("12.0", 5);
        JTextField yearsField = new JTextField("5", 4);
        JComboBox<String> freqCombo = new JComboBox<>(new String[]{"Annually (1)", "Quarterly (4)", "Monthly (12)"});

        JLabel maturityLabel = new JLabel("Future Maturity: $0.00");
        maturityLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        maturityLabel.setForeground(new Color(52, 211, 153));

        JLabel detailsLabel = new JLabel("Interest Earned: $0.00  |  Growth Multiplier: 1.0x");
        detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailsLabel.setForeground(new Color(203, 213, 225));

        JButton calcBtn = new JButton("Calculate Growth");
        calcBtn.setBackground(new Color(16, 185, 129));
        calcBtn.setForeground(Color.WHITE);
        calcBtn.addActionListener(e -> {
            try {
                double p = Double.parseDouble(principalField.getText().trim());
                double r = Double.parseDouble(rateField.getText().trim());
                double t = Double.parseDouble(yearsField.getText().trim());
                int n = freqCombo.getSelectedIndex() == 0 ? 1 : (freqCombo.getSelectedIndex() == 1 ? 4 : 12);
                FinancialCalculator.CompoundInterestResult res = FinancialCalculator.calculateCompoundInterest(p, r, n, t);
                maturityLabel.setText(String.format("Future Maturity: $%,.2f", res.futureValue));
                detailsLabel.setText(String.format("Interest: $%,.2f  |  Growth: %.2fx", res.interestEarned, res.growthMultiplier));
            } catch (Exception ex) {
                maturityLabel.setText("Invalid Inputs");
            }
        });

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        inputs.setOpaque(false);
        inputs.add(new JLabel("Principal:"));
        inputs.add(principalField);
        inputs.add(new JLabel("Rate (%):"));
        inputs.add(rateField);
        inputs.add(new JLabel("Years:"));
        inputs.add(yearsField);
        inputs.add(freqCombo);
        inputs.add(calcBtn);

        card.add(inputs, BorderLayout.CENTER);

        JPanel res = new JPanel(new GridLayout(2, 1, 2, 2));
        res.setOpaque(false);
        res.add(maturityLabel);
        res.add(detailsLabel);
        card.add(res, BorderLayout.SOUTH);

        return card;
    }

    // Calculator 3: SIP
    private JPanel createSipCalculatorCard() {
        JPanel card = createCalcCardTemplate("🎯 SIP / WEALTH BUILDER (Formula: M = P·[(1+i)^n - 1]/i·(1+i))");

        JTextField monthlyField = new JTextField("250", 6);
        JTextField returnRateField = new JTextField("14.0", 4);
        JTextField durationField = new JTextField("60", 4);

        JLabel maturityLabel = new JLabel("Expected Corpus: $0.00");
        maturityLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        maturityLabel.setForeground(new Color(52, 211, 153));

        JLabel detailsLabel = new JLabel("Invested: $0.00  |  Wealth Gain: $0.00");
        detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailsLabel.setForeground(new Color(203, 213, 225));

        JButton calcBtn = new JButton("Project SIP");
        calcBtn.setBackground(new Color(168, 85, 247));
        calcBtn.setForeground(Color.WHITE);
        calcBtn.addActionListener(e -> {
            try {
                double p = Double.parseDouble(monthlyField.getText().trim());
                double r = Double.parseDouble(returnRateField.getText().trim());
                int n = Integer.parseInt(durationField.getText().trim());
                FinancialCalculator.SipResult res = FinancialCalculator.calculateSip(p, r, n);
                maturityLabel.setText(String.format("Expected Corpus: $%,.2f", res.totalMaturityValue));
                detailsLabel.setText(String.format("Invested: $%,.2f  |  Gain: $%,.2f", res.totalInvested, res.estimatedReturns));
            } catch (Exception ex) {
                maturityLabel.setText("Invalid Inputs");
            }
        });

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        inputs.setOpaque(false);
        inputs.add(new JLabel("Monthly ($):"));
        inputs.add(monthlyField);
        inputs.add(new JLabel("Return (%):"));
        inputs.add(returnRateField);
        inputs.add(new JLabel("Months:"));
        inputs.add(durationField);
        inputs.add(calcBtn);

        card.add(inputs, BorderLayout.CENTER);

        JPanel res = new JPanel(new GridLayout(2, 1, 2, 2));
        res.setOpaque(false);
        res.add(maturityLabel);
        res.add(detailsLabel);
        card.add(res, BorderLayout.SOUTH);

        return card;
    }

    // Calculator 4: Discount & GST
    private JPanel createDiscountTaxCard() {
        JPanel card = createCalcCardTemplate("🏷️ DISCOUNT & GST/TAX ENGINE (Net = (Price - Disc) + GST)");

        JTextField priceField = new JTextField("1200", 6);
        JTextField discountField = new JTextField("15", 4);
        JTextField gstField = new JTextField("18", 4);

        JLabel payableLabel = new JLabel("Final Payable: $0.00");
        payableLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        payableLabel.setForeground(new Color(52, 211, 153));

        JLabel detailsLabel = new JLabel("Discount Savings: $0.00  |  Tax/GST: $0.00");
        detailsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailsLabel.setForeground(new Color(203, 213, 225));

        JButton calcBtn = new JButton("Calculate Net");
        calcBtn.setBackground(new Color(245, 158, 11));
        calcBtn.setForeground(Color.WHITE);
        calcBtn.addActionListener(e -> {
            try {
                double p = Double.parseDouble(priceField.getText().trim());
                double d = Double.parseDouble(discountField.getText().trim());
                double g = Double.parseDouble(gstField.getText().trim());
                FinancialCalculator.DiscountTaxResult res = FinancialCalculator.calculateDiscountAndTax(p, d, g);
                payableLabel.setText(String.format("Final Payable: $%,.2f", res.finalPayable));
                detailsLabel.setText(String.format("Savings: -$%,.2f  |  GST: +$%,.2f", res.discountSavings, res.taxAmount));
            } catch (Exception ex) {
                payableLabel.setText("Invalid Inputs");
            }
        });

        JPanel inputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        inputs.setOpaque(false);
        inputs.add(new JLabel("Price ($):"));
        inputs.add(priceField);
        inputs.add(new JLabel("Disc (%):"));
        inputs.add(discountField);
        inputs.add(new JLabel("GST (%):"));
        inputs.add(gstField);
        inputs.add(calcBtn);

        card.add(inputs, BorderLayout.CENTER);

        JPanel res = new JPanel(new GridLayout(2, 1, 2, 2));
        res.setOpaque(false);
        res.add(payableLabel);
        res.add(detailsLabel);
        card.add(res, BorderLayout.SOUTH);

        return card;
    }

    private JPanel createCalcCardTemplate(String title) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(147, 197, 253));
        card.add(titleLabel, BorderLayout.NORTH);

        return card;
    }

    private void updateView() {
        Budget b = service.getBudget();
        totalBudgetVal.setText(String.format("$%,.2f", b.getTotalBudget()));
        totalSpentVal.setText(String.format("$%,.2f", b.getCurrentSpent()));
        remainingVal.setText(String.format("$%,.2f", b.getRemaining()));

        double totalInc = 0;
        for (Transaction t : service.getTransactions()) {
            if (t.isIncome()) totalInc += t.getAbsoluteAmount();
        }
        incomeVal.setText(String.format("$%,.2f", totalInc));

        int percent = (int) Math.min(100, Math.max(0, (b.getCurrentSpent() / b.getTotalBudget()) * 100));
        budgetProgressBar.setValue(percent);
        budgetProgressBar.setString(percent + "% Used");
        if (percent > 90) {
            budgetProgressBar.setForeground(new Color(239, 68, 68));
        } else if (percent > 70) {
            budgetProgressBar.setForeground(new Color(245, 158, 11));
        } else {
            budgetProgressBar.setForeground(new Color(16, 185, 129));
        }

        Map<String, Double> breakdown = service.getCategoryBreakdown();
        pieChartPanel.setData(breakdown);

        tableModel.setRowCount(0);
        for (Transaction t : service.getTransactions()) {
            String dateStr = dateFormat.format(t.getDate());
            String catName = t.getCategory() != null ? t.getCategory().getDisplayName() : "General";
            String amtStr = (t.isIncome() ? "+$" : "-$") + String.format("%,.2f", t.getAbsoluteAmount());
            tableModel.addRow(new Object[]{dateStr, t.getDescription(), catName, amtStr});
        }
    }

    private void updateTicker(List<CryptoPrice> prices) {
        if (prices == null || prices.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < prices.size(); i++) {
            CryptoPrice cp = prices.get(i);
            String sign = cp.getChange24h() >= 0 ? "▲ +" : "▼ ";
            sb.append(String.format("%s: $%,.2f (%s%.2f%%)", cp.getSymbol(), cp.getPriceUSD(), sign, cp.getChange24h()));
            if (i < prices.size() - 1) sb.append("   |   ");
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

    private static class TransactionCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? new Color(24, 30, 42) : new Color(30, 38, 52));
            }
            if (col == 3 && value != null) {
                String str = value.toString();
                if (str.startsWith("+")) {
                    c.setForeground(new Color(52, 211, 153));
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(new Color(248, 113, 113));
                }
            } else if (col == 2) {
                c.setForeground(new Color(147, 197, 253));
            } else {
                c.setForeground(new Color(229, 231, 235));
            }
            return c;
        }
    }
}
