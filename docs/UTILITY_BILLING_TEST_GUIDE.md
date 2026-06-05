# Utility Billing — Fresh Start Testing Guide

Complete walkthrough from an **empty database** to a full billing cycle.  
Use **Swagger** at `http://localhost:8080/swagger-ui.html`.

---

## Before You Start — Checklist

| Item | Value |
|------|-------|
| Database | `utilitybilling` on PostgreSQL |
| DB user / password | `postgres` / `1234` |
| Swagger URL | `http://localhost:8080/swagger-ui.html` |
| **Mail sender (SMTP)** | `juniormanene@gmail.com` (configured in `application.properties`) |
| Gmail app password | Set in `spring.mail.password` — no spaces |

---

## Part A — Reset Database (Fresh Start)

Always reset before a clean demo so old IDs and duplicate emails do not cause errors.

### Step A1. Stop the Spring Boot app

Close the running terminal or stop the process so PostgreSQL is not locked.

### Step A2. Optional — fix old payment notification text

If you already have `PAYMENT_COMPLETED` rows saying **processed** instead of **paid**, run:

```powershell
$env:PGPASSWORD='1234'; psql -U postgres -d utilitybilling -f docs/update-payment-notification-messages.sql
```

### Step A3. Run the reset script

**Option 1 — pgAdmin**

1. Open pgAdmin → connect to PostgreSQL.
2. Open database `utilitybilling`.
3. Open **Query Tool**.
4. Open file `docs/reset-database.sql` from this project.
5. Click **Execute** (F5).

**Option 2 — PowerShell**

```powershell
cd C:\Users\RCA\Documents\ne\exam\utilitybilling
psql -U postgres -d utilitybilling -f docs/reset-database.sql
```

Enter password `1234` when prompted.

### Step A4. Confirm tables are empty

The script prints row counts. Every table should show **0** rows.

### Step A5. Start the app

```powershell
mvnw.cmd spring-boot:run
```

Wait until you see `Started UtilitybillingApplication`.

### Step A6. Confirm admin was re-seeded

Login is not needed yet — after startup, only this user exists:

| Field | Value |
|-------|-------|
| Email | `admin@utility.rw` |
| Password | `Admin@123` |
| Role | `ROLE_ADMIN` |
| Status | `ACTIVE` |

---

## Part B — Test Accounts & Email Plan

Use these accounts for the full demo. **Write down IDs** from each API response in the tracker table below.

### Who gets real emails?

| Account | Login email | Password | Final role | Real inbox? | Email you receive |
|---------|-------------|----------|------------|-------------|-------------------|
| **Admin** | `admin@utility.rw` | `Admin@123` | ADMIN | No | None (pre-seeded, no OTP) |
| **Alice (Customer)** | `jj3032205@gmail.com` | `Test@1234` | CUSTOMER | **YES — check jj3032205@gmail.com** | OTP at signup + **automatic** bill/payment emails |
| **Eric (Operator)** | `operator@utility.rw` | `Test@1234` | OPERATOR | No | OTP only in database (see SQL below) |
| **Fiona (Finance)** | `juniormanene@gmail.com` | `Test@1234` | FINANCE | **YES — check juniormanene@gmail.com** | OTP at signup |

> **SMTP sender** is `juniormanene@gmail.com`. OTP and notification emails are **sent from** that address **to** the recipient email in each step.

### Customer profile email (for notification send)

When creating Alice's **customer record**, use the same email `jj3032205@gmail.com`. Bill and full-payment emails are sent **automatically** to that address after Steps 10 and 12.

### ID tracker — fill in as you go

| What | Your ID | Notes |
|------|---------|-------|
| Alice userId | ______ | From signup response `userId` |
| Eric userId | ______ | From signup response `userId` |
| Fiona userId | ______ | From signup response `userId` |
| Customer id (Alice) | ______ | From `POST /api/customers` response `id` |
| Water meter id | ______ | From `POST /api/meters` (WTR-0001) |
| Electricity meter id | ______ | From `POST /api/meters` (ELC-0001) |
| Reading id | ______ | From `POST /api/readings` response `id` |
| Bill id | ______ | From `POST /api/bills/generate` |
| Bill reference | ______ | From bill response `billReference` |
| Notification id (bill) | ______ | From `GET /api/notifications` |

---

## Part C — JWT in Swagger

