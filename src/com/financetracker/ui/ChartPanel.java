package com.financetracker.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Custom High-Performance Swing Component rendering native Graphics2D data visualizations.
 * Displays:
 * 1. Income vs Expense vs Savings Comparative Bar Chart.
 * 2. Category-Wise Expense Distribution Donut/Pie Chart with Legends.
 * 
 * Satisfies CO4 Data Visualization (zero external library dependency).
 */
public class ChartPanel extends JPanel {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    // Harmonious modern visual palette
    private static final Color BG_DARK = new Color(248, 250, 252);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    // KPI Bar Colors
    private static final Color COLOR_INCOME = new Color(16, 185, 129);   // Emerald Green
    private static final Color COLOR_EXPENSE = new Color(239, 68, 68);   // Vibrant Coral/Red
    private static final Color COLOR_SAVINGS = new Color(59, 130, 246);  // Royal Blue

    // Donut Category Palette
    private static final Color[] SLICE_COLORS = {
            new Color(99, 102, 241),  // Indigo
            new Color(245, 158, 11),  // Amber
            new Color(236, 72, 153),  // Pink
            new Color(20, 184, 166),  // Teal
            new Color(139, 92, 246),  // Violet
            new Color(249, 115, 22),  // Orange
            new Color(14, 165, 233),  // Sky Blue
            new Color(168, 85, 247)   // Purple
    };

    private double totalIncome = 0.0;
    private double totalExpense = 0.0;
    private double totalSavings = 0.0;
    private Map<String, Double> categoryExpenses = Map.of();

    public ChartPanel() {
        setBackground(BG_DARK);
        setPreferredSize(new Dimension(860, 360));
        setMinimumSize(new Dimension(600, 320));
    }

    /**
     * Updates chart dataset and triggers visual repaint.
     */
    public void updateData(double income, double expense, double savings, Map<String, Double> categoryExpenses) {
        this.totalIncome = Math.max(0, income);
        this.totalExpense = Math.max(0, expense);
        this.totalSavings = Math.max(0, savings);
        this.categoryExpenses = categoryExpenses != null ? categoryExpenses : Map.of();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();

        // Enable ultra-smooth anti-aliasing for shapes and text
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();
        int margin = 16;
        int gap = 16;

        int halfWidth = (width - (margin * 2) - gap) / 2;
        int cardHeight = height - (margin * 2);

        // Card 1: Comparative Bar Chart (Left)
        drawBarChartCard(g2, margin, margin, halfWidth, cardHeight);

        // Card 2: Expense Donut Chart (Right)
        drawDonutChartCard(g2, margin + halfWidth + gap, margin, halfWidth, cardHeight);

        g2.dispose();
    }

    /**
     * Renders modern comparative bar chart (Income vs Expense vs Savings).
     */
    private void drawBarChartCard(Graphics2D g2, int x, int y, int w, int h) {
        drawRoundedCard(g2, x, y, w, h);

        // Header Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2.setColor(TEXT_PRIMARY);
        g2.drawString("Cash Flow Comparison", x + 20, y + 28);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(TEXT_MUTED);
        g2.drawString("Income vs. Expenses vs. Savings Reserve", x + 20, y + 46);

        int chartTop = y + 70;
        int chartBottom = y + h - 50;
        int chartHeight = chartBottom - chartTop;

        double maxVal = Math.max(Math.max(totalIncome, totalExpense), Math.max(totalSavings, 100.0));
        // Add 15% headroom for aesthetic labels
        maxVal *= 1.15;

        // Baseline Axis
        g2.setColor(BORDER_COLOR);
        g2.drawLine(x + 30, chartBottom, x + w - 30, chartBottom);

        int barWidth = Math.min(54, (w - 120) / 3);
        int spacing = ((w - 60) - (barWidth * 3)) / 4;

        // Bar 1: Income
        int x1 = x + 30 + spacing;
        drawSingleBar(g2, x1, chartBottom, barWidth, chartHeight, totalIncome, maxVal, COLOR_INCOME, "Income");

        // Bar 2: Expense
        int x2 = x1 + barWidth + spacing;
        drawSingleBar(g2, x2, chartBottom, barWidth, chartHeight, totalExpense, maxVal, COLOR_EXPENSE, "Expense");

        // Bar 3: Savings
        int x3 = x2 + barWidth + spacing;
        drawSingleBar(g2, x3, chartBottom, barWidth, chartHeight, totalSavings, maxVal, COLOR_SAVINGS, "Savings");
    }

