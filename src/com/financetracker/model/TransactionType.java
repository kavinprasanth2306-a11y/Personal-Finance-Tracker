package com.financetracker.model;

/**
 * Enumeration representing the valid classifications of financial transactions.
 * Demonstrates type-safety and eliminates invalid category classifications.
 */
public enum TransactionType {
    INCOME("Income"),
    EXPENSE("Expense"),
    SAVINGS("Savings");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TransactionType fromString(String text) {
        for (TransactionType type : TransactionType.values()) {
            if (type.name().equalsIgnoreCase(text) || type.displayName.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + text);
    }
}
