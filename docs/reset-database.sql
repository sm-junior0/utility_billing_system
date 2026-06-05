-- =============================================================================
-- Utility Billing — FULL DATABASE RESET
-- =============================================================================
-- Run this in pgAdmin or psql BEFORE a fresh end-to-end test.
-- Database: utilitybilling
--
-- What it does:
--   1. Deletes ALL business data (users, customers, bills, OTPs, etc.)
--   2. Resets ID sequences back to 1
--   3. On next app start, DataInitializer re-creates admin@utility.rw automatically
--
-- Usage (PowerShell):
--   psql -U postgres -d utilitybilling -f docs/reset-database.sql
--
-- Or paste into pgAdmin Query Tool and execute.
-- =============================================================================

-- Stop the Spring Boot app first to avoid open connections.

TRUNCATE TABLE
    notifications,
    payments,
    prepaid_transactions,
    bills,
    meter_readings,
    tariff_tiers,
    meters,
    customers,
    otp_verifications,
    tariffs,
    taxes,
    penalties,
    service_charges,
    user_accounts
RESTART IDENTITY CASCADE;

-- Verify empty tables (optional — should return 0 for every count)
SELECT 'user_accounts' AS table_name, COUNT(*) AS rows FROM user_accounts
UNION ALL SELECT 'otp_verifications', COUNT(*) FROM otp_verifications
UNION ALL SELECT 'customers', COUNT(*) FROM customers
UNION ALL SELECT 'meters', COUNT(*) FROM meters
UNION ALL SELECT 'meter_readings', COUNT(*) FROM meter_readings
UNION ALL SELECT 'bills', COUNT(*) FROM bills
UNION ALL SELECT 'payments', COUNT(*) FROM payments
UNION ALL SELECT 'notifications', COUNT(*) FROM notifications;

-- After running this script:
--   1. Start the app: mvnw.cmd spring-boot:run
--   2. Admin is auto-seeded: admin@utility.rw / Admin@123
--   3. Follow docs/UTILITY_BILLING_TEST_GUIDE.md from Step 1
