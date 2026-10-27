package com.financetracker.model;

import com.financetracker.exception.InvalidTransactionException;

import java.time.LocalDate;

/**
 * Concrete model representing Income transactions (e.g., Salary, Investments, Side Business).
 * Demonstrates CO2 Inheritance & Polymorphic behavior:
 * - Overrides getTransactionImpact() to return positive inflow (+amount).
 */
public class IncomeTransaction extends Transaction {

    public IncomeTransaction(int id, double amount, LocalDate date, String category, String description)
            throws InvalidTransactionException {
        super(id, amount, date, category, description, TransactionType.INCOME);
    }

    public IncomeTransaction(double amount, LocalDate date, String category, String description)
            throws InvalidTransactionException {
        this(0, amount, date, category, description);
    }

    /**
     * Polymorphic implementation:
     * Income is an inflow and increases the net balance (+amount).
     */
    @Override
    public double getTransactionImpact() {
        return getAmount();
    }
}
