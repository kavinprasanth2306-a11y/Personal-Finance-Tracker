package com.financetracker.model;

import com.financetracker.exception.InvalidTransactionException;

import java.time.LocalDate;

/**
 * Concrete model representing Expense transactions (e.g., Food, Rent, Entertainment).
 * Demonstrates CO2 Inheritance & Polymorphic behavior:
 * - Overrides getTransactionImpact() to return negative outflow (-amount).
 */
public class ExpenseTransaction extends Transaction {

    public ExpenseTransaction(int id, double amount, LocalDate date, String category, String description)
            throws InvalidTransactionException {
        super(id, amount, date, category, description, TransactionType.EXPENSE);
    }

    public ExpenseTransaction(double amount, LocalDate date, String category, String description)
            throws InvalidTransactionException {
        this(0, amount, date, category, description);
    }

    /**
     * Polymorphic implementation:
     * Expense is an outflow and decreases the net balance (-amount).
     */
    @Override
    public double getTransactionImpact() {
        return -getAmount();
    }
}
