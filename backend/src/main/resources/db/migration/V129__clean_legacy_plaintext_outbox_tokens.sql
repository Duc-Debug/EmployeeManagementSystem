-- P1-7: Clean up legacy unencrypted plaintext tokens in password_reset_email_outbox
-- 1. Expire and clear any unencrypted pending rows (tokens are valid for max 15 mins)
UPDATE password_reset_email_outbox
SET delivered_at = CURRENT_TIMESTAMP,
    reset_token = '',
    last_error = 'EXPIRED_LEGACY_MIGRATION'
WHERE delivered_at IS NULL
  AND reset_token NOT LIKE 'ENC:%';

-- 2. Clear token for any delivered/historical rows that still hold unencrypted tokens
UPDATE password_reset_email_outbox
SET reset_token = ''
WHERE reset_token != ''
  AND reset_token NOT LIKE 'ENC:%';
