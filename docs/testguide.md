# Utility Billing System — Complete API Testing Guide

Test every endpoint from signup through billing, payments, and notifications.  
Works with **Swagger**, **Postman**, or any HTTP client.

---

## Quick start


| Item               | Value                                            |
| ------------------ | ------------------------------------------------ |
| App                | `http://localhost:8080`                          |
| Swagger            | `http://localhost:8080/swagger-ui.html`          |
| Database           | PostgreSQL `utilitybilling`                      |
| DB user / password | `postgres` / `1234`                              |
| Postman collection | `postman/UtilityBilling.postman_collection.json` |
| SMTP sender        | `juniormanene@gmail.com`                         |


Start the app:

```powershell
mvnw.cmd spring-boot:run
```

---

## Fresh database reset (do this first)

1. Stop the Spring Boot app.
2. Run `docs/reset-database.sql` in pgAdmin or:

```powershell
$env:PGPASSWORD='1234'
psql -U postgres -d utilitybilling -f docs/reset-database.sql
```

1. Restart the app.
2. Confirm only the seeded admin exists:


| Field    | Value              |
| -------- | ------------------ |
| Email    | `admin@utility.rw` |
| Password | `Admin@123`        |
| Role     | `ROLE_ADMIN`       |


---

## Test accounts


| Role         | Email                    | Password    | Real inbox? | Emails received                    |
| ------------ | ------------------------ | ----------- | ----------- | ---------------------------------- |
| **Admin**    | `admin@utility.rw`       | `Admin@123` | No          | None (pre-seeded)                  |
| **Customer** | `juniormanene@gmail.com` | `Test@1234` | **Yes**     | OTP + bill + payment notifications |
| **Operator** | `jj3032205@gmail.com`    | `Test@1234` | **Yes**     | OTP at signup; password reset OTP  |
| **Finance**  | `s.manene.jr@gmail.com`  | `Test@1234` | **Yes**     | OTP at signup                      |


> SMTP sends **from** `juniormanene@gmail.com` **to** the recipient address in each step.  
> Customer bill/payment emails go to `juniormanene@gmail.com` (customer profile email).

---

## How to use JWT

1. `POST /api/auth/login` → copy `token`
2. **Swagger:** Authorize → paste token only (no `Bearer` prefix)
3. **Postman:** Authorization → Bearer Token → paste token
4. After admin changes a role → that user must **login again**

---

## ID tracker

Fill in IDs from API responses as you go:


| What                   | Your ID |
| ---------------------- | ------- |
| Customer userId        | ______  |
| Operator userId        | ______  |
| Finance userId         | ______  |
| Customer id            | ______  |
| Water meter id         | ______  |
| Electricity meter id   | ______  |
| Reading id             | ______  |
| Bill id                | ______  |
| Bill reference         | ______  |
| Notification id (bill) | ______  |


---

## Full endpoint checklist — 53 endpoints

Tick each row as you complete it. Run in order for the smoothest demo.

---

### 01 — Authentication (7 endpoints) · Public


| ✓   | #   | Method | Endpoint                    | What to do                                     | Expected         |
| --- | --- | ------ | --------------------------- | ---------------------------------------------- | ---------------- |
| ☐   | 1   | POST   | `/api/auth/signup`          | Register customer, operator, finance (3 calls) | `201`, OTP sent  |
| ☐   | 2   | POST   | `/api/auth/verify-otp`      | Verify OTP for each account                    | Account `ACTIVE` |
| ☐   | 3   | POST   | `/api/auth/resend-otp`      | Resend for an unverified user                  | New OTP sent     |
| ☐   | 4   | POST   | `/api/auth/forgot-password` | Send reset to `jj3032205@gmail.com`            | OTP sent         |
| ☐   | 5   | POST   | `/api/auth/reset-password`  | Submit OTP + new password                      | Password updated |
| ☐   | 6   | POST   | `/api/auth/login`           | Login each role                                | JWT returned     |
| ☐   | 7   | POST   | `/api/auth/logout`          | Call with valid token                          | Success message  |


#### Signup bodies

**Customer:**

