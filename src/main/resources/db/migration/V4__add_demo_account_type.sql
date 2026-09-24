-- Existing users, including real administrators, remain non-demo.
-- Hibernate creates this column on fresh schemas where users does not yet exist.
ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS demo_account_type VARCHAR(16) NOT NULL DEFAULT 'NONE';
