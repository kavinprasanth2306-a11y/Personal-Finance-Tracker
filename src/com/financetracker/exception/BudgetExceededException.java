package com.financetracker.exception;

/**
 * Custom exception thrown when an expense transaction causes a category's
 * total expenditure to surpass its allocated budget threshold.
 * Demonstrates CO4 custom domain exception handling.
 */
public class BudgetExceededException extends Exception {
    private final String category;
    private final double currentTotal;
    private final double budgetLimit;
    private final double attemptedAmount;

    public BudgetExceededException(String category, double currentTotal, double budgetLimit, double attemptedAmount) {
        super(String.format("Budget Exceeded for category '%s'! Limit: $%.2f, Current: $%.2f, Attempted: $%.2f (Breach by: $%.2f)",
                category, budgetLimit, currentTotal, attemptedAmount, (currentTotal + attemptedAmount - budgetLimit)));
        this.category = category;
        this.currentTotal = currentTotal;
        this.budgetLimit = budgetLimit;
        this.attemptedAmount = attemptedAmount;
    }

    public String getCategory() {
        return category;
    }

    public double getCurrentTotal() {
        return currentTotal;
    }

    public double getBudgetLimit() {
        return budgetLimit;
    }

    public double getAttemptedAmount() {
        return attemptedAmount;
    }

    public double getBreachAmount() {
        return (currentTotal + attemptedAmount) - budgetLimit;
    }
}
