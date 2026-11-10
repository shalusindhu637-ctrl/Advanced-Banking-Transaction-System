-- Safe migration: add transaction PIN fields (preserves all existing data/passwords/balances)
-- Run manually after review. Compatible with MySQL 8.0 (no column IF NOT EXISTS).
USE banking;

-- Add columns only if missing (check information_schema or run individually; ignore "Duplicate column" if re-run carefully).
ALTER TABLE customers ADD COLUMN tx_pin_hash VARCHAR(255) NULL AFTER password_hash;
ALTER TABLE customers ADD COLUMN tx_pin_failed_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE customers ADD COLUMN tx_pin_locked_until TIMESTAMP NULL DEFAULT NULL;