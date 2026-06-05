-- Update existing PAYMENT_COMPLETED notifications to use "paid" instead of "processed".
-- Safe to run multiple times.

UPDATE notifications
SET message = REPLACE(message, 'has been successfully processed.', 'has been successfully paid.'),
    updated_at = NOW()
WHERE type = 'PAYMENT_COMPLETED'
  AND message LIKE '%has been successfully processed.%';

SELECT id, type, status, LEFT(message, 120) AS message_preview
FROM notifications
WHERE type = 'PAYMENT_COMPLETED'
ORDER BY id;
