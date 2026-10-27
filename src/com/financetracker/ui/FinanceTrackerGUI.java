package com.financetracker.ui;

import com.financetracker.exception.InvalidTransactionException;
import com.financetracker.interfaces.BudgetAlertListener;
import com.financetracker.model.*;
import com.financetracker.service.BudgetManager;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Modern Swing GUI for Personal Finance Tracker.
 * Demonstrates CO4:
 * - Advanced Swing components (TabbedPane, JTable, Renderers, Card layouts).
 * - Real-time Observer Pattern implementation for budget warnings and exceedances.
 * - Interactive CRUD workflows with transaction validation.
 * - File I/O triggers for report and CSV export.
 */
public class FinanceTrackerGUI extends JFrame implements BudgetAlertListener {
    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Primary Colors
    private static final Color PRIMARY_COLOR = new Color(37, 99, 235);    // #2563eb
    private static final Color HEADER_BG = new Color(15, 23, 42);          // Slate 900
    private static final Color BG_LIGHT = new Color(241, 245, 249);        // Slate 100
    private static final Color CARD_BG = Color.WHITE;
    private static final Color SUCCESS_COLOR = new Color(16, 185, 129);    // Emerald
    private static final Color WARNING_COLOR = new Color(245, 158, 11);    // Amber
    private static final Color DANGER_COLOR = new Color(239, 68, 68);      // Red
    private static final Color TEXT_DARK = new Color(30, 41, 59);

    private final BudgetManager budgetManager;

    // UI Banner Components
    private JPanel alertBannerPanel;
    private JLabel alertBannerLabel;

    // KPI Metric Labels
    private JLabel lblTotalIncome;
    private JLabel lblTotalExpense;
    private JLabel lblTotalSavings;
    private JLabel lblNetBalance;

    // Charts
    private ChartPanel chartPanel;

    // Transactions Table & Form
    private DefaultTableModel transactionTableModel;
    private JTable transactionTable;
    private JComboBox<TransactionType> comboType;
    private JTextField txtAmount;
    private JComboBox<String> comboCategory;
    private JTextField txtDate;
    private JTextField txtDescription;
    private JTextField txtSelectedId;

    // Budgets Table & Form
    private DefaultTableModel budgetTableModel;
    private JTable budgetTable;
    private JTextField txtBudgetCategory;
    private JTextField txtBudgetLimit;
    private JTextField txtBudgetPeriod;

    // Reports Components
    private JTextField txtReportStart;
    private JTextField txtReportEnd;
    private JTextArea txtReportOutput;

    public FinanceTrackerGUI(BudgetManager budgetManager) {
        this.budgetManager = budgetManager;
        this.budgetManager.addBudgetAlertListener(this);

        try {
            this.budgetManager.resequenceTransactionIds();
        } catch (Exception ignored) {}

        initWindow();
        initComponents();
        refreshAllData();
    }

    private void initWindow() {
        setTitle("Personal Finance Tracker - OOP Micro-Project (CO2 & CO4)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 820);
        setMinimumSize(new Dimension(950, 680));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_LIGHT);
        setLayout(new BorderLayout());
    }

    private void initComponents() {
        // 1. Top Section: Header + Observer Alert Banner
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(createAppHeader(), BorderLayout.NORTH);
        topContainer.add(createAlertBanner(), BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // 2. Center Section: Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);

        tabbedPane.addTab("  Dashboard & Analytics  ", createDashboardTab());
        tabbedPane.addTab("  Manage Transactions  ", createTransactionsTab());
        tabbedPane.addTab("  Category Budgets  ", createBudgetsTab());
        tabbedPane.addTab("  Reports & File Export  ", createReportsTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // =========================================================================
    // HEADER & REAL-TIME OBSERVER STATUS BANNER
    // =========================================================================

    private JPanel createAppHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER_BG);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Personal Finance Tracker");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Academic Micro-Project | OOP Principles (CO2) & Advanced Java Constructs (CO4)");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(148, 163, 184));

        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        header.add(titlePanel, BorderLayout.WEST);

        JButton btnQuickRefresh = new JButton("Refresh Data");
        styleButton(btnQuickRefresh, new Color(51, 65, 85), Color.WHITE);
        btnQuickRefresh.addActionListener(e -> refreshAllData());
        header.add(btnQuickRefresh, BorderLayout.EAST);

