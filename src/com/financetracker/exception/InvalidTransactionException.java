package com.financetracker.exception;

/**
 * Custom exception thrown when a transaction fails validation constraints
 * (e.g., negative or zero amount, null date, or empty category).
 * Demonstrates CO4 custom exception handling.
 */
public class InvalidTransactionException extends Exception {
    private final String invalidField;

    public InvalidTransactionException(String message) {
        super(message);
        this.invalidField = "UNKNOWN";
    }

    public InvalidTransactionException(String invalidField, String message) {
        super(String.format("Validation failed for field [%s]: %s", invalidField, message));
        this.invalidField = invalidField;
    }

    public String getInvalidField() {
        return invalidField;
    }
}