```json
{
  "fullNames": "Alice Uwase",
  "email": "juniormanene@gmail.com",
  "phoneNumber": "250788123456",
  "password": "Test@1234"
}
```

**Operator:**

```json
{
  "fullNames": "Eric Operator",
  "email": "jj3032205@gmail.com",
  "phoneNumber": "250788111111",
  "password": "Test@1234"
}
```

**Finance:**

```json
{
  "fullNames": "Fiona Finance",
  "email": "s.manene.jr@gmail.com",
  "phoneNumber": "250788222222",
  "password": "Test@1234"
}
```

#### Verify OTP

```json
{
  "email": "juniormanene@gmail.com",
  "otpCode": "000000"
}
```

Replace `000000` with the 6-digit code from the inbox. Repeat for operator and finance emails.

> **OTP checkpoint:** Pause and check email before continuing.

---

### 02 — Admin user management (4) · ADMIN token


| ✓   | #   | Method | Endpoint                 | What to do                 | Expected      |
| --- | --- | ------ | ------------------------ | -------------------------- | ------------- |
| ☐   | 8   | GET    | `/api/users`             | List users (paginated)     | 4 users       |
| ☐   | 9   | GET    | `/api/users/{id}`        | Get operator by ID         | User details  |
| ☐   | 10  | PUT    | `/api/users/{id}/status` | Set status `ACTIVE`        | Updated       |
| ☐   | 11  | PUT    | `/api/users/{id}/role`   | Promote operator + finance | Roles updated |


**Promote operator** (`{id}` = operator userId):

```json
{ "role": "ROLE_OPERATOR" }
```

**Promote finance** (`{id}` = finance userId):

```json
{ "role": "ROLE_FINANCE" }
```

> Operator and finance must **re-login** after promotion.

---

### 03 — Customer setup (7)


| ✓   | #   | Method | Endpoint                         | Auth                   | What to do                               | Expected           |
| --- | --- | ------ | -------------------------------- | ---------------------- | ---------------------------------------- | ------------------ |
| ☐   | 12  | POST   | `/api/customers`                 | ADMIN                  | Create customer directly (optional path) | `201`              |
| ☐   | 13  | POST   | `/api/customers/profile-request` | CUSTOMER               | Submit nationalId + address              | `PENDING_APPROVAL` |
| ☐   | 14  | GET    | `/api/customers`                 | ADMIN/FINANCE/OPERATOR | List customers                           | Paginated list     |
| ☐   | 15  | GET    | `/api/customers/{id}`            | ADMIN+ or own CUSTOMER | Get profile                              | `200`              |
| ☐   | 16  | PUT    | `/api/customers/{id}`            | ADMIN                  | Update details                           | `200`              |
| ☐   | 17  | PUT    | `/api/customers/{id}/status`     | ADMIN                  | Change status                            | `200`              |
| ☐   | 18  | PUT    | `/api/customers/{id}/approve`    | ADMIN                  | Approve pending profile                  | `ACTIVE`           |


**Profile request (customer token):**

```json
{
  "nationalId": "1199912345678901",
  "address": "Kigali, Gasabo"
}
```

> **Email checkpoint:** Confirm `Account Activation Successful` at `juniormanene@gmail.com`.

---

### 04 — Meters (4) · ADMIN token


| ✓   | #   | Method | Endpoint                            | What to do               | Expected |
| --- | --- | ------ | ----------------------------------- | ------------------------ | -------- |
| ☐   | 19  | POST   | `/api/meters`                       | Create water meter       | `201`    |
| ☐   | 19b | POST   | `/api/meters`                       | Create electricity meter | `201`    |
| ☐   | 20  | GET    | `/api/meters`                       | List all meters          | `200`    |
| ☐   | 21  | GET    | `/api/meters/customer/{customerId}` | Customer's meters        | `200`    |
| ☐   | 22  | PUT    | `/api/meters/{id}/status`           | Set ACTIVE/INACTIVE      | `200`    |


**Water meter:**

```json
{
  "meterNumber": "WTR-0001",
  "meterType": "WATER",
  "installationDate": "2026-01-10",
  "status": "ACTIVE",
  "customerId": 1
}
```

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

---

### 05 — Billing configuration (12) · ADMIN token


