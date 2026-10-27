-- =====================================================================
-- Personal Finance Tracker - Database Schema (schema.sql)
-- Compatible with SQLite and MySQL / MariaDB
-- =====================================================================

-- 1. Create Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE', 'SAVINGS')),
    amount DOUBLE NOT NULL CHECK (amount > 0),
    date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255)
);

-- 2. Create Budgets Table
CREATE TABLE IF NOT EXISTS budgets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category VARCHAR(50) NOT NULL UNIQUE,
    monthly_limit DOUBLE NOT NULL CHECK (monthly_limit > 0),
    month_year VARCHAR(7) NOT NULL -- Format: YYYY-MM
);

-- 3. Seed Initial Sample Data for Immediate Evaluation
-- Note: Replace existing to avoid duplicate primary key errors during repeated runs
DELETE FROM transactions;
DELETE FROM budgets;

-- Sample Budgets
INSERT INTO budgets (category, monthly_limit, month_year) VALUES 
('Food & Dining', 600.00, '2026-10'),
('Rent & Housing', 1200.00, '2026-10'),
('Utilities', 250.00, '2026-10'),
('Entertainment', 200.00, '2026-10'),
('Transportation', 300.00, '2026-10');

-- Sample Transactions (Income, Expense, Savings)
INSERT INTO transactions (type, amount, date, category, description) VALUES
('INCOME', 4500.00, '2026-10-01', 'Salary', 'Monthly corporate engineering salary'),
('INCOME', 350.00, '2026-10-05', 'Investments', 'Dividend yield payout Q3'),
('EXPENSE', 1200.00, '2026-10-02', 'Rent & Housing', 'Apartment monthly lease payment'),
('EXPENSE', 145.50, '2026-10-04', 'Food & Dining', 'Supermarket monthly groceries restocking'),
('EXPENSE', 85.00, '2026-10-08', 'Utilities', 'High-speed fiber internet and electric bill'),
('EXPENSE', 65.00, '2026-10-12', 'Transportation', 'Gasoline refill and metro smartcard top-up'),
('EXPENSE', 110.00, '2026-10-15', 'Entertainment', 'Concert tickets and weekend dinner'),
('EXPENSE', 380.00, '2026-10-18', 'Food & Dining', 'Family weekend gathering & restaurants'),
('SAVINGS', 500.00, '2026-10-03', 'Emergency Fund', 'Transferred to high-yield savings account'),
('SAVINGS', 250.00, '2026-10-10', 'Index Funds', 'Automated recurring ETF investment');
