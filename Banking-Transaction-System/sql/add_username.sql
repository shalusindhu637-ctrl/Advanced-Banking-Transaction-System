-- Add username column to support username login
USE banking;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS username VARCHAR(50) UNIQUE AFTER account_number;
CREATE INDEX IF NOT EXISTS idx_username ON customers(username);