| ✓   | #   | Method | Endpoint                           | What to do                  | Expected |
| --- | --- | ------ | ---------------------------------- | --------------------------- | -------- |
| ☐   | 23  | POST   | `/api/tariffs`                     | Flat water tariff           | `201`    |
| ☐   | 23b | POST   | `/api/tariffs`                     | Tiered electricity tariff   | `201`    |
| ☐   | 24  | GET    | `/api/tariffs`                     | List tariffs                | `200`    |
| ☐   | 25  | PUT    | `/api/tariffs/{id}/deactivate`     | Deactivate old version      | `200`    |
| ☐   | 26  | POST   | `/api/taxes`                       | Create VAT 18%              | `201`    |
| ☐   | 27  | GET    | `/api/taxes`                       | List taxes                  | `200`    |
| ☐   | 28  | PUT    | `/api/taxes/{id}/status`           | Toggle active               | `200`    |
| ☐   | 29  | POST   | `/api/penalties`                   | Late penalty 5%             | `201`    |
| ☐   | 30  | GET    | `/api/penalties`                   | List penalties              | `200`    |
| ☐   | 31  | PUT    | `/api/penalties/{id}/status`       | Toggle active               | `200`    |
| ☐   | 32  | POST   | `/api/service-charges`             | Water + electricity charges | `201`    |
| ☐   | 33  | GET    | `/api/service-charges`             | List charges                | `200`    |
| ☐   | 34  | PUT    | `/api/service-charges/{id}/status` | Toggle active               | `200`    |


**Flat water tariff:**

```json
{
  "meterType": "WATER",
  "tariffType": "FLAT",
  "version": 1,
  "effectiveFrom": "2026-01-01",
  "flatUnitPrice": 340
}
```

**Tiered electricity tariff:**

```json
{
  "meterType": "ELECTRICITY",
  "tariffType": "TIERED",
  "version": 1,
  "effectiveFrom": "2026-01-01",
  "tiers": [
    { "minUnits": 0, "maxUnits": 50, "pricePerUnit": 100 },
    { "minUnits": 50, "maxUnits": 100, "pricePerUnit": 150 },
    { "minUnits": 100, "maxUnits": null, "pricePerUnit": 200 }
  ]
}
```

---

### 06 — Meter readings (3) · OPERATOR token


| ✓   | #   | Method | Endpoint                        | What to do             | Expected                      |
| --- | --- | ------ | ------------------------------- | ---------------------- | ----------------------------- |
| ☐   | 35  | POST   | `/api/readings`                 | Capture valid reading  | `201`, consumption calculated |
| ☐   | 36  | GET    | `/api/readings`                 | List all readings      | `200`                         |
| ☐   | 37  | GET    | `/api/readings/meter/{meterId}` | Readings for one meter | `200`                         |


**Valid reading:**

```json
{
  "meterId": 1,
  "previousReading": 100,
  "currentReading": 135,
  "readingDate": "2026-06-01"
}
```

**Business rules to verify (all should return 400):**


| Test                  | readingDate                                 | Expected error                                  |
| --------------------- | ------------------------------------------- | ----------------------------------------------- |
| Before installation   | `2025-01-01` (meter installed `2026-01-10`) | Reading date cannot be before installation date |
| Meter installed today | yesterday's date                            | Same error                                      |
| Future date           | tomorrow                                    | Reading date cannot be in the future            |
| Current ≤ previous    | `currentReading: 90`                        | Current reading must be greater than previous   |
| Same month twice      | second reading in June 2026                 | Only one reading per meter per month/year       |


---

### 07 — Billing (6) · FINANCE or ADMIN token


| ✓   | #   | Method | Endpoint                           | What to do                | Expected           |
| --- | --- | ------ | ---------------------------------- | ------------------------- | ------------------ |
| ☐   | 38  | POST   | `/api/bills/generate`              | Generate from `readingId` | `PENDING_APPROVAL` |
| ☐   | 39  | POST   | `/api/bills/{id}/approve`          | Approve bill              | `APPROVED`         |
| ☐   | 40  | GET    | `/api/bills`                       | List all bills            | `200`              |
| ☐   | 41  | GET    | `/api/bills/customer/{customerId}` | Customer bills            | `200`              |
| ☐   | 42  | POST   | `/api/bills/apply-late-penalties`  | Run penalty procedure     | `200`              |
| ☐   | 43  | GET    | `/api/bills/{id}`                  | Get bill + balance        | `200`              |


