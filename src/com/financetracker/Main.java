package com.financetracker;

import com.financetracker.service.BudgetManager;
import com.financetracker.ui.FinanceTrackerGUI;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application entry point for the Personal Finance Tracker micro-project.
 * Initializes the service layer, configures modern Look & Feel, and renders the GUI.
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        // Configure Modern Look & Feel (Nimbus or System native)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equalsIgnoreCase(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to apply system Look and Feel", ex);
            }
        }

        // Initialize Services & Launch GUI on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                BudgetManager budgetManager = new BudgetManager();
                FinanceTrackerGUI gui = new FinanceTrackerGUI(budgetManager);
                gui.setVisible(true);
                LOGGER.info("Personal Finance Tracker application launched successfully.");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fatal error launching Personal Finance Tracker GUI", e);
                JOptionPane.showMessageDialog(null,
                        "Fatal initialization error: " + e.getMessage() + "\nSee console logs for details.",
                        "Startup Failure", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
