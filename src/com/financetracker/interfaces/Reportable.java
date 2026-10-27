package com.financetracker.interfaces;

/**
 * Interface defining contract for objects capable of generating formatted
 * financial reports or exports (CSV / Plain Text).
 * Demonstrates CO2 Abstraction and Interface design.
 */
public interface Reportable {
    
    /**
     * Formats the entity into a structured, human-readable summary block.
     * @return Formatted multi-line or single-line representation.
     */
    String getFormattedSummary();

    /**
     * Converts the entity data into a standard comma-separated values (CSV) row.
     * @return CSV formatted string row.
     */
    String toCsvRow();
}
