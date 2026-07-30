ALTER TABLE club_users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN email_verified_at TIMESTAMPTZ,
    ADD COLUMN email_verification_code_hash VARCHAR(100),
    ADD COLUMN email_verification_code_expires_at TIMESTAMPTZ,
    ADD COLUMN email_verification_code_sent_at TIMESTAMPTZ;

-- Accounts that existed before verification was introduced keep access.
UPDATE club_users
SET email_verified_at = created_at
WHERE email_verified = TRUE;
