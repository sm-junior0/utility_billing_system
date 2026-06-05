# Utility Billing — Exam Presentation Guide

Use this when **presenting** to examiners. For step-by-step Swagger clicks and JSON bodies, see `UTILITY_BILLING_TEST_GUIDE.md`.

---

## Is everything working?

| Check | Status |
|-------|--------|
| `mvnw compile test` | Passes |
| PostgreSQL `utilitybilling` database | Connected |
| Swagger UI | `http://localhost:8080/swagger-ui.html` (no `/login` redirect) |
| JWT authentication | Login → Authorize → protected endpoints |
| Input validation | National ID, phone, email, dates, amounts, OTP |
| Bill generation email | Automatic on `POST /api/bills/generate` |
| Full payment email | Automatic when outstanding balance reaches 0 |
| DB triggers | `BILL_GENERATED` + `PAYMENT_COMPLETED` notifications |
| Pagination | `GET /api/users?page=0&size=10` |

**Before you present:** start the app, open Swagger, confirm `Started UtilitybillingApplication` in logs.

---

## One-minute system overview (say this first)

> This is a **Spring Boot 4** utility billing API for WASAC/REG. It uses **PostgreSQL**, **JWT role-based security**, **JPA**, **Swagger**, and **Gmail SMTP**.  
> Customers sign up with **OTP email verification**. An **admin** promotes users to Operator, Finance, or keeps them as Customer.  
> The **operator** captures meter readings. **Finance** generates and approves bills, then records payments.  
> **PostgreSQL triggers** create notification records; the application **emails every notification** to the customer.  
> All APIs are documented and testable in **Swagger**, ordered from authentication through billing to legacy prepaid.

---

## Test accounts (memorize these)

| Role | Email | Password | Real email inbox? |
|------|-------|----------|-------------------|
| **ADMIN** | `admin@utility.rw` | `Admin@123` | No (pre-seeded) |
| **CUSTOMER** | `jj3032205@gmail.com` | `Test@1234` | **Yes** — bill & payment emails |
| **OPERATOR** | `operator@utility.rw` | `Test@1234` | OTP only in DB |
| **FINANCE** | `juniormanene@gmail.com` | `Test@1234` | **Yes** — OTP at signup |

SMTP sender: `juniormanene@gmail.com` (configured in `application.properties`).

---

## Roles — what each user CAN and CANNOT do

### Everyone (no JWT)

| Can do | Cannot do |
|--------|-----------|
| Sign up (`POST /api/auth/signup`) | Choose their own role (always starts as CUSTOMER) |
| Verify OTP (`POST /api/auth/verify-otp`) | Access any protected endpoint |
| Resend OTP (`POST /api/auth/resend-otp`) | |
| Login (`POST /api/auth/login`) | |

---

### ROLE_ADMIN — system owner

| Can do | Cannot do |
|--------|-----------|
| List/paginate users, change status & roles | Capture meter readings (OPERATOR only) |
| Create/update customers & meters | Record payments (FINANCE/ADMIN — admin *can* pay) |
| Configure tariffs, taxes, penalties, service charges | View another customer's self-service data as customer |
| Deactivate tariffs | |
| Everything FINANCE can do (bills, payments, notifications) | |

**Swagger section:** `02` Admin User Management, `03` Customer, `04` Meter, `05` Billing Configuration

---

### ROLE_OPERATOR — field worker

| Can do | Cannot do |
|--------|-----------|
| Capture meter readings (`POST /api/readings`) | Generate or approve bills |
| List readings (all meters) | Record payments |
| View customers, meters (read-only lists) | Manage users, tariffs, taxes |
| | Change customer/meter status |

**Swagger section:** `06 - Meter Reading Operations`

**Business rules to mention:**
- One reading per meter per month/year
- Current reading must be greater than previous
- Only **ACTIVE** meters accept readings

---

### ROLE_FINANCE — billing & payments

| Can do | Cannot do |
|--------|-----------|
| Generate bills from readings | Sign up users or change roles |
| Approve bills | Capture meter readings |
| Record partial/full payments | Create customers or meters |
| Apply late penalties (stored procedure) | Deactivate tariffs (ADMIN only) |
| View all bills, payments, notifications | |
| Resend notification emails manually | |
| View tariffs (read-only) | |

**Swagger section:** `07` Billing, `08` Payments, `10` Notifications

**Business rules to mention:**
- Bill must be **APPROVED** before payment
- Partial payments supported; full payment triggers email
- Payment by `billId` **or** `billReference`

---