**Generate bill:**

```json
{ "readingId": 1 }
```

> **Email checkpoint:** Confirm `Utility Billing - Bill Generated` at `juniormanene@gmail.com`.

---

### 08 — Payments (3) · FINANCE or ADMIN token


| ✓   | #   | Method | Endpoint                              | What to do                 | Expected                  |
| --- | --- | ------ | ------------------------------------- | -------------------------- | ------------------------- |
| ☐   | 44  | POST   | `/api/payments`                       | Partial pay, then full pay | `PARTIALLY_PAID` → `PAID` |
| ☐   | 45  | GET    | `/api/payments/bill/{billId}`         | Payments for bill          | `200`                     |
| ☐   | 46  | GET    | `/api/payments/customer/{customerId}` | Customer history           | `200`                     |


**Partial payment (by billId):**

```json
{
  "billId": 1,
  "amountPaid": 5000,
  "paymentMethod": "MOBILE_MONEY",
  "paymentDate": "2026-06-07"
}
```

**Final payment (by billReference):**

```json
{
  "billReference": "BILL-202606-XXXXXXXX",
  "amountPaid": 9632,
  "paymentMethod": "CASH",
  "paymentDate": "2026-06-07"
}
```

> Use today's date for `paymentDate`. Future dates are rejected.  
> Use exact remaining `outstandingBalance` for the final payment.

> **Email checkpoint:** Confirm `Utility Billing - Payment Completed` at `juniormanene@gmail.com`.

---

### 09 — Customer self-service · CUSTOMER token


| ✓   | Endpoint                                          | Expected            |
| --- | ------------------------------------------------- | ------------------- |
| ☐   | `GET /api/customers/{ownCustomerId}`              | Own profile only    |
| ☐   | `GET /api/meters/customer/{ownCustomerId}`        | Own meters          |
| ☐   | `GET /api/bills/customer/{ownCustomerId}`         | Own bills           |
| ☐   | `GET /api/payments/customer/{ownCustomerId}`      | Own payments        |
| ☐   | `GET /api/notifications/customer/{ownCustomerId}` | Own notifications   |
| ☐   | `GET /api/prepaid/customer/{ownCustomerId}`       | Own prepaid history |


Try another customer's ID → expect **403 Forbidden**.

---

### 10 — Notifications (4) · FINANCE or ADMIN token


| ✓   | #   | Method | Endpoint                                                  | What to do              | Expected |
| --- | --- | ------ | --------------------------------------------------------- | ----------------------- | -------- |
| ☐   | 47  | GET    | `/api/notifications`                                      | List all notifications  | `200`    |
| ☐   | 48  | GET    | `/api/notifications/customer/{customerId}`                | Customer notifications  | `200`    |
| ☐   | 49  | POST   | `/api/notifications/bill/{billId}/send-payment-completed` | Resend payment email    | `SENT`   |
| ☐   | 50  | POST   | `/api/notifications/{id}/send`                            | Resend any notification | `SENT`   |


---

### 11 — Legacy prepaid (2) · FINANCE token


| ✓   | #   | Method | Endpoint                             | What to do                    | Expected      |
| --- | --- | ------ | ------------------------------------ | ----------------------------- | ------------- |
| ☐   | 51  | POST   | `/api/prepaid/topup`                 | Top-up electricity meter only | Token + units |
| ☐   | 52  | GET    | `/api/prepaid/customer/{customerId}` | List prepaid transactions     | `200`         |


**Prepaid top-up:**

```json
{
  "meterId": 2,
  "amount": 2000,
  "paymentMethod": "MOBILE_MONEY",
  "transactionDate": "2026-06-07"
}
```

Water meter top-up → expect **400**.

---

## Negative & security tests

Run after the happy path to prove RBAC and validations:


