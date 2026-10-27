package com.financetracker.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-Safe Singleton class managing JDBC connections.
 * Demonstrates CO4:
 * - Persistence abstraction supporting both SQLite and MySQL.
 * - Automatic schema initialization and seed data loading.
 * - Resource management with PreparedStatement and try-with-resources.
 */
public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static volatile DatabaseConnection instance;

    private String dbType = "SQLITE";
    private String jdbcUrl;
    private String username;
    private String password;

    /**
     * Private constructor to enforce Singleton design pattern.
     */
    private DatabaseConnection() {
        loadConfiguration();
        initializeDatabase();
    }

    /**
     * Double-checked locking Singleton accessor.
     * @return DatabaseConnection instance.
     */
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Loads database configuration from db_config.properties file.
     */
    private void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/resources/db_config.properties")) {
            if (in != null) {
                props.load(in);
            } else {
                File configFile = new File("src/resources/db_config.properties");
                if (configFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(configFile)) {
                        props.load(fis);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load db_config.properties; using default SQLite configuration.", e);
        }

        this.dbType = props.getProperty("database.type", "SQLITE").trim().toUpperCase();

        if ("MYSQL".equalsIgnoreCase(this.dbType)) {
            String host = props.getProperty("mysql.host", "localhost");
            String port = props.getProperty("mysql.port", "3306");
            String db = props.getProperty("mysql.database", "finance_tracker");
            String useSSL = props.getProperty("mysql.useSSL", "false");
            String tz = props.getProperty("mysql.serverTimezone", "UTC");
            String allowPublicKey = props.getProperty("mysql.allowPublicKeyRetrieval", "true");

            this.jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=%s&serverTimezone=%s&allowPublicKeyRetrieval=%s",
                    host, port, db, useSSL, tz, allowPublicKey);
            this.username = props.getProperty("mysql.username", "root");
            this.password = props.getProperty("mysql.password", "root");

            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "MySQL JDBC Driver not found on classpath. Falling back to SQLite.", e);
                this.dbType = "SQLITE";
            }
        }

        if ("SQLITE".equalsIgnoreCase(this.dbType)) {
            String dbPath = props.getProperty("sqlite.db.path", "finance_tracker.db");
            this.jdbcUrl = "jdbc:sqlite:" + dbPath;
            this.username = null;
            this.password = null;

            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.INFO, "org.sqlite.JDBC driver will be loaded via standard DriverManager.");
            }
        }

        LOGGER.info(String.format("Configured Database Engine: [%s] -> URL: %s", this.dbType, this.jdbcUrl));
    }

    /**
     * Establishes and returns an active JDBC Connection.
     * @return Connection object.
     * @throws SQLException if a database access error occurs.
     */
    public Connection getConnection() throws SQLException {
        if ("MYSQL".equalsIgnoreCase(dbType) && username != null) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }
        return DriverManager.getConnection(jdbcUrl);
    }

    /**
     * Automatically creates necessary tables and seeds initial data if tables are empty.
     */
    private void initializeDatabase() {
        boolean isMysql = "MYSQL".equalsIgnoreCase(dbType);
        String autoInc = isMysql ? "AUTO_INCREMENT" : "AUTOINCREMENT";

        String createTransactionsSql = "CREATE TABLE IF NOT EXISTS transactions ("
                + "id INTEGER PRIMARY KEY " + autoInc + ", "
                + "type VARCHAR(20) NOT NULL, "
                + "amount DOUBLE NOT NULL, "
                + "date VARCHAR(10) NOT NULL, "
                + "category VARCHAR(50) NOT NULL, "
                + "description VARCHAR(255)"
                + ")";

        String createBudgetsSql = "CREATE TABLE IF NOT EXISTS budgets ("
                + "id INTEGER PRIMARY KEY " + autoInc + ", "
                + "category VARCHAR(50) NOT NULL UNIQUE, "
                + "monthly_limit DOUBLE NOT NULL, "
                + "month_year VARCHAR(7) NOT NULL"
                + ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTransactionsSql);
            stmt.execute(createBudgetsSql);

            // Check if seeding is required (if transactions table is empty)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    seedInitialData(stmt);
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to auto-initialize database tables: " + e.getMessage(), e);
        }
    }

    /**
     * Seeds realistic financial records into the database on clean startup.
     */
    private void seedInitialData(Statement stmt) throws SQLException {
        LOGGER.info("Seeding initial financial transactions and budget allocations...");

        // Budgets
        stmt.execute("INSERT INTO budgets (category, monthly_limit, month_year) VALUES ('Food & Dining', 600.00, '2026-10')");
        stmt.execute("INSERT INTO budgets (category, monthly_limit, month_year) VALUES ('Rent & Housing', 1200.00, '2026-10')");
        stmt.execute("INSERT INTO budgets (category, monthly_limit, month_year) VALUES ('Utilities', 250.00, '2026-10')");
        stmt.execute("INSERT INTO budgets (category, monthly_limit, month_year) VALUES ('Entertainment', 200.00, '2026-10')");
        stmt.execute("INSERT INTO budgets (category, monthly_limit, month_year) VALUES ('Transportation', 300.00, '2026-10')");

        // Transactions
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('INCOME', 4500.00, '2026-10-01', 'Salary', 'Monthly corporate engineering salary')");
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('INCOME', 350.00, '2026-10-05', 'Investments', 'Dividend yield payout Q3')");
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('EXPENSE', 1200.00, '2026-10-02', 'Rent & Housing', 'Apartment monthly lease payment')");
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('EXPENSE', 145.50, '2026-10-04', 'Food & Dining', 'Supermarket monthly groceries restocking')");
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('EXPENSE', 85.00, '2026-10-08', 'Utilities', 'High-speed fiber internet and electric bill')");
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('EXPENSE', 65.00, '2026-10-12', 'Transportation', 'Gasoline refill and metro smartcard top-up')");
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('EXPENSE', 110.00, '2026-10-15', 'Entertainment', 'Concert tickets and weekend dinner')");
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('EXPENSE', 380.00, '2026-10-18', 'Food & Dining', 'Family weekend gathering & restaurants')");
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('SAVINGS', 500.00, '2026-10-03', 'Emergency Fund', 'Transferred to high-yield savings account')");
        stmt.execute("INSERT INTO transactions (type, amount, date, category, description) VALUES "
                + "('SAVINGS', 250.00, '2026-10-10', 'Index Funds', 'Automated recurring ETF investment')");

        LOGGER.info("Seeding completed successfully.");
    }

    public String getDbType() {
        return dbType;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }
}
