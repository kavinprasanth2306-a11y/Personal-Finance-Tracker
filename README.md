# Personal Finance Tracker

A production-ready, modular Java GUI desktop application engineered for personal financial management, expense tracking, dynamic budgeting, and analytical reporting. This application was built as an Object-Oriented Programming (OOP) Micro-Project explicitly demonstrating mastery of **Course Outcome 2 (OOP Principles)** and **Course Outcome 4 (Advanced Programming Constructs)**.

![Java Version](https://img.shields.io/badge/Java-17+-blue.svg)
![Architecture](https://img.shields.io/badge/Architecture-MVC%20%7C%20DAO%20%7C%20Observer-purple.svg)
![Database](https://img.shields.io/badge/Database-SQLite%20%7C%20MySQL-success.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

---

## 📑 Table of Contents
1. [Project Overview & Architectural Design](#-project-overview--architectural-design)
2. [Course Outcome (CO) Academic Mapping](#-course-outcome-co-academic-mapping)
3. [Phase 1: Database & OOP Core (CO2)](#-phase-1-database--oop-core-co2)
   - [Database Schema & Seed Data](#1-database-schema--seed-data)
   - [Custom Exception Hierarchy](#2-custom-exception-hierarchy)
   - [Interfaces & Contracts](#3-interfaces--contracts)
   - [Domain Model Hierarchy (Inheritance & Polymorphism)](#4-domain-model-hierarchy)
4. [Phase 2: JDBC DAO Layer & Business Logic (CO4)](#-phase-2-jdbc-dao-layer--business-logic-co4)
   - [Database Configuration & Singleton Connection](#1-database-configuration--singleton-connection)
   - [Data Access Object (DAO) Pattern](#2-data-access-object-dao-pattern)
   - [Service Layer & Java Streams Analytics](#3-service-layer--java-streams-analytics)
   - [Observer Pattern Implementation](#4-observer-pattern-implementation)
   - [Java File I/O Reporting](#5-java-file-io-reporting)
5. [Phase 3: Java Swing GUI & Custom Data Visualization (CO4)](#-phase-3-java-swing-gui--custom-data-visualization-co4)
   - [Zero-Dependency Graphics2D ChartPanel](#1-zero-dependency-graphics2d-chartpanel)
   - [FinanceTrackerGUI Tabbed Interface](#2-financetrackergui-tabbed-interface)
   - [Real-Time Visual & Modal Alerts](#3-real-time-visual--modal-alerts)
   - [Application Entry Points](#4-application-entry-points)
6. [Complete Project File Hierarchy](#-complete-project-file-hierarchy)
7. [Step-by-Step Compilation & Execution Guide](#-step-by-step-compilation--execution-guide)
8. [Sample Data & Verification Results](#-sample-data--verification-results)
9. [Academic Evaluation Checklist](#-academic-evaluation-checklist)

---

## 🏛 Project Overview & Architectural Design

The application follows industry-standard **Model-View-Controller (MVC)** and **Data Access Object (DAO)** architectural patterns:
- **Model (`com.financetracker.model`):** Pure encapsulated domain entities modeling financial transactions and category budgets.
- **DAO (`com.financetracker.dao`, `com.financetracker.db`):** Data layer abstracting SQL relational operations through parameterized JDBC queries.
- **Service (`com.financetracker.service`):** Business logic layer running in-memory analytical aggregations via the Java 8+ Collections & Streams API and coordinating observer notifications.
- **View / Controller (`com.financetracker.ui`):** Java Swing presentation layer featuring native 2D data visualizations and event listeners.

```
       [ View: FinanceTrackerGUI ] <====== (Observer Callbacks) ======\
                  |                                                    |
             (User Actions)                                            |
                  v                                                    |
       [ Service: BudgetManager ] ------------------------------------/
                  |
     (Streams & Threshold Logic)
                  v
       [ DAO: FinanceDAO ] <-----> [ Model: Transaction & Subclasses ]
                  |
     (JDBC PreparedStatements)
                  v
     [ SQLite / MySQL Database ]
```

---

## 🎯 Course Outcome (CO) Academic Mapping

| Academic Outcome | Specification | Project Implementation |
| :--- | :--- | :--- |
| **CO2: OOP Principles** | **Encapsulation** | Strict `private` fields across all models. Invariants enforced in setters throwing checked `InvalidTransactionException`. |
| **CO2: OOP Principles** | **Inheritance** | Concrete classes `IncomeTransaction`, `ExpenseTransaction`, and `SavingsTransaction` extend abstract `Transaction`. |
| **CO2: OOP Principles** | **Polymorphism** | Abstract method `getTransactionImpact()` dynamically dispatched at runtime to compute cash flow inflow/outflow. |
| **CO2: OOP Principles** | **Abstraction & Interfaces** | `Reportable` interface decouples formatting and CSV generation; `BudgetAlertListener` decouples threshold monitoring. |
| **CO4: Advanced Constructs** | **Relational JDBC Layer** | `DatabaseConnection` Singleton supporting SQLite and MySQL with `PreparedStatement` and `try-with-resources`. |
| **CO4: Advanced Constructs** | **Streams & Collections** | Category aggregation, percentage breakdowns, and cash totals computed with `Collectors.groupingBy()` and `Collectors.summingDouble()`. |
| **CO4: Advanced Constructs** | **Custom Exception Handling**| Domain exceptions: `BudgetExceededException` (with breach metrics) and `InvalidTransactionException` (with field attribution). |
| **CO4: Advanced Constructs** | **Observer Design Pattern** | `BudgetManager` notifies `FinanceTrackerGUI` upon 80% (Warning) and 100% (Exceeded) budget threshold breaches. |
| **CO4: Advanced Constructs** | **Java File I/O** | `BufferedWriter` / `FileWriter` export structured `.txt` statements and RFC-compliant `.csv` datasets. |
| **CO4: Advanced Constructs** | **Data Visualization** | Custom `Graphics2D` rendering of Bar Chart (Cash Flow) and Donut Chart (Category Distribution) with anti-aliasing. |

---

## 📦 Phase 1: Database & OOP Core (CO2)

Phase 1 establishes the relational foundation, domain model hierarchy, custom exception mechanisms, and operational contracts.

### 1. Database Schema & Seed Data
Location: `src/resources/schema.sql` (compatible with **SQLite** and **MySQL/MariaDB**).

- **`transactions` Table:** Stores all financial entries (`id`, `type`, `amount`, `date`, `category`, `description`).
- **`budgets` Table:** Stores monthly allocated caps per category (`id`, `category`, `monthly_limit`, `month_year`).
- **Seed Data:** Populates 5 baseline monthly budgets and 10 diverse transaction records for instant application evaluation.

### 2. Custom Exception Hierarchy
Location: `com.financetracker.exception`

- **`InvalidTransactionException.java`:** Checked exception thrown when an entity fails validation constraints (e.g., non-positive amount, null date, or empty category). Tracks the offending field name.
- **`BudgetExceededException.java`:** Checked domain exception triggered when an expense pushes category spending past 100% of its budget limit. Stores current spending, limit, attempted amount, and total overspend breach.

### 3. Interfaces & Contracts
Location: `com.financetracker.interfaces`

- **`Reportable.java`:**
  - `String getFormattedSummary()`: Formats entities into structured human-readable text.
  - `String toCsvRow()`: Serializes entities into standard comma-separated format.
- **`BudgetAlertListener.java`:**
  - `void onBudgetWarning(String category, double currentSpending, double limit, double percentage)`
  - `void onBudgetExceeded(String category, double currentSpending, double limit, double percentage)`

### 4. Domain Model Hierarchy
Location: `com.financetracker.model`

- **`TransactionType.java` (Enum):** Type-safe classifications: `INCOME`, `EXPENSE`, and `SAVINGS`.
- **`Transaction.java` (Abstract Base Class):**
  - Encapsulates: `id`, `amount`, `date`, `category`, `description`, `type`.
  - Implements: `Reportable`, `Comparable<Transaction>`.
  - Declares abstract method: `public abstract double getTransactionImpact();`
- **`IncomeTransaction.java`:**
  - Overrides `getTransactionImpact()` returning positive inflow: `+getAmount()`.
- **`ExpenseTransaction.java`:**
  - Overrides `getTransactionImpact()` returning negative outflow: `-getAmount()`.
- **`SavingsTransaction.java`:**
  - Overrides `getTransactionImpact()` returning reserve allocation outflow: `-getAmount()`.
- **`Budget.java`:**
  - Encapsulates category limit, month-year period (`YYYY-MM`), and validation logic.

---

## ⚙️ Phase 2: JDBC DAO Layer & Business Logic (CO4)

Phase 2 implements database communication, business services, stream aggregations, observer pattern notifications, and file export.

### 1. Database Configuration & Singleton Connection
Location: `src/resources/db_config.properties` and `com.financetracker.db.DatabaseConnection.java`

- **Thread-Safe Singleton:** Implements double-checked locking to manage database connections.
- **Dual Engine Compatibility:** Reads `db_config.properties` to connect to embedded SQLite (`finance_tracker.db`) or networked MySQL.
- **Auto-Initialization:** Automatically creates the tables if they do not exist and loads initial seed data if the tables are empty.

### 2. Data Access Object (DAO) Pattern
Location: `com.financetracker.dao.FinanceDAO.java`

- **Transaction CRUD:** `addTransaction()`, `updateTransaction()`, `deleteTransaction()`, `getAllTransactions()`, `getTransactionsByDateRange()`, `getTransactionsByCategory()`.
- **Budget CRUD:** `saveOrUpdateBudget()`, `deleteBudget()`, `deleteBudgetByCategory()`, `getAllBudgets()`, `getBudgetByCategory()`.
- **SQL Aggregations:** `getCategoryExpenseSumForMonth()`, `getCategoryWiseExpensesForMonth()`.
- **Security & Integrity:** All statements use parameterized `PreparedStatement` to prevent SQL injection.
- **Resource Management:** `try-with-resources` ensures automatic closure of Connections, Statements, and ResultSets.
- **Polymorphic Mapping:** Dynamically instantiates the correct concrete model (`IncomeTransaction`, `ExpenseTransaction`, `SavingsTransaction`) based on database rows.

### 3. Service Layer & Java Streams Analytics
Location: `com.financetracker.service.BudgetManager.java`

- **Java 8+ Streams Aggregations:**
  - **Total Income:**
    ```java
    transactions.stream().filter(t -> t.getType() == TransactionType.INCOME).mapToDouble(Transaction::getAmount).sum();
    ```
  - **Total Expenses:**
    ```java
    transactions.stream().filter(t -> t.getType() == TransactionType.EXPENSE).mapToDouble(Transaction::getAmount).sum();
    ```
  - **Total Savings:**
    ```java
    transactions.stream().filter(t -> t.getType() == TransactionType.SAVINGS).mapToDouble(Transaction::getAmount).sum();
    ```
  - **Category Expense Breakdown:**
    ```java
    transactions.stream()
        .filter(t -> t.getType() == TransactionType.EXPENSE)
        .collect(Collectors.groupingBy(Transaction::getCategory, Collectors.summingDouble(Transaction::getAmount)));
    ```
  - **Net Retained Balance:** Operational Cash Flow (`Income - Expenses`) minus Transferred Savings.

### 4. Observer Pattern Implementation
- `BudgetManager` maintains a thread-safe list of `BudgetAlertListener` observers.
- Whenever an expense transaction is added, the category's projected monthly spending is evaluated against the budget:
  - If spending reaches $\ge 80\%$ and $< 100\%$ $\rightarrow$ fires `onBudgetWarning()`.
  - If spending exceeds $\ge 100\%$ $\rightarrow$ fires `onBudgetExceeded()` and throws `BudgetExceededException`.

### 5. Java File I/O Reporting
- **Text Statement Export (`exportReportToTxt`):** Writes an executive summary report with period totals, category share breakdown, and transaction logs using `BufferedWriter`.
- **CSV Data Export (`exportTransactionsToCsv`):** Exports all transactions to RFC-compliant comma-separated format.

---

## 🖥 Phase 3: Java Swing GUI & Custom Data Visualization (CO4)

Phase 3 builds the graphical interface, real-time visual alerts, and zero-dependency 2D graphics renderers.

### 1. Zero-Dependency Graphics2D ChartPanel
Location: `com.financetracker.ui.ChartPanel.java`

- Built entirely with native `java.awt.Graphics2D` (no external charting dependencies).
- Configured with `KEY_ANTIALIASING`, `KEY_TEXT_ANTIALIASING`, and `KEY_RENDERING` quality hints.
- **Card 1 (Cash Flow Bar Chart):** Compares Income, Expenses, and Savings Reserve using rounded bar gradients, value labels, and dynamic axis scaling.
- **Card 2 (Expense Donut Chart):** Draws proportional category slices, an inner white cutout circle displaying total expenses, and an interactive legend with color indicators.

### 2. FinanceTrackerGUI Tabbed Interface
Location: `com.financetracker.ui.FinanceTrackerGUI.java`

- **Dashboard & Analytics Tab:**
  - 4 KPI cards: Total Income (green), Total Expenses (red), Savings Reserve (blue), Net Balance (violet).
  - Embedded `ChartPanel` that redraws automatically when transactions change.
- **Manage Transactions Tab:**
  - Form: Type, Amount, Category, Date, and Description with input validation.
  - CRUD Buttons: Add Transaction, Update Selected, Delete Selected, Clear Form.
  - Interactive `JTable` with custom `CurrencyCellRenderer` and `ImpactCellRenderer` (`+` green, `-` red).
- **Category Budgets Tab:**
  - Budget configuration form and table showing category caps and month-year periods.
- **Reports & File Export Tab:**
  - Date Range pickers (`From` and `To`).
  - Monospaced report preview pane.
  - File choosers (`JFileChooser`) to export to `.txt` and `.csv`.

### 3. Real-Time Visual & Modal Alerts
`FinanceTrackerGUI` implements `BudgetAlertListener`:
- **Top Status Banner:**
  - **Healthy (Green):** `[STATUS OK] All category spendings are within allocated budget limits.`
  - **Warning (Amber):** `[BUDGET WARNING] Category 'Food & Dining' spending is at 87.6% (₹525.50 / ₹600.00)!`
  - **Exceeded (Crimson):** `[CRITICAL ALERT] Budget EXCEEDED for 'Food & Dining' by ₹45.00 (Spending: 107.5%)!`
- **Modal Dialogs:** Immediate `JOptionPane.showMessageDialog` with warning/error icons and an audible system chime.

### 4. Application Entry Points
Location: `com.financetracker.Main.java` and `com.financetracker.MainApp.java`
- Configures Nimbus / System native Look & Feel.
- Instantiates `BudgetManager` and `FinanceTrackerGUI`.
- Dispatches UI initialization safely on the Swing Event Dispatch Thread (`SwingUtilities.invokeLater`).

---

## 📂 Complete Project File Hierarchy

```text
Personal-Finance-Tracker/
├── lib/
│   └── sqlite-jdbc.jar                  # Standalone SQLite JDBC driver
├── bin/                                 # Compiled bytecode (.class)
│   ├── com/financetracker/
│   │   ├── dao/
│   │   │   └── FinanceDAO.class
│   │   ├── db/
│   │   │   └── DatabaseConnection.class
│   │   ├── exception/
│   │   │   ├── BudgetExceededException.class
│   │   │   └── InvalidTransactionException.class
│   │   ├── interfaces/
│   │   │   ├── BudgetAlertListener.class
│   │   │   └── Reportable.class
│   │   ├── model/
│   │   │   ├── Budget.class
│   │   │   ├── ExpenseTransaction.class
│   │   │   ├── IncomeTransaction.class
│   │   │   ├── SavingsTransaction.class
│   │   │   ├── Transaction.class
│   │   │   └── TransactionType.class
│   │   ├── service/
│   │   │   └── BudgetManager.class
│   │   ├── ui/
│   │   │   ├── ChartPanel.class
│   │   │   └── FinanceTrackerGUI.class
│   │   ├── Main.class
│   │   └── MainApp.class
│   └── resources/
│       ├── db_config.properties
│       └── schema.sql
├── src/
│   ├── com/financetracker/
│   │   ├── dao/FinanceDAO.java
│   │   ├── db/DatabaseConnection.java
│   │   ├── exception/BudgetExceededException.java
│   │   ├── exception/InvalidTransactionException.java
│   │   ├── interfaces/BudgetAlertListener.java
│   │   ├── interfaces/Reportable.java
│   │   ├── model/Budget.java
│   │   ├── model/ExpenseTransaction.java
│   │   ├── model/IncomeTransaction.java
│   │   ├── model/SavingsTransaction.java
│   │   ├── model/Transaction.java
│   │   ├── model/TransactionType.java
│   │   ├── service/BudgetManager.java
│   │   ├── ui/ChartPanel.java
│   │   ├── ui/FinanceTrackerGUI.java
│   │   ├── Main.java
│   │   └── MainApp.java
│   └── resources/
│       ├── db_config.properties
│       └── schema.sql
├── .gitignore
└── README.md
```

---

## 🛠 Step-by-Step Compilation & Execution Guide

### Step 1: Verify Prerequisites
Ensure Java JDK 17 or higher is installed:
```powershell
java -version
javac -version
```

### Step 2: Compile All Sources
Open PowerShell or Command Prompt in the project root directory (`e:\projects\java`) and run:

**Using System `javac`:**
```powershell
javac -encoding UTF-8 -cp "lib/sqlite-jdbc.jar" -d bin (Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName })
```

*(Or on standard Command Prompt / Bash:)*
```bash
javac -encoding UTF-8 -cp "lib/sqlite-jdbc.jar" -d bin src/com/financetracker/*.java src/com/financetracker/*/*.java
```

### Step 3: Copy Configuration Resources
Ensure resource files are present in the output folder:
```powershell
if (!(Test-Path bin/resources)) { New-Item -ItemType Directory -Path bin/resources }; Copy-Item -Path src/resources/* -Destination bin/resources/ -Force
```

### Step 4: Run the Application
Launch the GUI with the SQLite driver included on the classpath:

**Using PowerShell / CMD:**
```powershell
java -cp "bin;lib/sqlite-jdbc.jar" com.financetracker.Main
```

*(Or via the alias entry point:)*
```powershell
java -cp "bin;lib/sqlite-jdbc.jar" com.financetracker.MainApp
```

---

## 📊 Sample Data & Verification Results

On initial execution, the database auto-seeds sample transactions and budgets. In-memory analytics and Stream aggregations produce the following initial financial profile:

```text
================================================================================
                 PERSONAL FINANCE TRACKER - FINANCIAL REPORT                    
================================================================================
Report Period:      Oct 01, 2026 to Oct 31, 2026
Generated On:       Oct 27, 2026
Total Transactions: 10
--------------------------------------------------------------------------------
EXECUTIVE SUMMARY
--------------------------------------------------------------------------------
  (+) Total Inflow (Income):        ₹     4850.00
  (-) Total Outflow (Expenses):     ₹     1985.50
  (=) Operational Cash Flow:        ₹     2864.50
  (-) Total Transferred to Savings: ₹      750.00
  (=) Net Retained Balance:         ₹     2114.50
--------------------------------------------------------------------------------
CATEGORY-WISE EXPENSE BREAKDOWN (STREAM AGGREGATIONS)
--------------------------------------------------------------------------------
  Category                  Amount (₹)      Share (%) 
  --------------------------------------------------------------
  Rent & Housing            ₹     1200.00      60.4%
  Food & Dining             ₹      525.50      26.5%
  Entertainment             ₹      110.00       5.5%
  Utilities                 ₹       85.00       4.3%
  Transportation            ₹       65.00       3.3%
--------------------------------------------------------------------------------
TRANSACTION DETAIL LOG
--------------------------------------------------------------------------------
  [EXPENSE] ID: 8     | Date: 2026-10-18 | Type: Expense  | Cat: Food & Dining   | Amt: ₹    380.00 | Note: Family weekend gathering & restaurants
  [EXPENSE] ID: 7     | Date: 2026-10-15 | Type: Expense  | Cat: Entertainment   | Amt: ₹    110.00 | Note: Concert tickets and weekend dinner
  [EXPENSE] ID: 6     | Date: 2026-10-12 | Type: Expense  | Cat: Transportation  | Amt: ₹     65.00 | Note: Gasoline refill and metro smartcard top-up
  [SAVINGS] ID: 10    | Date: 2026-10-10 | Type: Savings  | Cat: Index Funds     | Amt: ₹    250.00 | Note: Automated recurring ETF investment
  [EXPENSE] ID: 5     | Date: 2026-10-08 | Type: Expense  | Cat: Utilities       | Amt: ₹     85.00 | Note: High-speed fiber internet and electric bill
  [INCOME] ID: 2     | Date: 2026-10-05 | Type: Income   | Cat: Investments     | Amt: ₹    350.00 | Note: Dividend yield payout Q3
  [EXPENSE] ID: 4     | Date: 2026-10-04 | Type: Expense  | Cat: Food & Dining   | Amt: ₹    145.50 | Note: Supermarket monthly groceries restocking
  [SAVINGS] ID: 9     | Date: 2026-10-03 | Type: Savings  | Cat: Emergency Fund  | Amt: ₹    500.00 | Note: Transferred to high-yield savings account
  [EXPENSE] ID: 3     | Date: 2026-10-02 | Type: Expense  | Cat: Rent & Housing  | Amt: ₹   1200.00 | Note: Apartment monthly lease payment
  [INCOME] ID: 1     | Date: 2026-10-01 | Type: Income   | Cat: Salary          | Amt: ₹   4500.00 | Note: Monthly corporate engineering salary
================================================================================
```

---

## ✅ Academic Evaluation Checklist

- [x] **CO2 Encapsulation:** Private variables with validated setters in `Transaction` and `Budget`.
- [x] **CO2 Inheritance:** `IncomeTransaction`, `ExpenseTransaction`, `SavingsTransaction` extend `Transaction`.
- [x] **CO2 Polymorphism:** `getTransactionImpact()` returns `+amount` or `-amount` polymorphically.
- [x] **CO2 Abstraction & Interfaces:** `Reportable` and `BudgetAlertListener` contracts implemented.
- [x] **CO2 Design Patterns:** MVC architecture, DAO repository, Thread-safe Singleton.
- [x] **CO4 JDBC Layer:** SQLite & MySQL dual support with `PreparedStatement` and `try-with-resources`.
- [x] **CO4 Custom Exceptions:** `BudgetExceededException` and `InvalidTransactionException`.
- [x] **CO4 Java Collections & Streams:** `Collectors.groupingBy()`, `Collectors.summingDouble()`, filtering, and sorting.
- [x] **CO4 Observer Pattern:** Decoupled threshold listeners updating UI banner and triggering modals.
- [x] **CO4 Java File I/O:** Export formatted summaries to `.txt` and tabular data to `.csv`.
- [x] **CO4 Data Visualization:** Native `Graphics2D` Bar and Donut charts with anti-aliasing.

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
 
 
#   P e r s o n a l - F i n a n c e - T r a c k e r  
 