### ROLE_CUSTOMER — self-service only

| Can do | Cannot do |
|--------|-----------|
| View **own** customer profile | See other customers' data |
| View **own** meters | Create readings or bills |
| View **own** bills & payments | Record payments |
| View **own** notifications | Admin/finance operations |
| View **own** prepaid history | |

**How it works:** JWT email is linked to a customer via `ownerUserId`. The API checks ownership — accessing another `customerId` returns **403 Forbidden**.

**Swagger section:** `09 - Customer Self-Service`

---

## Recommended presentation flow (15–20 minutes)

Present in **Swagger tag order** (01 → 11). Switch JWT between steps.

### Phase 1 — Security & onboarding (5 min)

1. **Show Swagger** — public, no login redirect, tags ordered 01–11.
2. **Signup** — explain OTP, `PENDING_VERIFICATION`, role always CUSTOMER.
3. **Verify OTP** — account becomes ACTIVE; mention email to real inbox.
4. **Login as Admin** — JWT in Authorize (paste token only, not `Bearer`).
5. **List users** — pagination (`page`, `size`, `sort=id,asc`).
6. **Promote roles** — Operator for Eric, Finance for Fiona; explain **must re-login** for new JWT.

**Say:** *"Security is stateless JWT. Method-level `@PreAuthorize` enforces roles. Customers cannot escalate privileges."*

---

### Phase 2 — Setup (3 min)

7. **Admin creates customer** — link `ownerUserId` from signup; validation on national ID (16 digits), phone, email.
8. **Admin registers meters** — water + electricity for same customer.

**Say:** *"Inactive customers/meters are blocked from billing downstream."*

---

### Phase 3 — Billing configuration (2 min)

9. **Admin creates tariff, tax, penalty, service charge** — brief show of one POST + GET list.

**Say:** *"These are versioned configuration rules used when finance generates a bill."*

---

### Phase 4 — Operations (5 min) — **core demo**

10. **Login as Operator** → `POST /api/readings` (new month, new consumption).
11. **Login as Finance** → `POST /api/bills/generate`.
    - Show bill amounts: tariff + service charge + tax
    - **Open jj3032205@gmail.com** — bill email arrived
    - `GET /api/notifications` — `BILL_GENERATED`, status `SENT`
12. **Approve bill** → `POST /api/bills/{id}/approve`.
13. **Partial payment** → bill becomes `PARTIALLY_PAID`.
14. **Full payment** → bill becomes `PAID`, `PAYMENT_COMPLETED` notification, **payment email**.

**Say:** *"PostgreSQL trigger inserts the notification row; Java sends the email immediately. On startup, any PENDING notification is also emailed."*

---

### Phase 5 — Customer view (2 min)

15. **Login as Alice (customer)** → show self-service: own bills, payments, notifications only.

**Say:** *"Customer isolation is enforced in the service layer, not only in the controller."*

---

### Phase 6 — Technical extras (optional, 2 min)

16. **DB objects** — mention 2 triggers, 2 functions, 4 stored procedures in `routines.sql`.
17. **Validations** — show a bad request (e.g. invalid national ID → 400 with message).
18. **Legacy prepaid** — `11 - Legacy Prepaid Support` (electricity top-up).

---

## Swagger sections map

| Tag | Who uses it | Main purpose |
|-----|-------------|--------------|
| 01 Authentication | Public | Signup, OTP, login |
| 02 Admin User Management | ADMIN | Users, roles, status |
| 03 Customer Setup | ADMIN | Customer CRUD |
| 04 Meter Setup | ADMIN | Meter registration |
| 05 Billing Configuration | ADMIN | Tariffs, tax, penalty, charges |
| 06 Meter Reading Operations | OPERATOR | Capture readings |
| 07 Billing Operations | FINANCE, ADMIN | Generate & approve bills |
| 08 Payment Operations | FINANCE, ADMIN | Record payments |
| 09 Customer Self-Service | CUSTOMER (+ staff) | Own data only for customer |
| 10 Notifications | FINANCE, ADMIN | Audit & resend emails |
| 11 Legacy Prepaid Support | FINANCE | Electricity top-up |

---

## Exam tasks — how your project answers each