        return header;
    }

    private JPanel createAlertBanner() {
        alertBannerPanel = new JPanel(new BorderLayout());
        alertBannerPanel.setBackground(new Color(236, 253, 245)); // Soft green
        alertBannerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(209, 250, 229)),
                new EmptyBorder(10, 24, 10, 24)
        ));

        alertBannerLabel = new JLabel("[STATUS OK] All category spendings are within allocated budget limits.");
        alertBannerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        alertBannerLabel.setForeground(new Color(6, 95, 70));

        JButton btnDismiss = new JButton("Reset Banner");
        btnDismiss.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDismiss.setFocusPainted(false);
        btnDismiss.addActionListener(e -> resetAlertBanner());

        alertBannerPanel.add(alertBannerLabel, BorderLayout.CENTER);
        alertBannerPanel.add(btnDismiss, BorderLayout.EAST);

        return alertBannerPanel;
    }

    public void resetAlertBanner() {
        alertBannerPanel.setBackground(new Color(236, 253, 245));
        alertBannerLabel.setText("[STATUS OK] All category spendings are within allocated budget limits.");
        alertBannerLabel.setForeground(new Color(6, 95, 70));
    }

    // =========================================================================
    // OBSERVER PATTERN CALLBACKS (CO4)
    // =========================================================================

    @Override
    public void onBudgetWarning(String category, double currentSpending, double limit, double percentage) {
        SwingUtilities.invokeLater(() -> {
            alertBannerPanel.setBackground(new Color(254, 243, 199)); // Amber warning
            alertBannerLabel.setForeground(new Color(146, 64, 14));
            alertBannerLabel.setText(String.format("[BUDGET WARNING] Category '%s' spending is at %.1f%% ($%.2f / $%.2f)!",
                    category, percentage, currentSpending, limit));

            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,
                    String.format("Warning: Spending in '%s' reached %.1f%% of its monthly limit!\n\nTotal Spent: %s\nBudget Limit: %s",
                            category, percentage, CURRENCY.format(currentSpending), CURRENCY.format(limit)),
                    "Budget Threshold Warning (CO4 Observer)",
                    JOptionPane.WARNING_MESSAGE);
        });
    }

    @Override
    public void onBudgetExceeded(String category, double currentSpending, double limit, double percentage) {
        SwingUtilities.invokeLater(() -> {
            alertBannerPanel.setBackground(new Color(254, 226, 226)); // Crimson alert
            alertBannerLabel.setForeground(new Color(153, 27, 27));
            alertBannerLabel.setText(String.format("[CRITICAL ALERT] Budget EXCEEDED for '%s' by $%.2f (Spending: %.1f%%)!",
                    category, (currentSpending - limit), percentage));

            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,
                    String.format("CRITICAL: Budget Exceeded for '%s'!\n\nTotal Spent: %s\nBudget Limit: %s\nOverspent By: %s (%.1f%%)",
                            category, CURRENCY.format(currentSpending), CURRENCY.format(limit),
                            CURRENCY.format(currentSpending - limit), percentage),
                    "Budget Limit Breach Alert (CO4 Observer)",
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    // =========================================================================
    // TAB 1: DASHBOARD & CHARTS
    // =========================================================================

    private JPanel createDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 20, 20));

        // 1. KPI Cards Panel (Top)
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiGrid.setOpaque(false);

        lblTotalIncome = new JLabel("$0.00");
        lblTotalExpense = new JLabel("$0.00");
        lblTotalSavings = new JLabel("$0.00");
        lblNetBalance = new JLabel("$0.00");

        kpiGrid.add(createKpiCard("Total Income", lblTotalIncome, SUCCESS_COLOR));
        kpiGrid.add(createKpiCard("Total Expenses", lblTotalExpense, DANGER_COLOR));
        kpiGrid.add(createKpiCard("Savings Reserve", lblTotalSavings, PRIMARY_COLOR));
        kpiGrid.add(createKpiCard("Net Retained Balance", lblNetBalance, new Color(139, 92, 246)));

        panel.add(kpiGrid, BorderLayout.NORTH);

        // 2. Custom Graphics2D Chart Panel (Center)
        chartPanel = new ChartPanel();
        panel.add(chartPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accentColor),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(new Color(100, 116, 139));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(TEXT_DARK);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // TAB 2: MANAGE TRANSACTIONS (CRUD)
    // =========================================================================

    private JPanel createTransactionsTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 20, 20));

        // Left Form: Add/Edit Transaction
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setPreferredSize(new Dimension(340, 0));
        formCard.setBackground(CARD_BG);
        formCard.setBorder(new CompoundBorder(new LineBorder(new Color(226, 232, 240)), new EmptyBorder(16, 16, 16, 16)));

        JLabel formTitle = new JLabel("Transaction Details");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        formTitle.setForeground(TEXT_DARK);
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridLayout(12, 1, 0, 4));
        fieldsPanel.setOpaque(false);
        fieldsPanel.setBorder(new EmptyBorder(12, 0, 12, 0));

        txtSelectedId = new JTextField();
        txtSelectedId.setVisible(false);

        comboType = new JComboBox<>(TransactionType.values());
        txtAmount = new JTextField();
        comboCategory = new JComboBox<>(new String[]{
                "Salary", "Investments", "Food & Dining", "Rent & Housing", "Utilities",
                "Transportation", "Entertainment", "Healthcare", "Emergency Fund", "Index Funds"
        });
        comboCategory.setEditable(true);

        txtDate = new JTextField(LocalDate.now().format(ISO_DATE));
        txtDescription = new JTextField();

        fieldsPanel.add(new JLabel("Transaction Type:"));
        fieldsPanel.add(comboType);
        fieldsPanel.add(new JLabel("Amount ($):"));
        fieldsPanel.add(txtAmount);
        fieldsPanel.add(new JLabel("Category:"));
        fieldsPanel.add(comboCategory);
        fieldsPanel.add(new JLabel("Date (YYYY-MM-DD):"));
        fieldsPanel.add(txtDate);
        fieldsPanel.add(new JLabel("Description (Optional):"));
        fieldsPanel.add(txtDescription);

        formCard.add(fieldsPanel, BorderLayout.CENTER);

        // Form Action Buttons
        JPanel btnPanel = new JPanel(new GridLayout(3, 2, 8, 8));
        btnPanel.setOpaque(false);

        JButton btnAdd = new JButton("Add Transaction");
        styleButton(btnAdd, PRIMARY_COLOR, Color.WHITE);
        btnAdd.addActionListener(e -> handleAddTransaction());

        JButton btnUpdate = new JButton("Update Selected");
        styleButton(btnUpdate, new Color(14, 165, 233), Color.WHITE);
        btnUpdate.addActionListener(e -> handleUpdateTransaction());

        JButton btnDelete = new JButton("Delete Selected");
        styleButton(btnDelete, DANGER_COLOR, Color.WHITE);
        btnDelete.addActionListener(e -> handleDeleteTransaction());

        JButton btnClear = new JButton("Clear Form");
        styleButton(btnClear, new Color(100, 116, 139), Color.WHITE);
        btnClear.addActionListener(e -> clearTransactionForm());

        JButton btnDeleteAll = new JButton("Delete All (Reset to 1)");
        styleButton(btnDeleteAll, new Color(185, 28, 28), Color.WHITE);
        btnDeleteAll.addActionListener(e -> handleDeleteAllTransactions());

        JButton btnResequence = new JButton("Renumber IDs (1..N)");
        styleButton(btnResequence, new Color(79, 70, 229), Color.WHITE);
        btnResequence.addActionListener(e -> handleResequence());

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        btnPanel.add(btnDeleteAll);
        btnPanel.add(btnResequence);

        formCard.add(btnPanel, BorderLayout.SOUTH);
        panel.add(formCard, BorderLayout.WEST);

        // Right Table: Transactions List
        String[] columns = {"ID", "Date", "Type", "Category", "Amount", "Balance Impact", "Description"};
        transactionTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        transactionTable = new JTable(transactionTableModel);
        transactionTable.setRowHeight(28);
        transactionTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        transactionTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom Cell Formatting
        transactionTable.getColumnModel().getColumn(4).setCellRenderer(new CurrencyCellRenderer());
        transactionTable.getColumnModel().getColumn(5).setCellRenderer(new ImpactCellRenderer());

        transactionTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && transactionTable.getSelectedRow() != -1) {
                populateFormFromSelectedRow();
            }
        });

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(new LineBorder(new Color(226, 232, 240)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void handleAddTransaction() {
        try {
            double amount = parseAmount(txtAmount.getText());
            LocalDate date = LocalDate.parse(txtDate.getText().trim(), ISO_DATE);
            String category = (String) comboCategory.getSelectedItem();
            String desc = txtDescription.getText().trim();
            TransactionType type = (TransactionType) comboType.getSelectedItem();

            Transaction tx;
            if (type == TransactionType.INCOME) {
                tx = new IncomeTransaction(amount, date, category, desc);
            } else if (type == TransactionType.EXPENSE) {
                tx = new ExpenseTransaction(amount, date, category, desc);
            } else {
                tx = new SavingsTransaction(amount, date, category, desc);
            }

            budgetManager.recordTransaction(tx);
            JOptionPane.showMessageDialog(this, "Transaction saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearTransactionForm();
            refreshAllData();

        } catch (InvalidTransactionException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validation Error (CO4 Exception)", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to save transaction: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdateTransaction() {
        if (txtSelectedId.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a transaction from the table to update.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            int id = Integer.parseInt(txtSelectedId.getText().trim());
            double amount = parseAmount(txtAmount.getText());
            LocalDate date = LocalDate.parse(txtDate.getText().trim(), ISO_DATE);
            String category = (String) comboCategory.getSelectedItem();
            String desc = txtDescription.getText().trim();
            TransactionType type = (TransactionType) comboType.getSelectedItem();

            Transaction tx;
            if (type == TransactionType.INCOME) {
                tx = new IncomeTransaction(id, amount, date, category, desc);
            } else if (type == TransactionType.EXPENSE) {
                tx = new ExpenseTransaction(id, amount, date, category, desc);
            } else {
                tx = new SavingsTransaction(id, amount, date, category, desc);
            }

            budgetManager.updateTransaction(tx);
            JOptionPane.showMessageDialog(this, "Transaction updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            clearTransactionForm();
            refreshAllData();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to update transaction: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a transaction from the table to delete.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int id = (int) transactionTableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete transaction ID " + id + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                budgetManager.deleteTransaction(id);
                clearTransactionForm();
                refreshAllData();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Deletion failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteAllTransactions() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete ALL transactions?\nThis will clear all transactions and reset the ID sequence back to 1.",
                "Confirm Delete All", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                budgetManager.deleteAllTransactions();
                clearTransactionForm();
                refreshAllData();
                JOptionPane.showMessageDialog(this,
                        "All transactions deleted successfully!\nThe next transaction will start at ID: 1.",
                        "Sequence Reset", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to clear transactions: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleResequence() {
        try {
            budgetManager.resequenceTransactionIds();
            refreshAllData();
            JOptionPane.showMessageDialog(this,
                    "Transaction IDs successfully resequenced to 1, 2, 3...",
                    "Resequenced", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Resequence failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateFormFromSelectedRow() {
        int row = transactionTable.getSelectedRow();
        if (row >= 0) {
            txtSelectedId.setText(transactionTableModel.getValueAt(row, 0).toString());
            txtDate.setText(transactionTableModel.getValueAt(row, 1).toString());
            comboType.setSelectedItem(TransactionType.fromString(transactionTableModel.getValueAt(row, 2).toString()));
            comboCategory.setSelectedItem(transactionTableModel.getValueAt(row, 3).toString());
            txtAmount.setText(transactionTableModel.getValueAt(row, 4).toString());
            txtDescription.setText(transactionTableModel.getValueAt(row, 6).toString());
        }
    }

    private void clearTransactionForm() {
        txtSelectedId.setText("");
        txtAmount.setText("");
        txtDate.setText(LocalDate.now().format(ISO_DATE));
        txtDescription.setText("");
        transactionTable.clearSelection();
    }

    // =========================================================================
    // TAB 3: CATEGORY BUDGETS
    // =========================================================================

    private JPanel createBudgetsTab() {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 20, 20));

        // Left Budget Form
        JPanel formCard = new JPanel(new BorderLayout());
        formCard.setPreferredSize(new Dimension(320, 0));
        formCard.setBackground(CARD_BG);
        formCard.setBorder(new CompoundBorder(new LineBorder(new Color(226, 232, 240)), new EmptyBorder(16, 16, 16, 16)));

        JLabel formTitle = new JLabel("Configure Category Budget");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        formTitle.setForeground(TEXT_DARK);
        formCard.add(formTitle, BorderLayout.NORTH);

        JPanel fields = new JPanel(new GridLayout(8, 1, 0, 4));
        fields.setOpaque(false);
        fields.setBorder(new EmptyBorder(16, 0, 16, 0));

        txtBudgetCategory = new JTextField();
        txtBudgetLimit = new JTextField();
        txtBudgetPeriod = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));

        fields.add(new JLabel("Category Name:"));
        fields.add(txtBudgetCategory);
        fields.add(new JLabel("Monthly Limit ($):"));
        fields.add(txtBudgetLimit);
        fields.add(new JLabel("Month-Year (YYYY-MM):"));
        fields.add(txtBudgetPeriod);

        formCard.add(fields, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnSaveBudget = new JButton("Save Budget");
        styleButton(btnSaveBudget, PRIMARY_COLOR, Color.WHITE);
        btnSaveBudget.addActionListener(e -> handleSaveBudget());

        JButton btnDeleteBudget = new JButton("Delete Budget");
        styleButton(btnDeleteBudget, DANGER_COLOR, Color.WHITE);
        btnDeleteBudget.addActionListener(e -> handleDeleteBudget());

        btnPanel.add(btnSaveBudget);
        btnPanel.add(btnDeleteBudget);

        formCard.add(btnPanel, BorderLayout.SOUTH);
        panel.add(formCard, BorderLayout.WEST);

        // Right Budget Table
        String[] cols = {"Category", "Monthly Limit", "Period"};
        budgetTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        budgetTable = new JTable(budgetTableModel);
        budgetTable.setRowHeight(28);
        budgetTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        budgetTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        budgetTable.getColumnModel().getColumn(1).setCellRenderer(new CurrencyCellRenderer());

        budgetTable.getSelectionModel().addListSelectionListener(e -> {
            int row = budgetTable.getSelectedRow();
            if (row != -1) {
                txtBudgetCategory.setText(budgetTableModel.getValueAt(row, 0).toString());
                txtBudgetLimit.setText(budgetTableModel.getValueAt(row, 1).toString());
                txtBudgetPeriod.setText(budgetTableModel.getValueAt(row, 2).toString());
            }
        });

        JScrollPane scrollPane = new JScrollPane(budgetTable);
        scrollPane.setBorder(new LineBorder(new Color(226, 232, 240)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void handleSaveBudget() {
        try {
            String cat = txtBudgetCategory.getText().trim();
            double limit = parseAmount(txtBudgetLimit.getText());
            String period = txtBudgetPeriod.getText().trim();

            Budget b = new Budget(cat, limit, period);
            budgetManager.saveBudget(b);

            JOptionPane.showMessageDialog(this, "Budget saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            txtBudgetCategory.setText("");
            txtBudgetLimit.setText("");
            refreshAllData();

        } catch (InvalidTransactionException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving budget: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteBudget() {
        int row = budgetTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a budget to delete.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String category = budgetTableModel.getValueAt(row, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this, "Delete budget for '" + category + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                budgetManager.getAllBudgets().stream()
                        .filter(b -> b.getCategory().equalsIgnoreCase(category))
                        .findFirst()
                        .ifPresent(b -> {
                            try { budgetManager.deleteBudget(b.getId()); } catch (Exception ignored) {}
                        });
                refreshAllData();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to delete: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // TAB 4: REPORTS & EXPORT (JAVA I/O - CO4)
    // =========================================================================

    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new EmptyBorder(16, 20, 20, 20));

        // Filter Controls Card (Top)
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterCard.setBackground(CARD_BG);
        filterCard.setBorder(new LineBorder(new Color(226, 232, 240)));

        LocalDate now = LocalDate.now();
        txtReportStart = new JTextField(now.withDayOfMonth(1).format(ISO_DATE), 10);
        txtReportEnd = new JTextField(now.format(ISO_DATE), 10);

        JButton btnGenerateReport = new JButton("Generate Report");
        styleButton(btnGenerateReport, PRIMARY_COLOR, Color.WHITE);
        btnGenerateReport.addActionListener(e -> generateReportPreview());

        JButton btnExportTxt = new JButton("Export to .TXT");
        styleButton(btnExportTxt, new Color(51, 65, 85), Color.WHITE);
        btnExportTxt.addActionListener(e -> handleExportTxt());

        JButton btnExportCsv = new JButton("Export to .CSV");
        styleButton(btnExportCsv, SUCCESS_COLOR, Color.WHITE);
        btnExportCsv.addActionListener(e -> handleExportCsv());

        filterCard.add(new JLabel("From (YYYY-MM-DD):"));
        filterCard.add(txtReportStart);
        filterCard.add(new JLabel("To (YYYY-MM-DD):"));
        filterCard.add(txtReportEnd);
        filterCard.add(btnGenerateReport);
        filterCard.add(btnExportTxt);
        filterCard.add(btnExportCsv);

        panel.add(filterCard, BorderLayout.NORTH);

        // Report Preview Text Area (Center)
        txtReportOutput = new JTextArea();
        txtReportOutput.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtReportOutput.setEditable(false);
        txtReportOutput.setBackground(CARD_BG);
        txtReportOutput.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(txtReportOutput);
        scrollPane.setBorder(new LineBorder(new Color(226, 232, 240)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void generateReportPreview() {
        try {
            LocalDate start = LocalDate.parse(txtReportStart.getText().trim(), ISO_DATE);
            LocalDate end = LocalDate.parse(txtReportEnd.getText().trim(), ISO_DATE);
            String report = budgetManager.generateFinancialSummaryText(start, end);
            txtReportOutput.setText(report);
            txtReportOutput.setCaretPosition(0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD.", "Date Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExportTxt() {
        try {
            LocalDate start = LocalDate.parse(txtReportStart.getText().trim(), ISO_DATE);
            LocalDate end = LocalDate.parse(txtReportEnd.getText().trim(), ISO_DATE);

            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("financial_summary_" + LocalDate.now() + ".txt"));
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                budgetManager.exportReportToTxt(chooser.getSelectedFile(), start, end);
                JOptionPane.showMessageDialog(this, "Report exported to TXT successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleExportCsv() {
        try {
            List<Transaction> list = budgetManager.getAllTransactions();
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new File("transactions_" + LocalDate.now() + ".csv"));
            int result = chooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                budgetManager.exportTransactionsToCsv(chooser.getSelectedFile(), list);
                JOptionPane.showMessageDialog(this, "Transactions exported to CSV successfully!", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // DATA REFRESH & SYNCHRONIZATION
    // =========================================================================

    public void refreshAllData() {
        List<Transaction> transactions = budgetManager.getAllTransactions();
        List<Budget> budgets = budgetManager.getAllBudgets();

        // 1. Calculate KPI Metrics using Java 8 Streams (CO4)
        double totalInc = budgetManager.calculateTotalIncome(transactions);
        double totalExp = budgetManager.calculateTotalExpenses(transactions);
        double totalSav = budgetManager.calculateTotalSavings(transactions);
        double netBal = budgetManager.calculateNetBalance(transactions);
        Map<String, Double> categoryDist = budgetManager.getCategoryWiseExpenseDistribution(transactions);

        // Update KPI Labels
        lblTotalIncome.setText(CURRENCY.format(totalInc));
        lblTotalExpense.setText(CURRENCY.format(totalExp));
        lblTotalSavings.setText(CURRENCY.format(totalSav));
        lblNetBalance.setText(CURRENCY.format(netBal));

        // 2. Update Graphics2D Chart Panel
        chartPanel.updateData(totalInc, totalExp, totalSav, categoryDist);

        // 3. Update Transactions Table
        transactionTableModel.setRowCount(0);
        for (Transaction tx : transactions) {
            transactionTableModel.addRow(new Object[]{
                    tx.getId(),
                    tx.getDate().format(ISO_DATE),
                    tx.getType().name(),
                    tx.getCategory(),
                    tx.getAmount(),
                    tx.getTransactionImpact(),
                    tx.getDescription()
            });
        }

        // 4. Update Budgets Table
        budgetTableModel.setRowCount(0);
        for (Budget b : budgets) {
            budgetTableModel.addRow(new Object[]{
                    b.getCategory(),
                    b.getMonthlyLimit(),
                    b.getMonthYear()
            });
        }

        // 5. Update Default Report Preview
        generateReportPreview();
    }

    // =========================================================================
    // HELPER METHODS & CUSTOM RENDERERS
    // =========================================================================

    private double parseAmount(String text) throws InvalidTransactionException {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception e) {
            throw new InvalidTransactionException("amount", "Amount must be a valid positive number.");
        }
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private static class CurrencyCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (value instanceof Number) {
                setText(CURRENCY.format(((Number) value).doubleValue()));
            }
            setHorizontalAlignment(RIGHT);
            return this;
        }
    }

    private static class ImpactCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            if (value instanceof Number) {
                double val = ((Number) value).doubleValue();
                setText(CURRENCY.format(val));
                if (val > 0) {
                    setForeground(new Color(16, 185, 129)); // Positive green
                    setText("+" + CURRENCY.format(val));
                } else {
                    setForeground(new Color(239, 68, 68)); // Negative red
                }
            }
            setHorizontalAlignment(RIGHT);
            return this;
        }
    }
}
