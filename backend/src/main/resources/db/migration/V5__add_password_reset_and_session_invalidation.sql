-- ============================================================
-- FLYWAY MIGRATION V5: ADD EMAIL, PASSWORD_CHANGED_AT & RESET TOKEN TABLE
-- ============================================================

-- 1. Add email and password_changed_at columns safely (NULLABLE)
ALTER TABLE users ADD COLUMN email VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP NULL;

-- 2. Create Password Reset Tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 3. Backfill default email addresses for existing users if NULL
UPDATE users SET email = CONCAT(username, '@company.com') WHERE email IS NULL;
