package com.financetracker.model;

import com.financetracker.exception.InvalidTransactionException;
import com.financetracker.interfaces.Reportable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Abstract Base Class representing a financial transaction.
 * Demonstrates CO2 OOP Principles:
 * - Abstraction: Cannot be directly instantiated; defines generic contracts.
 * - Encapsulation: Strict private fields with rigorous validation in setters.
 * - Interface Implementation: Implements Reportable and Comparable contracts.
 */
public abstract class Transaction implements Reportable, Comparable<Transaction> {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private int id;
    private double amount;
    private LocalDate date;
    private String category;
    private String description;
    private final TransactionType type;

    /**
     * Protected constructor to be invoked by concrete subclasses.
     *
     * @param id          Unique transaction identifier (0 for new, persisted ID from DB).
     * @param amount      Monetary value of the transaction (must be > 0).
     * @param date        Date the transaction occurred (cannot be null).
     * @param category    Category label (cannot be empty or null).
     * @param description Brief contextual note (optional, sanitized).
     * @param type        Transaction classification (INCOME, EXPENSE, SAVINGS).
     * @throws InvalidTransactionException if any invariant validation fails.
     */
    protected Transaction(int id, double amount, LocalDate date, String category, String description, TransactionType type)
            throws InvalidTransactionException {
        this.id = id;
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        setAmount(amount);
        setDate(date);
        setCategory(category);
        setDescription(description);
    }

    // ==========================================
    // Abstract Method (Polymorphism - CO2)
    // ==========================================
    /**
     * Calculates the net numerical financial impact on the user's cash balance.
     * Concrete implementations determine positive (inflow) vs negative (outflow).
     *
     * @return Positive value for Income, negative value for Expense or Savings outflow.
     */
    public abstract double getTransactionImpact();

    // ==========================================
    // Encapsulated Getters & Setters with Validation
    // ==========================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) throws InvalidTransactionException {
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0.0) {
            throw new InvalidTransactionException("amount", "Transaction amount must be a positive number greater than 0.00.");
        }
        // Round to 2 decimal places to maintain currency precision
        this.amount = Math.round(amount * 100.0) / 100.0;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) throws InvalidTransactionException {
        if (date == null) {
            throw new InvalidTransactionException("date", "Transaction date cannot be null.");
        }
        this.date = date;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) throws InvalidTransactionException {
        if (category == null || category.trim().isEmpty()) {
            throw new InvalidTransactionException("category", "Category cannot be empty or null.");
        }
        this.category = category.trim();
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public TransactionType getType() {
        return type;
    }

    // ==========================================
    // Reportable Interface Implementation (CO2)
    // ==========================================

    @Override
    public String getFormattedSummary() {
        return String.format("[%s] ID: %-5d | Date: %s | Type: %-8s | Cat: %-15s | Amt: $%10.2f | Note: %s",
                getType().name(),
                id,
                date.format(DATE_FORMATTER),
                type.getDisplayName(),
                category,
                amount,
                getDescription().isEmpty() ? "None" : getDescription());
    }

    @Override
    public String toCsvRow() {
        // CSV Format: id,type,amount,date,category,description
        String sanitizedDesc = getDescription().replace("\"", "\"\"");
        return String.format("%d,%s,%.2f,%s,\"%s\",\"%s\"",
                id,
                type.name(),
                amount,
                date.format(DATE_FORMATTER),
                category,
                sanitizedDesc);
    }

    // ==========================================
    // Comparable Implementation (Default sort by date descending)
    // ==========================================

    @Override
    public int compareTo(Transaction other) {
        if (other == null) return 1;
        int dateCompare = other.date.compareTo(this.date); // newest first
        if (dateCompare != 0) return dateCompare;
        return Integer.compare(other.id, this.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction)) return false;
        Transaction that = (Transaction) o;
        return id == that.id && id != 0 && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return getFormattedSummary();
    }
}