1. Call `POST /api/auth/login` and copy the `token` value.
2. Click the green **Authorize** button (top right).
3. Paste **only the token** — do not type `Bearer`.
4. Click **Authorize**, then **Close**.

**Important:** After admin changes a user's role, that user must **login again** to get a new JWT.

---

## Part D — Step-by-Step Demo (17 Steps)

---

### Step 1 — Sign up all three staff/customer users

**Swagger:** `01 - Authentication`  
**Auth:** None (public)

Run **three separate** signup calls:

**1a. Alice (future customer) — OTP goes to jj3032205@gmail.com**

```
POST /api/auth/signup
```

```json
{
  "fullNames": "Alice Uwase",
  "email": "jj3032205@gmail.com",
  "phoneNumber": "250788123456",
  "password": "Test@1234"
}
```

**Save from response:** `userId` → this is Alice's `ownerUserId` later.

**1b. Eric (future operator) — OTP in database only**

```json
{
  "fullNames": "Eric Operator",
  "email": "operator@utility.rw",
  "phoneNumber": "250788111111",
  "password": "Test@1234"
}
```

**1c. Fiona (future finance) — OTP goes to juniormanene@gmail.com**

```json
{
  "fullNames": "Fiona Finance",
  "email": "juniormanene@gmail.com",
  "phoneNumber": "250788222222",
  "password": "Test@1234"
}
```

**Expected for each:**

- Status: `201 Created`
- Response includes `userId`, `email`, `message`
- User role is always `ROLE_CUSTOMER`
- User status is `PENDING_VERIFICATION`
- OTP email sent (check real inboxes for Alice and Fiona)

---

### Step 2 — Verify OTP for each user

**Swagger:** `01 - Authentication`

```
POST /api/auth/verify-otp
```

**Alice:**

```json
{
  "email": "jj3032205@gmail.com",
  "otpCode": "000000"
}
```

Replace `000000` with the 6-digit code from **jj3032205@gmail.com** inbox.

**Eric** — get OTP from database (no real inbox):

```sql
SELECT email, otp_code, expires_at, used
FROM otp_verifications
WHERE email = 'operator@utility.rw'
ORDER BY created_at DESC
LIMIT 1;
```

```json
{
  "email": "operator@utility.rw",
  "otpCode": "PASTE_FROM_DB"
}
```

**Fiona:**

```json
{
  "email": "juniormanene@gmail.com",
  "otpCode": "000000"
}
```

Replace with code from **juniormanene@gmail.com** inbox.

**Expected:** `"OTP verified successfully..."` — users become `ACTIVE`.

---

### Step 3 — Login as Admin

```
POST /api/auth/login
```

```json
{
  "email": "admin@utility.rw",
  "password": "Admin@123"
}
```

**Authorize Swagger** with the admin token. Keep this token for Steps 4–10.

---

### Step 4 — List users and note IDs (paginated)

**Swagger:** `02 - Admin User Management`

```
GET /api/users
```

Default: page `0`, size `10`, sorted by `id` ascending.

**Pagination examples:**

| Request | Result |
|---------|--------|
| `GET /api/users` | First 10 users |
| `GET /api/users?page=0&size=5` | First 5 users |
| `GET /api/users?page=1&size=5` | Next 5 users |
| `GET /api/users?sort=email,asc` | Sort by email |

**Response shape:**