| Test                          | Role     | Endpoint                  | Expected |
| ----------------------------- | -------- | ------------------------- | -------- |
| Customer lists users          | CUSTOMER | `GET /api/users`          | 403      |
| Operator records payment      | OPERATOR | `POST /api/payments`      | 403      |
| Duplicate signup email        | Public   | `POST /api/auth/signup`   | 400      |
| Login before OTP verify       | Public   | `POST /api/auth/login`    | 400      |
| Payment before bill approval  | FINANCE  | `POST /api/payments`      | 400      |
| Overpayment                   | FINANCE  | `POST /api/payments`      | 400      |
| Past reading                  | OPERATOR | `POST /api/readings`      | 400      |
| Prepaid on water meter        | FINANCE  | `POST /api/prepaid/topup` | 400      |
| Customer views other customer | CUSTOMER | `GET /api/customers/999`  | 403      |


---

## Database verification (after full test)

```sql
-- Users
SELECT id, full_names, email, role, status FROM user_accounts ORDER BY id;

-- OTP history
SELECT id, email, otp_code, purpose, used, created_at FROM otp_verifications ORDER BY id DESC;

-- Customers
SELECT id, full_names, email, status, owner_user_id FROM customers;

-- Meters
SELECT id, meter_number, meter_type, installation_date, customer_id FROM meters;

-- Readings
SELECT id, meter_id, consumption, reading_month, reading_year FROM meter_readings;

-- Bills
SELECT id, bill_reference, status, total_amount, outstanding_balance FROM bills;

-- Payments
SELECT id, bill_id, amount_paid, payment_method FROM payments;

-- Notifications
SELECT id, customer_id, type, status, LEFT(message, 80) AS message FROM notifications;
```

---

## Postman setup

**Collection:** `postman/UtilityBilling.postman_collection.json`  
**Suggested environment variables:**


| Variable        | Example                 |
| --------------- | ----------------------- |
| `baseUrl`       | `http://localhost:8080` |
| `adminToken`    | (from admin login)      |
| `customerToken` | (from customer login)   |
| `operatorToken` | (from operator login)   |
| `financeToken`  | (from finance login)    |
| `customerId`    | `1`                     |
| `meterId`       | `1`                     |
| `readingId`     | `1`                     |
| `billId`        | `1`                     |
| `billReference` | `BILL-202606-...`       |
| `otpCode`       | (from email)            |


---

## Troubleshooting


| Problem                     | Solution                                                              |
| --------------------------- | --------------------------------------------------------------------- |
| 401 Unauthorized            | Re-login and refresh JWT                                              |
| 403 after role change       | User must login again                                                 |
| OTP not received            | Check spam; verify `spring.mail.password` in `application.properties` |
| Notification not in inbox   | Check Spam/Promotions tab                                             |
| Payment date rejected       | Use today or a past date — not future                                 |
| Past reading rejected       | `readingDate` must be ≥ meter `installationDate`                      |
| Duplicate email/National ID | Run `docs/reset-database.sql`                                         |
| Fake email bounce           | Do not use addresses like `bob.direct@test.rw`                        |
| Owner user not found        | Use `userId` from signup response                                     |
| No active tariff            | Ensure at least one active tariff exists before generating bills      |


---

## Endpoint count summary


| Module          | Count  |
| --------------- | ------ |
| Authentication  | 7      |
| Admin users     | 4      |
| Customers       | 7      |
| Meters          | 4      |
| Tariffs         | 3      |
| Taxes           | 3      |
| Penalties       | 3      |
| Service charges | 3      |
| Readings        | 3      |
| Bills           | 6      |
| Payments        | 3      |
| Notifications   | 4      |
| Prepaid         | 2      |
| **Total**       | **53** |


---

## Exam talking points

- **Auth:** BCrypt passwords, JWT with email + role, OTP verification for signup
- **RBAC:** `@PreAuthorize` on every secured endpoint
- **Customer isolation:** `ownerUserId` link — customers see only their data
- **Billing:** Reading → tariff/tax/charge calculation → bill → approve → pay
- **DB routines:** Triggers auto-insert notifications; cursor procedure for late penalties
- **Emails:** OTP to signup address; bill/payment notifications to customer profile email
- **Meter rule:** Reading date cannot be before installation date (including meters installed today)