    private void drawSingleBar(Graphics2D g2, int x, int baselineY, int width, int totalHeight,
                               double value, double maxValue, Color color, String label) {
        int barHeight = (int) Math.round((value / maxValue) * totalHeight);
        barHeight = Math.max(barHeight, value > 0 ? 4 : 0);
        int barY = baselineY - barHeight;

        // Bar Gradient
        GradientPaint gp = new GradientPaint(x, barY, color, x, baselineY, color.darker());
        g2.setPaint(gp);
        g2.fill(new RoundRectangle2D.Float(x, barY, width, barHeight, 8, 8));

        // Value text above bar
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g2.setColor(TEXT_PRIMARY);
        String valStr = CURRENCY_FORMAT.format(value);
        FontMetrics fm = g2.getFontMetrics();
        int valX = x + (width - fm.stringWidth(valStr)) / 2;
        g2.drawString(valStr, valX, Math.max(barY - 6, baselineY - totalHeight + 12));

        // Category label below axis
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(TEXT_MUTED);
        int lblX = x + (width - fm.stringWidth(label)) / 2;
        g2.drawString(label, lblX, baselineY + 22);
    }

    /**
     * Renders modern Donut/Pie chart representing category spending breakdown.
     */
    private void drawDonutChartCard(Graphics2D g2, int x, int y, int w, int h) {
        drawRoundedCard(g2, x, y, w, h);

        // Header Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2.setColor(TEXT_PRIMARY);
        g2.drawString("Expense Breakdown", x + 20, y + 28);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(TEXT_MUTED);
        g2.drawString("Category-wise spending distribution", x + 20, y + 46);

        if (categoryExpenses.isEmpty() || totalExpense <= 0) {
            g2.setColor(TEXT_MUTED);
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            String msg = "No expense data recorded yet.";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(msg, x + (w - fm.stringWidth(msg)) / 2, y + (h / 2));
            return;
        }

        // Layout: Donut on Left, Legend on Right
        int donutDiameter = Math.min(180, Math.min(w / 2, h - 90));
        int donutX = x + 24;
        int donutY = y + 70 + (h - 90 - donutDiameter) / 2;

        double startAngle = 90.0;
        int colorIdx = 0;

        for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
            double sliceAngle = (entry.getValue() / totalExpense) * 360.0;
            Color sliceColor = SLICE_COLORS[colorIdx % SLICE_COLORS.length];

            g2.setColor(sliceColor);
            g2.fill(new Arc2D.Double(donutX, donutY, donutDiameter, donutDiameter, startAngle, sliceAngle, Arc2D.PIE));

            startAngle += sliceAngle;
            colorIdx++;
        }

        // Donut Hole (Inner circle)
        int holeDiameter = (int) (donutDiameter * 0.58);
        int holeX = donutX + (donutDiameter - holeDiameter) / 2;
        int holeY = donutY + (donutDiameter - holeDiameter) / 2;
        g2.setColor(CARD_BG);
        g2.fillOval(holeX, holeY, holeDiameter, holeDiameter);

        // Total amount in donut center
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(TEXT_PRIMARY);
        String centerVal = CURRENCY_FORMAT.format(totalExpense);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(centerVal, holeX + (holeDiameter - fm.stringWidth(centerVal)) / 2, holeY + (holeDiameter / 2) + 5);

        // Legend Rendering (Right side of card)
        int legendX = donutX + donutDiameter + 24;
        int legendY = y + 75;
        int legendMaxY = y + h - 15;

        colorIdx = 0;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
            if (legendY + 18 > legendMaxY) break; // prevent overflow

            Color sliceColor = SLICE_COLORS[colorIdx % SLICE_COLORS.length];
            double pct = (entry.getValue() / totalExpense) * 100.0;

            // Color indicator pill
            g2.setColor(sliceColor);
            g2.fillRoundRect(legendX, legendY - 9, 12, 12, 4, 4);

            // Category & Percentage text
            g2.setColor(TEXT_PRIMARY);
            String label = String.format("%s: %.1f%% (%s)",
                    entry.getKey(), pct, CURRENCY_FORMAT.format(entry.getValue()));
            g2.drawString(label, legendX + 20, legendY);

            legendY += 22;
            colorIdx++;
        }
    }

    private void drawRoundedCard(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(CARD_BG);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 14, 14));
        g2.setColor(BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Float(x, y, w, h, 14, 14));
    }
}