| Task | What to show | Where in code |
|------|--------------|---------------|
| **1. User management** | Signup, OTP, admin lists users, changes role/status | `AuthController`, `UserController`, `OtpVerificationService` |
| **2. Customer & meter** | Admin creates customer + meters | `CustomerController`, `MeterController` |
| **3. Billing configuration** | Tariffs (flat/tiered), tax, penalty, service charge | `TariffController`, `TaxController`, etc. |
| **4. Meter reading** | Operator captures reading, consumption calculated | `MeterReadingController`, `MeterReadingService` |
| **5. Bill generation** | Finance generates from reading, approval workflow | `BillController`, `BillService` |
| **6. Payment & notification** | Payments update balance; emails on bill + full payment | `PaymentController`, `NotificationService`, `routines.sql` |

---

## Technical talking points (if examiner asks)

### Security
- JWT in `Authorization: Bearer <token>` header
- BCrypt passwords
- No form login; 401 JSON if token missing/invalid
- Swagger and `/api/auth/**` are public; everything else protected

### Validation
- Jakarta Bean Validation on all request DTOs (`@Valid`)
- National ID: 16 digits | Phone: 10–15 digits | Dates: not in future
- Payment: `billId` OR `billReference` required (not both)

### Notifications & email
- Trigger on bill INSERT → `BILL_GENERATED` notification (message: **processed**)
- Trigger on full payment → `PAYMENT_COMPLETED` notification (message: **paid**)
- `NotificationService.deliverEmail()` sends every notification
- `PendingNotificationEmailRunner` emails leftover `PENDING` rows on startup

### Database
- PostgreSQL with Hibernate `ddl-auto=update`
- SQL init: triggers, functions, procedures from `routines.sql`
- Stored procedure: `approve_bill`, `apply_late_penalties`, etc.

### API docs
- SpringDoc OpenAPI / Swagger UI
- Tags sorted 01–11 for exam demo order
- Realistic example JSON on request bodies

---

## JWT cheat sheet for Swagger

1. `POST /api/auth/login` with the right user
2. Copy `token` from response
3. Click **Authorize** → paste token only → **Authorize** → **Close**
4. Call protected endpoints
5. **After role change** → login again with that user

| Next action | Login as |
|-------------|----------|
| User/role management | `admin@utility.rw` |
| Customer & meters | `admin@utility.rw` |
| Billing config | `admin@utility.rw` |
| Meter reading | `operator@utility.rw` |
| Bill & payment | `juniormanene@gmail.com` |
| Customer self-service | `jj3032205@gmail.com` |

---

## Quick new-bill demo (if examiner asks “show email now”)

Your DB already has bills. To generate a **new** bill and email:

1. **Operator** — new reading (new month, e.g. August):

```json
{
  "meterId": 1,
  "previousReading": 135,
  "currentReading": 168,
  "readingDate": "2026-08-01"
}
```

2. **Finance** — generate bill with `readingId` from step 1 response.

3. Check **jj3032205@gmail.com** and `GET /api/notifications`.

4. Approve → pay full amount → second email on payment.

---

## Common examiner questions & answers

**Q: Why OTP?**  
A: Signup creates `PENDING_VERIFICATION` users. OTP email proves email ownership before activation.

**Q: Can a customer pay their own bill?**  
A: No — only FINANCE (and ADMIN) record payments. Customer can only view.

**Q: What happens on partial payment?**  
A: Status `PARTIALLY_PAID`, balance reduced. No payment-completed email until fully paid.

**Q: What if email fails?**  
A: Notification status becomes `FAILED` in DB. Finance can resend via `POST /api/notifications/{id}/send`.

**Q: How do you prevent duplicate bills?**  
A: Unique constraint on meter + billing month/year; service checks before insert.

**Q: Where is role enforced?**  
A: `@PreAuthorize` on controllers + ownership checks in `CurrentUserService` for customers.

---

## Pre-presentation checklist

- [ ] App running (`mvnw spring-boot:run`)
- [ ] Swagger opens without 401 on the UI itself
- [ ] Gmail app password still valid in `application.properties`
- [ ] Know your live IDs (customer `1`, meters `1`/`2`, or read from GET responses)
- [ ] Phone ready to show **jj3032205@gmail.com** inbox
- [ ] Optional: pgAdmin ready to show triggers/notifications table

---

## If something fails during the demo

| Problem | Quick fix |
|---------|-----------|
| 401 Unauthorized | Login again, re-Authorize with fresh token |
| 403 Forbidden | Wrong role — switch to correct user JWT |
| 400 validation | Read error message; fix national ID, dates, readings |
| Duplicate bill/reading | Use a **new month** in `readingDate` |
| No email | Check `GET /api/notifications` status; resend manually |
| Swagger sort 400 | Leave `sort` empty or use `sort=id,asc` |

---

**Good luck with your presentation.**
