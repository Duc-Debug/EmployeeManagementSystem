-- Fail safely when legacy identities are ambiguous. The temporary table makes
-- duplicate emails and username/email cross-collisions abort this migration.
DROP TABLE IF EXISTS user_identity_uniqueness_check;

CREATE TABLE user_identity_uniqueness_check (
    identity_value VARCHAR(255) PRIMARY KEY
);

INSERT INTO user_identity_uniqueness_check (identity_value)
SELECT LOWER(TRIM(username)) FROM users;

INSERT INTO user_identity_uniqueness_check (identity_value)
SELECT LOWER(TRIM(email))
FROM users
WHERE email IS NOT NULL AND TRIM(email) <> '';

DROP TABLE user_identity_uniqueness_check;

UPDATE users
SET email = NULLIF(LOWER(TRIM(email)), '')
WHERE email IS NOT NULL;

CREATE UNIQUE INDEX ux_users_email ON users(email);
