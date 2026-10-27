package com.financetracker.dao;

import com.financetracker.db.DatabaseConnection;
import com.financetracker.exception.InvalidTransactionException;
import com.financetracker.model.*;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) providing complete CRUD operations and SQL-level
 * aggregations for Transactions and Budgets.
 * Demonstrates CO4:
 * - Robust JDBC implementation using PreparedStatements to thwart SQL injection.
 * - try-with-resources blocks for automatic connection, statement, and resultset closure.
 * - Polymorphic mapping from database records to concrete Transaction subclasses.
 */
public class FinanceDAO {
    private static final Logger LOGGER = Logger.getLogger(FinanceDAO.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DatabaseConnection dbManager;

    public FinanceDAO() {
        this.dbManager = DatabaseConnection.getInstance();
    }

    // =========================================================================
    // TRANSACTION CRUD OPERATIONS
    // =========================================================================

    /**
     * Persists a new transaction into the database and updates its generated ID.
     * @param tx Concrete transaction (Income, Expense, or Savings).
     * @return boolean true if inserted successfully.
     * @throws SQLException if database write fails.
     */
    public boolean addTransaction(Transaction tx) throws SQLException {
        String sql = "INSERT INTO transactions (type, amount, date, category, description) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, tx.getType().name());
            pstmt.setDouble(2, tx.getAmount());
            pstmt.setString(3, tx.getDate().format(ISO_FORMATTER));
            pstmt.setString(4, tx.getCategory());
            pstmt.setString(5, tx.getDescription());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        tx.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Updates an existing transaction record by ID.
     */
    public boolean updateTransaction(Transaction tx) throws SQLException {
        String sql = "UPDATE transactions SET type = ?, amount = ?, date = ?, category = ?, description = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, tx.getType().name());
            pstmt.setDouble(2, tx.getAmount());
            pstmt.setString(3, tx.getDate().format(ISO_FORMATTER));
            pstmt.setString(4, tx.getCategory());
            pstmt.setString(5, tx.getDescription());
            pstmt.setInt(6, tx.getId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a transaction record by its unique ID.
     */
    public boolean deleteTransaction(int transactionId) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, transactionId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all transactions ordered chronologically (newest first).
     */
    public List<Transaction> getAllTransactions() {
        String sql = "SELECT id, type, amount, date, category, description FROM transactions ORDER BY date DESC, id DESC";
        List<Transaction> list = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                mapRowToTransaction(rs).ifPresent(list::add);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transactions from database", e);
        }
        return list;
    }

    /**
     * Retrieves transactions within an inclusive start and end date range.
     */
    public List<Transaction> getTransactionsByDateRange(LocalDate start, LocalDate end) {
        String sql = "SELECT id, type, amount, date, category, description FROM transactions "
                   + "WHERE date >= ? AND date <= ? ORDER BY date DESC";
        List<Transaction> list = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, start.format(ISO_FORMATTER));
            pstmt.setString(2, end.format(ISO_FORMATTER));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mapRowToTransaction(rs).ifPresent(list::add);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transactions by date range", e);
        }
        return list;
    }

    /**
     * Retrieves transactions by category.
     */
    public List<Transaction> getTransactionsByCategory(String category) {
        String sql = "SELECT id, type, amount, date, category, description FROM transactions "
                   + "WHERE LOWER(category) = LOWER(?) ORDER BY date DESC";
        List<Transaction> list = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mapRowToTransaction(rs).ifPresent(list::add);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving transactions by category", e);
        }
        return list;
    }

    // =========================================================================
    // BUDGET CRUD OPERATIONS
    // =========================================================================