```json
{
  "content": [ { "id": 1, "email": "admin@utility.rw", ... } ],
  "page": 0,
  "size": 10,
  "totalElements": 4,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

User list is in the `content` array.

**Expected:** 4 users (admin + Alice + Eric + Fiona).

Confirm:

| Email | Role | Status |
|-------|------|--------|
| admin@utility.rw | ROLE_ADMIN | ACTIVE |
| jj3032205@gmail.com | ROLE_CUSTOMER | ACTIVE |
| operator@utility.rw | ROLE_CUSTOMER | ACTIVE |
| juniormanene@gmail.com | ROLE_CUSTOMER | ACTIVE |

---

### Step 5 — Promote Eric and Fiona

**Swagger:** `02 - Admin User Management`  
**Auth:** Admin token

**Promote Eric to operator:**

```
PUT /api/users/{id}/role
```

Path `id` = Eric's userId from Step 1b.

```json
{
  "role": "ROLE_OPERATOR"
}
```

**Promote Fiona to finance:**

```
PUT /api/users/{id}/role
```

Path `id` = Fiona's userId from Step 1c.

```json
{
  "role": "ROLE_FINANCE"
}
```

**Expected:** Role updated. Eric and Fiona must **login again** before using operator/finance endpoints.

---

### Step 6 — Create Alice's customer record

**Swagger:** `03 - Customer Setup`  
**Auth:** Admin token

```
POST /api/customers
```

```json
{
  "fullNames": "Alice Uwase",
  "nationalId": "1199912345678901",
  "email": "jj3032205@gmail.com",
  "phoneNumber": "250788123456",
  "address": "Kigali, Gasabo",
  "status": "ACTIVE",
  "ownerUserId": 2
}
```

> **Replace `ownerUserId`** with Alice's real `userId` from Step 1a — **not** the example `2` unless your signup response actually returned `2`.

**Expected:** `201 Created` with customer `id`. **Save this `id`** for meters.

**Common errors:**

| Error | Fix |
|-------|-----|
| Owner user not found | Wrong `ownerUserId` — use signup `userId` or `GET /api/users` |
| Owner user must be ACTIVE | Run Step 2 OTP verify first |
| National ID already registered | Run `docs/reset-database.sql` and start over |

---

### Step 7 — Create water and electricity meters

**Swagger:** `04 - Meter Setup`  
**Auth:** Admin token

**Water meter:**

```
POST /api/meters
```

```json
{
  "meterNumber": "WTR-0001",
  "meterType": "WATER",
  "installationDate": "2026-01-10",
  "status": "ACTIVE",
  "customerId": 1
}
```

> **Replace `customerId`** with the real customer `id` from Step 6.

**Electricity meter:**

```json
{
  "meterNumber": "ELC-0001",
  "meterType": "ELECTRICITY",
  "installationDate": "2026-01-10",
  "status": "ACTIVE",
  "customerId": 1
}
```

**Expected:** Two meters created. Save both meter `id` values.

---

### Step 8 — Configure billing (tariffs, tax, penalty, service charges)

**Swagger:** `05 - Billing Configuration`  
**Auth:** Admin token

Run all four blocks in order:

**8a. Flat water tariff**

```
POST /api/tariffs
```

```json
{
  "meterType": "WATER",
  "tariffType": "FLAT",
  "version": 1,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null,
  "flatUnitPrice": 340,
  "tiers": null
}
```

**8b. Tiered electricity tariff**

```
POST /api/tariffs
```

```json
{
  "meterType": "ELECTRICITY",
  "tariffType": "TIERED",
  "version": 1,
  "effectiveFrom": "2026-01-01",
  "effectiveTo": null,
  "flatUnitPrice": null,
  "tiers": [
    { "minUnits": 0, "maxUnits": 50, "pricePerUnit": 100 },
    { "minUnits": 50, "maxUnits": 100, "pricePerUnit": 150 },
    { "minUnits": 100, "maxUnits": null, "pricePerUnit": 200 }
  ]
}
```

**8c. VAT tax**

```
POST /api/taxes
```

```json
{
  "name": "VAT",
  "percentage": 18,
  "active": true
}
```

**8d. Late payment penalty**

```
POST /api/penalties
```

```json
{
  "name": "Late Payment Penalty",
  "percentage": 5,
  "active": true
}
```

**8e. Service charges**

```
POST /api/service-charges
```

Water:

```json
{
  "meterType": "WATER",
  "amount": 500,
  "active": true
}
```

Electricity:

```json
{
  "meterType": "ELECTRICITY",
  "amount": 800,
  "active": true
}
```

**Verify with:** `GET /api/tariffs`, `GET /api/taxes`, `GET /api/penalties`, `GET /api/service-charges`

---

### Step 9 — Login as Operator and capture a reading

**Logout admin JWT** (or open incognito). Login as Eric:

```
POST /api/auth/login
```

```json
{
  "email": "operator@utility.rw",
  "password": "Test@1234"
}
```

**Authorize Swagger** with operator token.

**Swagger:** `06 - Meter Reading Operations`

```
POST /api/readings
```

```json
{
  "meterId": 1,
  "previousReading": 100,
  "currentReading": 135,
  "readingDate": "2026-06-01"
}
```

> Replace `meterId` with the **water meter id** from Step 7.

**Expected:**

- Consumption = `35` (135 − 100)
- Reading month = `6`, year = `2026`
- **Save `readingId`** from response

**Verify:** `GET /api/readings` and `GET /api/readings/meter/{meterId}`

---

### Step 10 — Login as Finance and generate a bill

Login as Fiona:

```json
{
  "email": "juniormanene@gmail.com",
  "password": "Test@1234"
}
```

**Authorize Swagger** with finance token.

**Swagger:** `07 - Billing Operations`

```
POST /api/bills/generate
```

```json
{
  "readingId": 1
}
```

> Replace with real `readingId` from Step 9.

**Expected:**

- Bill status: `PENDING_APPROVAL`
- `outstandingBalance` = `totalAmount`
- `dueDate` = today + 15 days
- **PostgreSQL trigger** creates a `BILL_GENERATED` notification
- **Email sent automatically** to customer email (`jj3032205@gmail.com`)
- **Save `id` and `billReference`**

**Verify:** `GET /api/bills` and `GET /api/notifications` — you should see a new notification row.

---

### Step 11 — Approve the bill

```
POST /api/bills/{id}/approve
```

Path `id` = bill id from Step 10.

**Expected:** Status becomes `APPROVED`, `approvedBy` and `approvedAt` are set.

---

### Step 12 — Record payments (partial then full)

**Swagger:** `08 - Payment Operations`  
**Auth:** Finance token (admin can also record payments)

**12a. Partial payment (by bill id):**

```
POST /api/payments
```

```json
{
  "billId": 1,
  "amountPaid": 5000,
  "paymentMethod": "MOBILE_MONEY",
  "paymentDate": "2026-06-05"
}
```

**Expected:** Bill status `PARTIALLY_PAID`, balance reduced.

**12b. Final payment (by bill reference — alternative):**

First check remaining balance: `GET /api/bills/{id}`

```json
{
  "billReference": "BILL-202606-XXXXXXXX",
  "amountPaid": 9999,
  "paymentMethod": "CASH",
  "paymentDate": "2026-06-10"
}
```

> Replace `billReference` with the real value from Step 10.  
> Replace `amountPaid` with the **exact** remaining `outstandingBalance`.

**Expected:**

- Bill status `PAID`
- `outstandingBalance` = `0`
- **PostgreSQL trigger** creates `PAYMENT_COMPLETED` notification
- **Email sent automatically** to customer email on full payment

**Verify:** `GET /api/payments/bill/{billId}` and check **jj3032205@gmail.com** inbox.

---

### Step 13 — Review notifications (optional manual resend)

**Swagger:** `10 - Notifications`  
**Auth:** Finance or Admin token

```
GET /api/notifications
```

Expected: notifications with status `SENT` after bill generation and full payment.

To resend manually if needed:

```
POST /api/notifications/{id}/send
```

---

### Step 14 — Customer self-service (Alice)

Login as Alice:

```json
{
  "email": "jj3032205@gmail.com",
  "password": "Test@1234"
}
```

**Authorize Swagger** with customer token.

**Swagger:** `09 - Customer Self-Service`

Test each endpoint using **Alice's customer id**:

| Endpoint | Should work? |
|----------|--------------|
| `GET /api/customers/{customerId}` | Yes — own profile only |
| `GET /api/meters/customer/{customerId}` | Yes |
| `GET /api/bills/customer/{customerId}` | Yes |
| `GET /api/payments/customer/{customerId}` | Yes |
| `GET /api/notifications/customer/{customerId}` | Yes |

Try another customer's id — should return **403 Forbidden**.

---

### Step 15 — Legacy prepaid top-up (electricity only)

**Auth:** Finance token

**Swagger:** `11 - Legacy Prepaid Support`

```
POST /api/prepaid/topup
```

```json
{
  "meterId": 2,
  "amount": 2000,
  "paymentMethod": "MOBILE_MONEY",
  "transactionDate": "2026-06-05"
}
```

> `meterId` = electricity meter id from Step 7.

**Expected:** Token generated, units calculated. Fails on water meters.

**Verify:** `GET /api/prepaid/customer/{customerId}`

---

### Step 16 — Late penalties (optional exam demo)

**Swagger:** `07 - Billing Operations`  
**Auth:** Admin or Finance

```
POST /api/bills/apply-late-penalties
```

Runs the PostgreSQL `cursor_apply_late_penalties()` procedure on overdue approved/partially-paid bills.

> For a live demo, you may need a bill past its `dueDate`. This is also applied automatically before each payment.

---

### Step 17 — Negative tests (prove business rules)

Run a few of these to show validations work:

| Test | Endpoint | Expected |
|------|----------|----------|
| Login before OTP verify | `POST /api/auth/login` | 400 error |
| Duplicate signup email | `POST /api/auth/signup` | 400 error |
| Customer views all users | `GET /api/users` as Alice | 403 |
| Operator records payment | `POST /api/payments` as Eric | 403 |
| Payment before approval | `POST /api/payments` on unapproved bill | 400 |
| Overpayment | `amountPaid` > outstanding balance | 400 |
| Reading: current ≤ previous | `POST /api/readings` | 400 |
| Prepaid on water meter | `POST /api/prepaid/topup` on WTR meter | 400 |

---

## Part E — Database Verification Queries

Run in pgAdmin after the full demo:

```sql
-- Users
SELECT id, full_names, email, role, status FROM user_accounts ORDER BY id;

