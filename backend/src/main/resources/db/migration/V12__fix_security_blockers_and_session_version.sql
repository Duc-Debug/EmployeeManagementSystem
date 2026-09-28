-- ============================================================
-- FLYWAY MIGRATION V6: FIX SECURITY BLOCKERS & SESSION VERSION
-- ============================================================

-- 1. Add token_version to users table for deterministic JWT session invalidation
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 1;

-- 2. Add version to password_reset_tokens table for optimistic locking
ALTER TABLE password_reset_tokens ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
