-- Sample data guide for manual exam testing.
-- Users are easiest to create through Swagger so BCrypt is handled automatically.

-- Suggested signup payload for any public user
-- {
--   "fullNames": "Customer User",
--   "email": "customer1@utility.rw",
--   "phoneNumber": "250788999111",
--   "password": "Admin@123"
-- }
-- The system will automatically assign ROLE_CUSTOMER and PENDING_VERIFICATION status,
-- generate a 6-digit OTP, and send it to the email address.
-- Verify the user with POST /api/auth/verify-otp before login.
-- Admin should later promote users with PUT /api/users/{id}/role when operator or finance access is needed.

-- Suggested customer payload
-- {
--   "fullNames": "Alice Uwase",
--   "nationalId": "1199912345678901",
--   "email": "alice@client.rw",
--   "phoneNumber": "250788123456",
--   "address": "Kigali, Gasabo",
--   "status": "ACTIVE",
--   "ownerUserId": 4
-- }

-- Suggested water meter payload
-- {
--   "meterNumber": "WTR-0001",
--   "meterType": "WATER",
--   "installationDate": "2026-01-10",
--   "status": "ACTIVE",
--   "customerId": 1
-- }

-- Suggested electricity meter payload
-- {
--   "meterNumber": "ELC-0001",
--   "meterType": "ELECTRICITY",
--   "installationDate": "2026-01-10",
--   "status": "ACTIVE",
--   "customerId": 1
-- }

-- Suggested tax payload
-- {
--   "name": "VAT",
--   "percentage": 18,
--   "active": true
-- }

-- Suggested penalty payload
-- {
--   "name": "Late Payment Penalty",
--   "percentage": 5,
--   "active": true
-- }

-- Suggested service charge payload
-- {
--   "meterType": "WATER",
--   "amount": 500,
--   "active": true
-- }

-- Suggested water flat tariff payload
-- {
--   "meterType": "WATER",
--   "tariffType": "FLAT",
--   "version": 1,
--   "effectiveFrom": "2026-01-01",
--   "flatUnitPrice": 340
-- }

-- Suggested electricity tiered tariff payload
-- {
--   "meterType": "ELECTRICITY",
--   "tariffType": "TIERED",
--   "version": 1,
--   "effectiveFrom": "2026-01-01",
--   "tiers": [
--     { "minUnits": 0, "maxUnits": 50, "pricePerUnit": 100 },
--     { "minUnits": 50, "maxUnits": 100, "pricePerUnit": 150 },
--     { "minUnits": 100, "maxUnits": null, "pricePerUnit": 200 }
--   ]
-- }