    /**
     * Inserts or replaces a monthly budget allocation.
     */
    public boolean saveOrUpdateBudget(Budget budget) throws SQLException {
        String sql = "INSERT INTO budgets (category, monthly_limit, month_year) VALUES (?, ?, ?) "
                   + "ON CONFLICT(category) DO UPDATE SET monthly_limit = excluded.monthly_limit, month_year = excluded.month_year";
        
        // Standard SQL fallback for MySQL/SQLite cross-compatibility
        if ("MYSQL".equalsIgnoreCase(dbManager.getDbType())) {
            sql = "INSERT INTO budgets (category, monthly_limit, month_year) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE monthly_limit = VALUES(monthly_limit), month_year = VALUES(month_year)";
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, budget.getCategory());
            pstmt.setDouble(2, budget.getMonthlyLimit());
            pstmt.setString(3, budget.getMonthYear());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        budget.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes a budget by its primary ID.
     */
    public boolean deleteBudget(int budgetId) throws SQLException {
        String sql = "DELETE FROM budgets WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, budgetId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a budget by category name.
     */
    public boolean deleteBudgetByCategory(String category) throws SQLException {
        String sql = "DELETE FROM budgets WHERE LOWER(category) = LOWER(?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.trim());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Retrieves all saved category budgets.
     */
    public List<Budget> getAllBudgets() {
        String sql = "SELECT id, category, monthly_limit, month_year FROM budgets ORDER BY category ASC";
        List<Budget> list = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                try {
                    Budget b = new Budget(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getDouble("monthly_limit"),
                            rs.getString("month_year")
                    );
                    list.add(b);
                } catch (InvalidTransactionException e) {
                    LOGGER.log(Level.WARNING, "Skipping malformed budget record ID: " + rs.getInt("id"), e);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving budgets", e);
        }
        return list;
    }

    /**
     * Looks up a budget by category name.
     */
    public Optional<Budget> getBudgetByCategory(String category) {
        String sql = "SELECT id, category, monthly_limit, month_year FROM budgets WHERE LOWER(category) = LOWER(?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Budget b = new Budget(
                            rs.getInt("id"),
                            rs.getString("category"),
                            rs.getDouble("monthly_limit"),
                            rs.getString("month_year")
                    );
                    return Optional.of(b);
                }
            }
        } catch (SQLException | InvalidTransactionException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving budget for category: " + category, e);
        }
        return Optional.empty();
    }

    // =========================================================================
    // AGGREGATION QUERIES (CO4 JDBC Persistence)
    // =========================================================================

    /**
     * Computes total sum of expenses for a specific category within a month (YYYY-MM).
     */
    public double getCategoryExpenseSumForMonth(String category, String monthYearPrefix) {
        String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM transactions "
                   + "WHERE type = 'EXPENSE' AND LOWER(category) = LOWER(?) AND date LIKE ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, category.trim());
            pstmt.setString(2, monthYearPrefix + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating monthly category expense sum", e);
        }
        return 0.0;
    }

    /**
     * Aggregates total expenses grouped by category for a specific month.
     */
    public Map<String, Double> getCategoryWiseExpensesForMonth(String monthYearPrefix) {
        String sql = "SELECT category, SUM(amount) AS total FROM transactions "
                   + "WHERE type = 'EXPENSE' AND date LIKE ? GROUP BY category ORDER BY total DESC";
        Map<String, Double> map = new LinkedHashMap<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, monthYearPrefix + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("category"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error aggregating category-wise expenses", e);
        }
        return map;
    }

    // =========================================================================
    // POLYMORPHIC FACTORY MAPPING (CO2 Polymorphism)
    // =========================================================================

    /**
     * Converts a database ResultSet row into its specialized concrete Transaction subclass.
     */
    private Optional<Transaction> mapRowToTransaction(ResultSet rs) {
        try {
            int id = rs.getInt("id");
            String typeStr = rs.getString("type");
            double amount = rs.getDouble("amount");
            LocalDate date = LocalDate.parse(rs.getString("date"), ISO_FORMATTER);
            String category = rs.getString("category");
            String description = rs.getString("description");

            TransactionType type = TransactionType.fromString(typeStr);
            Transaction tx;

            switch (type) {
                case INCOME:
                    tx = new IncomeTransaction(id, amount, date, category, description);
                    break;
                case EXPENSE:
                    tx = new ExpenseTransaction(id, amount, date, category, description);
                    break;
                case SAVINGS:
                    tx = new SavingsTransaction(id, amount, date, category, description);
                    break;
                default:
                    LOGGER.warning("Unknown transaction type encountered: " + typeStr);
                    return Optional.empty();
            }
            return Optional.of(tx);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to map row to Transaction object", e);
            return Optional.empty();
        }
    }
}
