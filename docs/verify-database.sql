-- Verify PostgreSQL triggers, procedures, and core tables for the exam demo.

-- 1. Triggers on bills and payments
SELECT tgname AS trigger_name, relname AS table_name
FROM pg_trigger t
JOIN pg_class c ON c.oid = t.tgrelid
WHERE tgname IN ('after_bill_insert_trigger', 'after_payment_insert_trigger')
ORDER BY relname;

-- 2. Functions used by triggers
SELECT proname AS function_name
FROM pg_proc
WHERE proname IN ('insert_bill_notification', 'update_bill_after_payment')
ORDER BY proname;

-- 3. Stored procedures (cursor + billing helpers)
SELECT proname AS procedure_name
FROM pg_proc
WHERE proname IN (
    'approve_bill',
    'record_payment',
    'cursor_apply_late_penalties',
    'generate_monthly_bill'
)
ORDER BY proname;

-- 4. User status constraint (PENDING_VERIFICATION allowed)
SELECT conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conname = 'user_accounts_status_check';

-- 5. Recent notifications (bill vs payment wording)
SELECT id, type, status, sent_at, LEFT(message, 100) AS message_preview
FROM notifications
ORDER BY id DESC
LIMIT 10;

-- Expected healthy result:
--   2 triggers, 2 functions, 4 procedures, status constraint present
