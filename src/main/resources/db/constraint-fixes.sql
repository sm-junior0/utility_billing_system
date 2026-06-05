ALTER TABLE user_accounts
    DROP CONSTRAINT IF EXISTS user_accounts_status_check
@@

ALTER TABLE user_accounts
    ADD CONSTRAINT user_accounts_status_check
    CHECK (status IN ('ACTIVE', 'PENDING_VERIFICATION', 'INACTIVE'))
@@