-- OTP history
SELECT id, email, otp_code, used, created_at FROM otp_verifications ORDER BY id DESC;

-- Customer linked to Alice
SELECT id, full_names, email, owner_user_id FROM customers;

-- Meters
SELECT id, meter_number, meter_type, customer_id FROM meters ORDER BY id;

-- Reading
SELECT id, meter_id, consumption, reading_month, reading_year FROM meter_readings;

-- Bills
SELECT id, bill_reference, status, total_amount, outstanding_balance FROM bills;

-- Payments
SELECT id, bill_id, amount_paid, payment_method FROM payments;

-- Notifications
SELECT id, customer_id, type, status, LEFT(message, 80) AS message FROM notifications;
```

---

## Part F — Quick Reference

### Swagger section order

| # | Section | Role |
|---|---------|------|
| 01 | Authentication | Public |
| 02 | Admin User Management | ADMIN |
| 03 | Customer Setup | ADMIN |
| 04 | Meter Setup | ADMIN |
| 05 | Billing Configuration | ADMIN |
| 06 | Meter Reading Operations | OPERATOR |
| 07 | Billing Operations | FINANCE, ADMIN |
| 08 | Payment Operations | FINANCE, ADMIN |
| 09 | Customer Self-Service | CUSTOMER |
| 10 | Notifications | ADMIN, FINANCE |
| 11 | Legacy Prepaid | FINANCE |

### Endpoint updates (recent)

| Feature | Endpoint / field |
|---------|------------------|
| Signup returns userId | `POST /api/auth/signup` → `userId` in response |
| Pay by bill reference | `POST /api/payments` → `billReference` OR `billId` |
| Late penalties | `POST /api/bills/apply-late-penalties` |
| Admin can record payments | `POST /api/payments` allows ADMIN + FINANCE |

### Postman collection

`postman/UtilityBilling.postman_collection.json`

Suggested variables: `baseUrl`, `adminToken`, `operatorToken`, `financeToken`, `customerToken`, `userId`, `customerId`, `meterId`, `readingId`, `billId`, `billReference`, `notificationId`

---

## Part G — Troubleshooting

| Problem | Solution |
|---------|----------|
| OTP email not received | Check `spring.mail.password` app password, internet, spam folder |
| Owner user not found | Use `userId` from signup response, not Swagger example `2` |
| Customer not found | Create customer first; use returned `id`, not example `8` |
| 401 Unauthorized | Re-login and re-Authorize Swagger |
| 403 after role change | User must login again after admin promoted them |
| Duplicate email/National ID | Run `docs/reset-database.sql` and restart |
| Gmail app password has spaces | Remove all spaces in `application.properties` |

---

## Exam Talking Points

- **Auth:** BCrypt passwords, JWT with email + role, OTP verification for signup
- **Roles:** `@PreAuthorize` on every secured endpoint
- **Customer isolation:** `ownerUserId` link — customers see only their data
- **Billing:** Reading → tariff/tax/charge calculation → bill → approve → pay
- **Database routines:** Triggers auto-insert notifications; cursor procedure for late penalties
- **Emails:** OTP to signup address; notifications to customer profile email (`jj3032205@gmail.com` for Alice)
