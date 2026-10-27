package com.financetracker.interfaces;

/**
 * Observer/Listener interface for real-time budget notifications.
 * Implements the Observer pattern (CO4) to decouple business calculation
 * from UI alerting (JOptionPanes / status indicators).
 */
public interface BudgetAlertListener {

    /**
     * Fired when spending reaches or exceeds warning threshold (e.g. 80% of limit).
     *
     * @param category        The spending category triggering the alert.
     * @param currentSpending The total spent in this category to date.
     * @param limit           The configured budget limit.
     * @param percentage      The percentage ratio (e.g., 85.5%).
     */
    void onBudgetWarning(String category, double currentSpending, double limit, double percentage);

    /**
     * Fired when spending breaches 100% of the allocated budget limit.
     *
     * @param category        The spending category breached.
     * @param currentSpending The total spent in this category to date.
     * @param limit           The configured budget limit.
     * @param percentage      The percentage ratio (e.g., 108.0%).
     */
    void onBudgetExceeded(String category, double currentSpending, double limit, double percentage);
}
