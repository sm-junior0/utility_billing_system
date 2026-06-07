ALTER TABLE user_accounts
    DROP CONSTRAINT IF EXISTS user_accounts_status_check
@@

ALTER TABLE user_accounts
    ADD CONSTRAINT user_accounts_status_check
    CHECK (status IN ('ACTIVE', 'PENDING_VERIFICATION', 'INACTIVE'))
@@

ALTER TABLE customers
    DROP CONSTRAINT IF EXISTS customers_status_check
@@

ALTER TABLE customers
    ADD CONSTRAINT customers_status_check
    CHECK (status IN ('ACTIVE', 'PENDING_APPROVAL', 'PENDING_VERIFICATION', 'INACTIVE'))
@@

ALTER TABLE otp_verifications
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(30)
@@

UPDATE otp_verifications
SET purpose = 'REGISTRATION'
WHERE purpose IS NULL
@@

ALTER TABLE otp_verifications
    ALTER COLUMN purpose SET NOT NULL
@@

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS approved_by BIGINT
@@

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP
@@
