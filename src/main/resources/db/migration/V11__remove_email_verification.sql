-- Email verification is no longer part of account registration. Remove its
-- temporary verification state after allowing any legacy account to sign in.
UPDATE club_users
SET email_verified = TRUE,
    email_verified_at = COALESCE(email_verified_at, created_at)
WHERE email_verified = FALSE;

ALTER TABLE club_users
    DROP COLUMN email_verification_code_sent_at,
    DROP COLUMN email_verification_code_expires_at,
    DROP COLUMN email_verification_code_hash,
    DROP COLUMN email_verified_at,
    DROP COLUMN email_verified;
