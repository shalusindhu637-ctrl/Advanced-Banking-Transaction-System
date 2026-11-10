package com.banking.dao;

import com.banking.model.Customer;
import com.banking.model.Transaction;
import com.banking.util.DBConnection;
import com.banking.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomerDAO {
    private static final Logger LOG = Logger.getLogger(CustomerDAO.class.getName());
    
    private String generateAccountNumber() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder("ACC");
        for (int i = 0; i < 10; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
    
    public static class RegistrationResult {
        private final boolean success;
        private final String errorCode;
        private final String userMessage;
        public RegistrationResult(boolean success, String errorCode, String userMessage) {
            this.success = success; this.errorCode = errorCode; this.userMessage = userMessage;
        }
        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
        public String getUserMessage() { return userMessage; }
    }

    public RegistrationResult registerCustomerDetailed(Customer customer) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String accountNumber = generateAccountNumber();
            while (isAccountNumberExists(accountNumber, conn)) {
                accountNumber = generateAccountNumber();
            }

            String customerQuery = "INSERT INTO customers (account_number, username, name, email, phone, address, password_hash) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement psCustomer = conn.prepareStatement(customerQuery, Statement.RETURN_GENERATED_KEYS);
            psCustomer.setString(1, accountNumber);
            psCustomer.setString(2, customer.getUsername());
            psCustomer.setString(3, customer.getName());
            psCustomer.setString(4, customer.getEmail());
            psCustomer.setString(5, customer.getPhone());
            psCustomer.setString(6, customer.getAddress());
            psCustomer.setString(7, PasswordUtil.hashPassword(customer.getPasswordHash()));
            psCustomer.executeUpdate();

            ResultSet rs = psCustomer.getGeneratedKeys();
            int customerId = 0;
            if (rs.next()) {
                customerId = rs.getInt(1);
            }
            rs.close();
            psCustomer.close();

            String accountQuery = "INSERT INTO accounts (customer_id, balance) VALUES (?, 0.00)";
            PreparedStatement psAccount = conn.prepareStatement(accountQuery);
            psAccount.setInt(1, customerId);
            psAccount.executeUpdate();
            psAccount.close();

            conn.commit();
            return new RegistrationResult(true, null, null);
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOG.log(Level.WARNING, "Rollback failed during registration", ex);
                }
            }
            LOG.log(Level.WARNING, "Registration SQL failure (SQLState=" + e.getSQLState() + ", code=" + e.getErrorCode()
                    + ", op=registerCustomer, user=" + safeId(customer == null ? null : customer.getUsername()) + ")", e);
            return mapRegistrationSqlError(e);
        } catch (RuntimeException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { LOG.log(Level.WARNING, "Rollback failed during registration", ex); }
            }
            LOG.log(Level.SEVERE, "Registration unexpected failure (op=registerCustomer)", e);
            return new RegistrationResult(false, "APP_ERROR", "Registration service unavailable. Please try again later.");
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOG.log(Level.FINE, "Connection close failed", e);
                }
            }
        }
    }

    /** Legacy API kept for compatibility; prefer registerCustomerDetailed for diagnostics. */
    public boolean registerCustomer(Customer customer) {
        return registerCustomerDetailed(customer).isSuccess();
    }

    private static RegistrationResult mapRegistrationSqlError(SQLException e) {
        String state = e.getSQLState();
        int code = e.getErrorCode();
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (code == 1045 || "28000".equals(state)) {
            return new RegistrationResult(false, "DB_AUTH",
                    "Registration service unavailable (database authentication). Please contact support.");
        }
        if (code == 0 && (msg.contains("communications link failure") || msg.contains("connection refused") || msg.contains("unknown database"))) {
            return new RegistrationResult(false, "DB_CONNECT",
                    "Registration service unavailable (database connection). Please try again later.");
        }
        if (code == 1062 || "23000".equals(state)) {
            if (msg.contains("username")) return new RegistrationResult(false, "DUP_USERNAME", "Username already taken!");
            if (msg.contains("email")) return new RegistrationResult(false, "DUP_EMAIL", "Email already registered!");
            if (msg.contains("account_number")) return new RegistrationResult(false, "DUP_ACCOUNT", "Registration failed. Please try again.");
            return new RegistrationResult(false, "DUPLICATE", "A record with the same details already exists.");
        }
        if (code == 1054 || (msg.contains("unknown column") && msg.contains("username"))) {
            return new RegistrationResult(false, "SCHEMA",
                    "Registration service unavailable (database schema). Please contact support.");
        }
        return new RegistrationResult(false, "DB_ERROR", "Registration service unavailable. Please try again later.");
    }

    private static String safeId(String v) {
        if (v == null) return "null";
        String t = v.trim();
        if (t.length() > 64) t = t.substring(0, 64);
        return t.replaceAll("[^A-Za-z0-9@._\\-]", "_");
    }
    
    private boolean isAccountNumberExists(String accountNumber, Connection conn) throws SQLException {
        String query = "SELECT account_number FROM customers WHERE account_number = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, accountNumber);
        ResultSet rs = ps.executeQuery();
        boolean exists = rs.next();
        rs.close();
        ps.close();
        return exists;
    }
    
    public boolean isEmailExists(String email) throws SQLException {
        String query = "SELECT email FROM customers WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    public boolean isUsernameExists(String username) throws SQLException {
        String query = "SELECT username FROM customers WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    public static class AuthResult {
        private final Customer customer;
        private final String errorCode;
        private final String message;

        private AuthResult(Customer customer, String errorCode, String message) {
            this.customer = customer;
            this.errorCode = errorCode;
            this.message = message;
        }

        public static AuthResult ok(Customer customer) { return new AuthResult(customer, null, null); }
        public static AuthResult error(String code, String message) { return new AuthResult(null, code, message); }
        public Customer getCustomer() { return customer; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public boolean isSuccess() { return customer != null; }
    }

    public AuthResult authenticate(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || password == null) {
            return AuthResult.error("INVALID", "Invalid username/email or password.");
        }
        String safeUser = safeId(usernameOrEmail);
        try (Connection conn = DBConnection.getConnection()) {
            String q = "SELECT id, account_number, username, name, email, phone, address, password_hash, created_at FROM customers WHERE username = ? OR email = ? LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setString(1, usernameOrEmail);
                ps.setString(2, usernameOrEmail);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        LOG.fine("Auth failed: no matching customer (id=" + safeUser + ")");
                        return AuthResult.error("INVALID", "Invalid username/email or password.");
                    }
                    String storedHash = rs.getString("password_hash");
                    if (storedHash == null || storedHash.isEmpty()) {
                        LOG.warning("Auth failed: missing password hash (user=" + safeUser + ", customerId=" + rs.getInt("id") + ")");
                        return AuthResult.error("INVALID", "Invalid username/email or password.");
                    }
                    boolean verified;
                    try {
                        verified = PasswordUtil.verifyPassword(password, storedHash);
                    } catch (RuntimeException e) {
                        LOG.log(Level.WARNING, "Auth password verification threw (user=" + safeUser + ", customerId=" + rs.getInt("id") + ")", e);
                        verified = false;
                    }
                    if (!verified) {
                        LOG.fine("Auth failed: password mismatch (user=" + safeUser + ", customerId=" + rs.getInt("id") + ", hashFormat=" + (storedHash.contains(":") ? "pbkdf2" : "legacy") + ")");
                        return AuthResult.error("INVALID", "Invalid username/email or password.");
                    }
                    int customerId = rs.getInt("id");
                    double balance = 0.0;
                    boolean hasAccount = false;
                    try (PreparedStatement aps = conn.prepareStatement("SELECT balance FROM accounts WHERE customer_id = ? LIMIT 1")) {
                        aps.setInt(1, customerId);
                        try (ResultSet ars = aps.executeQuery()) {
                            if (ars.next()) {
                                hasAccount = true;
                                balance = ars.getDouble("balance");
                            }
                        }
                    }
                    if (!hasAccount) {
                        LOG.warning("Auth blocked: missing account row (user=" + safeUser + ", customerId=" + customerId + ")");
                        return AuthResult.error("MISSING_ACCOUNT", "Account record missing for this customer. Please contact support or run the verified repair step.");
                    }
                    Customer customer = new Customer();
                    customer.setId(customerId);
                    customer.setAccountNumber(rs.getString("account_number"));
                    customer.setUsername(rs.getString("username"));
                    customer.setName(rs.getString("name"));
                    customer.setEmail(rs.getString("email"));
                    customer.setPhone(rs.getString("phone"));
                    customer.setAddress(rs.getString("address"));
                    customer.setPasswordHash(storedHash);
                    customer.setCreatedAt(rs.getTimestamp("created_at"));
                    customer.setBalance(balance);
                    LOG.info("Auth succeeded (user=" + safeUser + ", customerId=" + customerId + ")");
                    return AuthResult.ok(customer);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Auth DB failure (user=" + safeUser + ", SQLState=" + e.getSQLState() + ", code=" + e.getErrorCode() + ")", e);
            return AuthResult.error("DB_ERROR", "Login service unavailable. Please try again later.");
        }
    }

    /** @deprecated Use authenticate() to distinguish invalid credentials, missing account, and DB errors. */
    @Deprecated
    public Customer loginCustomerByUsernameOrEmail(String usernameOrEmail, String password) throws SQLException {
        AuthResult r = authenticate(usernameOrEmail, password);
        if (r.isSuccess()) return r.getCustomer();
        if ("DB_ERROR".equals(r.getErrorCode())) throw new SQLException(r.getMessage());
        return null;
    }
    
    public Customer getCustomerById(int customerId) {
        String query = "SELECT c.*, a.balance FROM customers c " +
                      "JOIN accounts a ON c.id = a.customer_id WHERE c.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Customer customer = new Customer();
                customer.setId(rs.getInt("id"));
                customer.setAccountNumber(rs.getString("account_number"));
                customer.setUsername(rs.getString("username"));
                customer.setName(rs.getString("name"));
                customer.setEmail(rs.getString("email"));
                customer.setPhone(rs.getString("phone"));
                customer.setAddress(rs.getString("address"));
                customer.setPasswordHash(rs.getString("password_hash"));
                customer.setCreatedAt(rs.getTimestamp("created_at"));
                customer.setBalance(rs.getDouble("balance"));
                rs.close();
                return customer;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public int getAccountId(int customerId) {
        String query = "SELECT id FROM accounts WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                rs.close();
                return id;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public static class TxResult {
        public final boolean ok; public final String message; public final double balance; public final String transactionId; public final String date;
        public TxResult(boolean ok, String message, double balance, String transactionId, String date) { this.ok=ok; this.message=message; this.balance=balance; this.transactionId=transactionId; this.date=date; }
    }

    public boolean deposit(int customerId, double amount) { return depositResult(customerId, amount).ok; }

    public TxResult depositResult(int customerId, double amount) {
        if (amount <= 0) return new TxResult(false, "Amount must be greater than zero.", 0, null, null);
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            int accountId = getAccountIdForConnection(customerId, conn);
            if (accountId == 0) { conn.rollback(); return new TxResult(false, "Deposit failed. Please try again.", 0, null, null); }
            String updateQuery = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(updateQuery);
            psUpdate.setDouble(1, amount);
            psUpdate.setInt(2, accountId);
            int result = psUpdate.executeUpdate();
            psUpdate.close();
            if (result == 0) { conn.rollback(); return new TxResult(false, "Deposit failed. Please try again.", 0, null, null); }
            double newBalance = getBalanceForConnection(accountId, conn);
            String transQuery = "INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description) VALUES (?, 'DEPOSIT', ?, ?, ?)";
            PreparedStatement psTrans = conn.prepareStatement(transQuery, Statement.RETURN_GENERATED_KEYS);
            psTrans.setInt(1, accountId);
            psTrans.setDouble(2, amount);
            psTrans.setDouble(3, newBalance);
            psTrans.setString(4, "Deposit");
            psTrans.executeUpdate();
            String txId = null, txDate = null;
            try (ResultSet gk = psTrans.getGeneratedKeys()) { if (gk.next()) txId = String.valueOf(gk.getLong(1)); }
            psTrans.close();
            try (PreparedStatement ds = conn.prepareStatement("SELECT created_at FROM transactions WHERE id=?")) { ds.setString(1, txId); try (ResultSet dr = ds.executeQuery()) { if (dr.next() && dr.getTimestamp(1)!=null) txDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dr.getTimestamp(1)); } } catch (Exception ignore) {}
            conn.commit();
            if (txDate == null) txDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            return new TxResult(true, "Deposit successful.", newBalance, txId, txDate);
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            e.printStackTrace();
            return new TxResult(false, "Deposit failed. Please try again.", 0, null, null);
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
        }
    }
    
    public boolean withdraw(int customerId, double amount) { return withdrawResult(customerId, amount).ok; }

    public TxResult withdrawResult(int customerId, double amount) {
        if (amount <= 0) return new TxResult(false, "Amount must be greater than zero.", 0, null, null);
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            int accountId = getAccountIdForConnection(customerId, conn);
            if (accountId == 0) { conn.rollback(); return new TxResult(false, "Withdrawal failed. Please try again.", 0, null, null); }
            double currentBalance = getBalanceForConnection(accountId, conn);
            if (currentBalance < amount) { conn.rollback(); return new TxResult(false, "Insufficient balance.", currentBalance, null, null); }
            String updateQuery = "UPDATE accounts SET balance = balance - ? WHERE id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(updateQuery);
            psUpdate.setDouble(1, amount);
            psUpdate.setInt(2, accountId);
            int result = psUpdate.executeUpdate();
            psUpdate.close();
            if (result == 0) { conn.rollback(); return new TxResult(false, "Withdrawal failed. Please try again.", currentBalance, null, null); }
            double newBalance = currentBalance - amount;
            String transQuery = "INSERT INTO transactions (account_id, transaction_type, amount, balance_after, description) VALUES (?, 'WITHDRAWAL', ?, ?, ?)";
            PreparedStatement psTrans = conn.prepareStatement(transQuery, Statement.RETURN_GENERATED_KEYS);
            psTrans.setInt(1, accountId);
            psTrans.setDouble(2, amount);
            psTrans.setDouble(3, newBalance);
            psTrans.setString(4, "Withdrawal");
            psTrans.executeUpdate();
            String txId = null, txDate = null;
            try (ResultSet gk = psTrans.getGeneratedKeys()) { if (gk.next()) txId = String.valueOf(gk.getLong(1)); }
            psTrans.close();
            try (PreparedStatement ds = conn.prepareStatement("SELECT created_at FROM transactions WHERE id=?")) { ds.setString(1, txId); try (ResultSet dr = ds.executeQuery()) { if (dr.next() && dr.getTimestamp(1)!=null) txDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(dr.getTimestamp(1)); } } catch (Exception ignore) {}
            conn.commit();
            if (txDate == null) txDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            return new TxResult(true, "Withdrawal successful.", newBalance, txId, txDate);
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            e.printStackTrace();
            return new TxResult(false, "Withdrawal failed. Please try again.", 0, null, null);
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
        }
    }
    
    private int getAccountIdForConnection(int customerId, Connection conn) throws SQLException {
        String query = "SELECT id FROM accounts WHERE customer_id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, customerId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int id = rs.getInt("id");
            rs.close();
            ps.close();
            return id;
        }
        rs.close();
        ps.close();
        return 0;
    }
    
    private double getBalanceForConnection(int accountId, Connection conn) throws SQLException {
        String query = "SELECT balance FROM accounts WHERE id = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, accountId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            double balance = rs.getDouble("balance");
            rs.close();
            ps.close();
            return balance;
        }
        rs.close();
        ps.close();
        return 0.00;
    }
    
    public double getBalance(int customerId) {
        String query = "SELECT a.balance FROM accounts a WHERE a.customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                rs.close();
                return balance;
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.00;
    }
    
    public List<Transaction> getTransactionHistory(int customerId) {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT t.* FROM transactions t " +
                      "JOIN accounts a ON t.account_id = a.id " +
                      "WHERE a.customer_id = ? " +
                      "ORDER BY t.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setId(rs.getInt("id"));
                transaction.setAccountId(rs.getInt("account_id"));
                transaction.setTransactionType(rs.getString("transaction_type"));
                transaction.setAmount(rs.getDouble("amount"));
                transaction.setBalanceAfter(rs.getDouble("balance_after"));
                transaction.setDescription(rs.getString("description"));
                transaction.setCreatedAt(rs.getTimestamp("created_at"));
                transactions.add(transaction);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }
}
