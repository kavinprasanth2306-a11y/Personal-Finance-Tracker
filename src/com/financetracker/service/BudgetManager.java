package com.financetracker.service;

import com.financetracker.dao.FinanceDAO;
import com.financetracker.exception.BudgetExceededException;
import com.financetracker.exception.InvalidTransactionException;
import com.financetracker.interfaces.BudgetAlertListener;
import com.financetracker.model.Budget;
import com.financetracker.model.ExpenseTransaction;
import com.financetracker.model.Transaction;
import com.financetracker.model.TransactionType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service Layer coordinating business logic, Java Streams analytics,
 * budget threshold evaluations, and Observer/Listener pattern notifications.
 * Demonstrates CO4:
 * - Java Collections Framework & Java 8+ Streams (groupingBy, summingDouble, filter, sort).
 * - Observer Pattern for decoupled real-time visual alerts.
 * - Robust Java File I/O for CSV and TXT report generation.
 */
public class BudgetManager {
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final FinanceDAO financeDAO;
    private final List<BudgetAlertListener> listeners;

    public BudgetManager() {
        this.financeDAO = new FinanceDAO();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public BudgetManager(FinanceDAO dao) {
        this.financeDAO = dao;
        this.listeners = new CopyOnWriteArrayList<>();
    }

    // =========================================================================
    // OBSERVER PATTERN: LISTENER REGISTRATION (CO4)
    // =========================================================================

    public void addBudgetAlertListener(BudgetAlertListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeBudgetAlertListener(BudgetAlertListener listener) {
        listeners.remove(listener);
    }

    /**
     * Evaluates a potential or recorded expense against category budget thresholds.
     * Fires observer alerts for Warnings (>= 80%) or Violations (>= 100%).
     *
     * @param category Expense category.
     * @param amount   Amount of new or modified expense.
     * @param date     Date of the transaction.
     * @throws BudgetExceededException if user chooses strict enforcement.
     */
    public void evaluateBudgetThreshold(String category, double amount, LocalDate date) throws BudgetExceededException {
        String monthYear = date.format(MONTH_YEAR_FORMATTER);
        Optional<Budget> budgetOpt = financeDAO.getBudgetByCategory(category);

        if (budgetOpt.isPresent()) {
            Budget budget = budgetOpt.get();
            double currentSpent = financeDAO.getCategoryExpenseSumForMonth(category, monthYear);
            double totalProjected = currentSpent + amount;
            double limit = budget.getMonthlyLimit();
            double percentage = (totalProjected / limit) * 100.0;

            if (totalProjected > limit) {
                // Fire Exceeded Notification to all registered UI observers
                for (BudgetAlertListener listener : listeners) {
                    listener.onBudgetExceeded(category, totalProjected, limit, percentage);
                }
                throw new BudgetExceededException(category, currentSpent, limit, amount);
            } else if (percentage >= 80.0) {
                // Fire Warning Notification to all registered UI observers
                for (BudgetAlertListener listener : listeners) {
                    listener.onBudgetWarning(category, totalProjected, limit, percentage);
                }
            }
        }
    }

    /**
     * Executes budget alert check safely without throwing exception to caller.
     */
    public void checkAndNotifyBudget(String category, double amount, LocalDate date) {
        try {
            evaluateBudgetThreshold(category, amount, date);
        } catch (BudgetExceededException ignored) {
            // Already dispatched to listeners
        }
    }

    // =========================================================================
    // TRANSACTION BUSINESS LOGIC
    // =========================================================================

    public boolean recordTransaction(Transaction tx) throws SQLException, InvalidTransactionException {
        if (tx == null) {
            throw new InvalidTransactionException("Transaction cannot be null.");
        }

        // If it's an expense, evaluate real-time threshold observer notifications
        if (tx.getType() == TransactionType.EXPENSE) {
            checkAndNotifyBudget(tx.getCategory(), tx.getAmount(), tx.getDate());
        }

        return financeDAO.addTransaction(tx);
    }

    public boolean updateTransaction(Transaction tx) throws SQLException, InvalidTransactionException {
        if (tx == null) {
            throw new InvalidTransactionException("Transaction cannot be null.");
        }
        return financeDAO.updateTransaction(tx);
    }

    public boolean deleteTransaction(int transactionId) throws SQLException {
        return financeDAO.deleteTransaction(transactionId);
    }

    public List<Transaction> getAllTransactions() {
        return financeDAO.getAllTransactions();
    }

    public List<Transaction> getTransactionsByDateRange(LocalDate start, LocalDate end) {
        return financeDAO.getTransactionsByDateRange(start, end);
    }

    // =========================================================================
    // BUDGET BUSINESS LOGIC
    // =========================================================================

    public boolean saveBudget(Budget budget) throws SQLException {
        return financeDAO.saveOrUpdateBudget(budget);
    }

    public boolean deleteBudget(int budgetId) throws SQLException {
        return financeDAO.deleteBudget(budgetId);
    }

    public List<Budget> getAllBudgets() {
        return financeDAO.getAllBudgets();
    }

    public Optional<Budget> getBudgetByCategory(String category) {
        return financeDAO.getBudgetByCategory(category);
    }

    // =========================================================================
    // STREAM-BASED ANALYTICS & AGGREGATIONS (CO4 Java Collections & Streams)
    // =========================================================================

    /**
     * Calculates total income using Java 8 Streams.
     */
    public double calculateTotalIncome(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Calculates total expenses using Java 8 Streams.
     */
    public double calculateTotalExpenses(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Calculates total savings using Java 8 Streams.
     */
    public double calculateTotalSavings(List<Transaction> transactions) {
        return transactions.stream()
                .filter(t -> t.getType() == TransactionType.SAVINGS)
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    /**
     * Calculates net savings / balance: Income - (Expenses + Savings).
     */
    public double calculateNetBalance(List<Transaction> transactions) {
        double income = calculateTotalIncome(transactions);
        double expenses = calculateTotalExpenses(transactions);
        double savings = calculateTotalSavings(transactions);
        return income - (expenses + savings);
    }

    /**
     * Groups expenses by category and sums their amounts using Streams Collectors.groupingBy.
     * @return Map sorted by expenditure descending.
     */
    public Map<String, Double> getCategoryWiseExpenseDistribution(List<Transaction> transactions) {
        Map<String, Double> unsorted = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        // Sort descending by value for prominent visualization
        return unsorted.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    // =========================================================================
    // REPORT GENERATION & FILE I/O (CO4 Java I/O)
    // =========================================================================

    /**
     * Generates a structured multi-line financial statement for a specified date range.
     */
    public String generateFinancialSummaryText(LocalDate start, LocalDate end) {
        List<Transaction> list = getTransactionsByDateRange(start, end);

        double totalIncome = calculateTotalIncome(list);
        double totalExpenses = calculateTotalExpenses(list);
        double totalSavings = calculateTotalSavings(list);
        double netCashFlow = totalIncome - totalExpenses;
        double unallocatedBalance = netCashFlow - totalSavings;

        Map<String, Double> categoryExpenses = getCategoryWiseExpenseDistribution(list);

        StringBuilder sb = new StringBuilder();
        sb.append("================================================================================\n");
        sb.append("                 PERSONAL FINANCE TRACKER - FINANCIAL REPORT                    \n");
        sb.append("================================================================================\n");
        sb.append(String.format("Report Period:      %s to %s\n", start.format(DISPLAY_DATE_FORMAT), end.format(DISPLAY_DATE_FORMAT)));
        sb.append(String.format("Generated On:       %s\n", LocalDate.now().format(DISPLAY_DATE_FORMAT)));
        sb.append(String.format("Total Transactions: %d\n", list.size()));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("EXECUTIVE SUMMARY\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("  (+) Total Inflow (Income):        $%12.2f\n", totalIncome));
        sb.append(String.format("  (-) Total Outflow (Expenses):     $%12.2f\n", totalExpenses));
        sb.append(String.format("  (=) Operational Cash Flow:        $%12.2f\n", netCashFlow));
        sb.append(String.format("  (-) Total Transferred to Savings: $%12.2f\n", totalSavings));
        sb.append(String.format("  (=) Net Retained Balance:         $%12.2f\n", unallocatedBalance));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("CATEGORY-WISE EXPENSE BREAKDOWN (STREAM AGGREGATIONS)\n");
        sb.append("--------------------------------------------------------------------------------\n");

        if (categoryExpenses.isEmpty()) {
            sb.append("  No recorded expense transactions found within this period.\n");
        } else {
            sb.append(String.format("  %-25s %-15s %-10s\n", "Category", "Amount ($)", "Share (%)"));
            sb.append("  --------------------------------------------------------------\n");
            for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
                double pct = totalExpenses > 0 ? (entry.getValue() / totalExpenses) * 100.0 : 0.0;
                sb.append(String.format("  %-25s $%12.2f     %5.1f%%\n", entry.getKey(), entry.getValue(), pct));
            }
        }

        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("TRANSACTION DETAIL LOG\n");
        sb.append("--------------------------------------------------------------------------------\n");
        for (Transaction tx : list) {
            sb.append("  ").append(tx.getFormattedSummary()).append("\n");
        }
        sb.append("================================================================================\n");

        return sb.toString();
    }

    /**
     * Exports the summary report to a UTF-8 text file.
     */
    public void exportReportToTxt(File destination, LocalDate start, LocalDate end) throws IOException {
        String report = generateFinancialSummaryText(start, end);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination))) {
            writer.write(report);
        }
    }

    /**
     * Exports all provided transactions to standard RFC-compliant CSV format.
     */
    public void exportTransactionsToCsv(File destination, List<Transaction> transactions) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination))) {
            writer.write("ID,Type,Amount,Date,Category,Description\n");
            for (Transaction tx : transactions) {
                writer.write(tx.toCsvRow());
                writer.newLine();
            }
        }
    }
}
