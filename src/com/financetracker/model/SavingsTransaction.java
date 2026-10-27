package com.financetracker.model;

import com.financetracker.exception.InvalidTransactionException;

import java.time.LocalDate;

/**
 * Concrete model representing Savings transactions (e.g., Emergency Fund, Index Funds).
 * Demonstrates CO2 Inheritance & Polymorphic behavior:
 * - Represents money set aside from active spending pool into long-term reserves.
 * - getTransactionImpact() treats it as an allocation reserve (-amount from liquid checking cash).
 */
public class SavingsTransaction extends Transaction {

    public SavingsTransaction(int id, double amount, LocalDate date, String category, String description)
            throws InvalidTransactionException {
        super(id, amount, date, category, description, TransactionType.SAVINGS);
    }

    public SavingsTransaction(double amount, LocalDate date, String category, String description)
            throws InvalidTransactionException {
        this(0, amount, date, category, description);
    }

    /**
     * Polymorphic implementation:
     * Savings is allocated out of liquid operational balance into a wealth reserve.
     */
    @Override
    public double getTransactionImpact() {
        return -getAmount();
    }
}
