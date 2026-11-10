package com.banking.dao;

import com.banking.util.DBConnection;
import com.banking.util.PinUtil;

import java.sql.*;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transaction PIN and secure balance access. Uses server-side session customerId only (no IDOR).
 * Never logs PINs or hashes.
 */
public class TransactionSecurityDAO {
    private static final Logger LOG = Logger.getLogger(TransactionSecurityDAO.class.getName());
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCKOUT_MINUTES = 15;

    public static class PinStatus {
        public final boolean hasPin;
        public final boolean locked;
        public final String message;
        public PinStatus(boolean hasPin, boolean locked, String message) { this.hasPin = hasPin; this.locked = locked; this.message = message; }
    }

    public static class PinResult {
        public final boolean ok;
        public final String code;
        public final String message;
        public PinResult(boolean ok, String code, String message) { this.ok = ok; this.code = code; this.message = message; }
    }

    public PinStatus getPinStatus(int customerId) {
        String q = "SELECT tx_pin_hash, tx_pin_failed_attempts, tx_pin_locked_until FROM customers WHERE id = ?";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new PinStatus(false, false, "Account not found.");
                boolean has = rs.getString("tx_pin_hash") != null && !rs.getString("tx_pin_hash").isEmpty();
                Timestamp until = rs.getTimestamp("tx_pin_locked_until");
                boolean locked = until != null && until.toInstant().isAfter(Instant.now());
                return new PinStatus(has, locked, locked ? "Transaction PIN temporarily locked. Try again later." : null);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "PIN status lookup failed (customerId=" + customerId + ", SQLState=" + e.getSQLState() + ")", e);
            return new PinStatus(false, false, "Service unavailable.");
        }
    }

    public PinResult setPin(int customerId, String newPin, String confirmPin) {
        if (!PinUtil.isValidPinFormat(newPin) || !newPin.equals(confirmPin)) {
            return new PinResult(false, "INVALID", "Please enter a valid 4- or 6-digit PIN and matching confirmation.");
        }
        try (Connection c = DBConnection.getConnection()) {
            if (hasPinRow(c, customerId)) return new PinResult(false, "HAS_PIN", "A transaction PIN already exists. Use Change PIN.");
            String hash = PinUtil.hashPin(newPin);
            try (PreparedStatement ps = c.prepareStatement("UPDATE customers SET tx_pin_hash=?, tx_pin_failed_attempts=0, tx_pin_locked_until=NULL WHERE id=?")) {
                ps.setString(1, hash);
                ps.setInt(2, customerId);
                ps.executeUpdate();
            }
            LOG.info("Transaction PIN set (customerId=" + customerId + ")");
            return new PinResult(true, "OK", "Transaction PIN set successfully.");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Set PIN failed (customerId=" + customerId + ")", e);
            return new PinResult(false, "ERROR", "Unable to set transaction PIN. Please try again.");
        }
    }

    public PinResult changePin(int customerId, String currentPin, String newPin, String confirmPin) {
        if (!PinUtil.isValidPinFormat(currentPin) || !PinUtil.isValidPinFormat(newPin) || !newPin.equals(confirmPin)) {
            return new PinResult(false, "INVALID", "Please enter valid PIN values and matching confirmation.");
        }
        if (currentPin.equals(newPin)) return new PinResult(false, "SAME", "New PIN must be different from the current PIN.");
        PinResult verify = verifyInternal(customerId, currentPin, true);
        if (!verify.ok) return verify; // preserves lockout/generic messaging
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE customers SET tx_pin_hash=?, tx_pin_failed_attempts=0, tx_pin_locked_until=NULL WHERE id=?")) {
            ps.setString(1, PinUtil.hashPin(newPin));
            ps.setInt(2, customerId);
            ps.executeUpdate();
            LOG.info("Transaction PIN changed (customerId=" + customerId + ")");
            return new PinResult(true, "OK", "Transaction PIN changed successfully.");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Change PIN failed (customerId=" + customerId + ")", e);
            return new PinResult(false, "ERROR", "Unable to change transaction PIN. Please try again.");
        }
    }

    /** Verifies PIN and returns balance on success. Does NOT modify balance/transactions. */
    public BalanceResult verifyPinAndGetBalance(int customerId, String pin) {
        PinResult vr = verifyInternal(customerId, pin, false);
        if (!vr.ok) return new BalanceResult(false, vr.code, vr.message, null);
        String q = "SELECT a.balance, c.account_number FROM accounts a JOIN customers c ON c.id=a.customer_id WHERE a.customer_id=? LIMIT 1";
        try (Connection c = DBConnection.getConnection(); PreparedStatement ps = c.prepareStatement(q)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new BalanceResult(false, "NO_ACCOUNT", "Unable to retrieve balance.", null);
                return new BalanceResult(true, "OK", null, rs.getBigDecimal("balance"));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Balance retrieval failed (customerId=" + customerId + ", SQLState=" + e.getSQLState() + ")", e);
            return new BalanceResult(false, "ERROR", "Unable to retrieve balance. Please try again.", null);
        }
    }

    /** Verifies transaction PIN for money-movement flows (server-side, lockout/FOR UPDATE). No balance/transaction mutation. */
    public PinResult verifyPin(int customerId, String pin) {
        return verifyInternal(customerId, pin, false);
    }

    public static class BalanceResult {
        public final boolean ok; public final String code; public final String message; public final java.math.BigDecimal balance;
        public BalanceResult(boolean ok, String code, String message, java.math.BigDecimal balance) { this.ok=ok; this.code=code; this.message=message; this.balance=balance; }
    }

    private PinResult verifyInternal(int customerId, String pin, boolean forChange) {
        if (!PinUtil.isValidPinFormat(pin)) return new PinResult(false, "INVALID", "Incorrect PIN or PIN not set.");
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                String sel = "SELECT tx_pin_hash, tx_pin_failed_attempts, tx_pin_locked_until FROM customers WHERE id=? FOR UPDATE";
                try (PreparedStatement ps = c.prepareStatement(sel)) {
                    ps.setInt(1, customerId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) { c.rollback(); return new PinResult(false, "NOT_FOUND", "Unable to verify PIN."); }
                        String hash = rs.getString("tx_pin_hash");
                        int attempts = rs.getInt("tx_pin_failed_attempts");
                        Timestamp until = rs.getTimestamp("tx_pin_locked_until");
                        if (hash == null || hash.isEmpty()) {
                            c.rollback();
                            return new PinResult(false, "NOT_SET", "Please set your transaction PIN first.");
                        }
                        if (until != null && until.toInstant().isAfter(Instant.now())) {
                            c.rollback();
                            LOG.fine("PIN verify blocked: locked (customerId=" + customerId + ")");
                            return new PinResult(false, "LOCKED", "Transaction PIN temporarily locked. Try again later.");
                        }
                        boolean ok = PinUtil.verifyPin(pin, hash);
                        if (ok) {
                            try (PreparedStatement up = c.prepareStatement("UPDATE customers SET tx_pin_failed_attempts=0, tx_pin_locked_until=NULL WHERE id=?")) {
                                up.setInt(1, customerId); up.executeUpdate();
                            }
                            c.commit();
                            return new PinResult(true, "OK", null);
                        } else {
                            attempts++;
                            boolean lock = attempts >= MAX_FAILED_ATTEMPTS;
                            Timestamp lockUntil = lock ? Timestamp.from(Instant.now().plusSeconds(LOCKOUT_MINUTES * 60L)) : null;
                            try (PreparedStatement up = c.prepareStatement("UPDATE customers SET tx_pin_failed_attempts=?, tx_pin_locked_until=? WHERE id=?")) {
                                up.setInt(1, lock ? 0 : attempts);
                                up.setTimestamp(2, lockUntil);
                                up.setInt(3, customerId);
                                up.executeUpdate();
                            }
                            c.commit();
                            LOG.fine("PIN verify failed (customerId=" + customerId + ", lock=" + lock + ")");
                            // Generic message (do not reveal correctness details)
                            return new PinResult(false, lock ? "LOCKED" : "INVALID", lock ? "Transaction PIN temporarily locked. Try again later." : "Incorrect PIN or PIN not set.");
                        }
                    }
                }
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "PIN verify error (customerId=" + customerId + ")", e);
            return new PinResult(false, "ERROR", "Unable to verify PIN. Please try again.");
        }
    }

    private boolean hasPinRow(Connection c, int customerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT tx_pin_hash FROM customers WHERE id=?")) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getString(1) != null && !rs.getString(1).isEmpty(); }
        }
    }
}