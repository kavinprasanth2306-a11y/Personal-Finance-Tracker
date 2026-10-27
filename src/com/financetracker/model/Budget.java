package com.financetracker.model;

import com.financetracker.exception.InvalidTransactionException;
import com.financetracker.interfaces.Reportable;

/**
 * Model class representing a monthly budget allocation per category.
 * Demonstrates CO2 Encapsulation and Reportable contract.
 */
public class Budget implements Reportable {
    private int id;
    private String category;
    private double monthlyLimit;
    private String monthYear; // Format: YYYY-MM

    public Budget(int id, String category, double monthlyLimit, String monthYear) throws InvalidTransactionException {
        this.id = id;
        setCategory(category);
        setMonthlyLimit(monthlyLimit);
        setMonthYear(monthYear);
    }

    public Budget(String category, double monthlyLimit, String monthYear) throws InvalidTransactionException {
        this(0, category, monthlyLimit, monthYear);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) throws InvalidTransactionException {
        if (category == null || category.trim().isEmpty()) {
            throw new InvalidTransactionException("category", "Budget category cannot be empty or null.");
        }
        this.category = category.trim();
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) throws InvalidTransactionException {
        if (Double.isNaN(monthlyLimit) || monthlyLimit <= 0.0) {
            throw new InvalidTransactionException("monthlyLimit", "Monthly limit must be a positive number > 0.00.");
        }
        this.monthlyLimit = Math.round(monthlyLimit * 100.0) / 100.0;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) throws InvalidTransactionException {
        if (monthYear == null || !monthYear.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new InvalidTransactionException("monthYear", "Month-Year must be in the valid format YYYY-MM (e.g. 2026-10).");
        }
        this.monthYear = monthYear;
    }

    @Override
    public String getFormattedSummary() {
        return String.format("Budget [%s] - Category: %-15s | Limit: $%10.2f",
                monthYear, category, monthlyLimit);
    }

    @Override
    public String toCsvRow() {
        return String.format("%d,\"%s\",%.2f,%s", id, category, monthlyLimit, monthYear);
    }

    @Override
    public String toString() {
        return getFormattedSummary();
    }
}
