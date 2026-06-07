# Utility Billing System — Database Schema (dbdiagram.io)

Copy everything inside the **DBML code block** below and paste it into [dbdiagram.io](https://dbdiagram.io).

---

## How to use

1. Open https://dbdiagram.io
2. Click **Import** or paste into the editor
3. Copy the DBML block below (between the triple backticks)
4. Export as PNG/PDF/SQL when done

---

## DBML (paste into dbdiagram.io)

```dbml
// Utility Billing System — PostgreSQL Schema
// WASAC/REG Integrated Utility Billing

Project utility_billing {
  database_type: 'PostgreSQL'
  Note: '''
    # Utility Billing Database
    - 15 tables
    - JWT auth via user_accounts
    - Postpaid water & electricity billing
    - DB triggers for bill/payment notifications
  '''
}

// ─── Authentication & Users ───────────────────────────────────────────────

Table user_accounts {
  id bigint [pk, increment, note: 'Primary key']
  full_names varchar(120) [not null]
  email varchar(120) [not null, unique, note: 'Login username']
  phone_number varchar(20) [not null, unique]
  password varchar(255) [not null, note: 'BCrypt hashed']
  status varchar(20) [not null, note: 'ACTIVE | INACTIVE | PENDING_VERIFICATION']
  role varchar(30) [not null, note: 'ROLE_ADMIN | ROLE_OPERATOR | ROLE_FINANCE | ROLE_CUSTOMER']
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table otp_verifications {
  id bigint [pk, increment]
  email varchar(120) [not null, note: 'Target inbox for OTP']
  otp_code varchar(6) [not null]
  purpose varchar(30) [not null, note: 'REGISTRATION | PASSWORD_RESET']
  expires_at timestamp [not null]
  used boolean [not null, default: false]
  created_at timestamp [not null]

  Note: 'Stores signup and password-reset OTP codes'
}

// ─── Customer & Meters ──────────────────────────────────────────────────────

Table customers {
  id bigint [pk, increment]
  full_names varchar(120) [not null]
  national_id varchar(16) [not null, unique, note: '16-digit Rwandan National ID']
  email varchar(120) [not null, unique, note: 'Bill/notification email']
  phone_number varchar(20) [not null, unique]
  address varchar(255) [not null]
  status varchar(20) [not null, note: 'ACTIVE | INACTIVE | PENDING_APPROVAL']
  owner_user_id bigint [unique, ref: > user_accounts.id, note: 'Links ROLE_CUSTOMER login to profile']
  approved_by bigint [ref: > user_accounts.id]
  approved_at timestamp
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table meters {
  id bigint [pk, increment]
  meter_number varchar(50) [not null, unique]
  meter_type varchar(20) [not null, note: 'WATER | ELECTRICITY']
  installation_date date [not null, note: 'Readings cannot be dated before this']
  status varchar(20) [not null, note: 'ACTIVE | INACTIVE']
  customer_id bigint [not null, ref: > customers.id]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table meter_readings {
  id bigint [pk, increment]
  meter_id bigint [not null, ref: > meters.id]
  previous_reading decimal(14,2) [not null]
  current_reading decimal(14,2) [not null]
  consumption decimal(14,2) [not null, note: 'current - previous']
  reading_date date [not null]
  reading_month int [not null]
  reading_year int [not null]
  captured_by bigint [not null, ref: > user_accounts.id, note: 'ROLE_OPERATOR']
  created_at timestamp [not null]
  updated_at timestamp [not null]

  indexes {
    (meter_id, reading_month, reading_year) [unique, name: 'uq_meter_month_year']
  }
}

// ─── Billing Configuration ──────────────────────────────────────────────────

Table tariffs {
  id bigint [pk, increment]
  meter_type varchar(20) [not null, note: 'WATER | ELECTRICITY']
  tariff_type varchar(20) [not null, note: 'FLAT | TIERED']
  version_number int [not null, note: 'Versioned; new tariffs for future cycles']
  effective_from date [not null]
  effective_to date
  active boolean [not null, default: true]
  flat_unit_price decimal(14,2) [note: 'Used when tariff_type = FLAT']
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table tariff_tiers {
  id bigint [pk, increment]
  tariff_id bigint [not null, ref: > tariffs.id]
  min_units decimal(14,2) [not null]
  max_units decimal(14,2) [note: 'NULL = unlimited upper bound']
  price_per_unit decimal(14,2) [not null]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table taxes {
  id bigint [pk, increment]
  name varchar(100) [not null, note: 'e.g. VAT']
  percentage decimal(6,2) [not null]
  active boolean [not null, default: true]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table penalties {
  id bigint [pk, increment]
  name varchar(100) [not null, note: 'Late payment penalty']
  percentage decimal(6,2) [not null]
  active boolean [not null, default: true]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table service_charges {
  id bigint [pk, increment]
  meter_type varchar(20) [not null, note: 'WATER | ELECTRICITY']
  amount decimal(14,2) [not null, note: 'Fixed monthly charge']
  active boolean [not null, default: true]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

// ─── Billing & Payments ─────────────────────────────────────────────────────

Table bills {
  id bigint [pk, increment]
  bill_reference varchar(50) [not null, unique, note: 'e.g. BILL-202606-7A6F4F68']
  customer_id bigint [not null, ref: > customers.id]
  meter_id bigint [not null, ref: > meters.id]
  reading_id bigint [not null, unique, ref: - meter_readings.id]
  billing_month int [not null]
  billing_year int [not null]
  due_date date [not null, note: 'Default: today + 15 days']
  consumption decimal(14,2) [not null]
  tariff_amount decimal(14,2) [not null]
  service_charge_amount decimal(14,2) [not null]
  tax_amount decimal(14,2) [not null]
  penalty_amount decimal(14,2) [not null]
  total_amount decimal(14,2) [not null]
  amount_paid decimal(14,2) [not null, default: 0]
  outstanding_balance decimal(14,2) [not null]
  status varchar(30) [not null, note: 'PENDING_APPROVAL | APPROVED | PARTIALLY_PAID | PAID']
  approved_by bigint [ref: > user_accounts.id]
  approved_at timestamp
  created_at timestamp [not null]
  updated_at timestamp [not null]

  indexes {
    (meter_id, billing_month, billing_year) [unique, name: 'uq_bill_meter_period']
  }

  Note: 'INSERT trigger creates BILL_GENERATED notification'
}

Table payments {
  id bigint [pk, increment]
  bill_id bigint [not null, ref: > bills.id]
  amount_paid decimal(14,2) [not null]
  payment_method varchar(30) [not null, note: 'CASH | MOBILE_MONEY | BANK_TRANSFER']
  payment_date date [not null, note: 'Cannot be in the future']
  recorded_by bigint [not null, ref: > user_accounts.id]
  approved_by bigint [not null, ref: > user_accounts.id]
  approved_at timestamp [not null]
  created_at timestamp [not null]
  updated_at timestamp [not null]

  Note: 'INSERT trigger marks bill PAID and creates PAYMENT_COMPLETED notification when balance = 0'
}

// ─── Prepaid & Notifications ────────────────────────────────────────────────

Table prepaid_transactions {
  id bigint [pk, increment]
  meter_id bigint [not null, ref: > meters.id, note: 'ELECTRICITY meters only']
  customer_id bigint [not null, ref: > customers.id]
  amount decimal(14,2) [not null]
  token varchar(60) [not null, note: 'Generated prepaid token']
  units_purchased decimal(14,2) [not null]
  payment_method varchar(30) [not null]
  transaction_date date [not null]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table notifications {
  id bigint [pk, increment]
  customer_id bigint [not null, ref: > customers.id]
  bill_id bigint [ref: > bills.id, note: 'NULL for ACCOUNT_ACTIVATED']
  message varchar(500) [not null]
  type varchar(30) [not null, note: 'BILL_GENERATED | PAYMENT_COMPLETED | ACCOUNT_ACTIVATED | GENERAL']
  status varchar(20) [not null, note: 'PENDING | SENT | FAILED']
  sent_at timestamp
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

// ─── Relationship Notes ─────────────────────────────────────────────────────

TableGroup auth {
  user_accounts
  otp_verifications
}

TableGroup customer_domain {
  customers
  meters
  meter_readings
}

TableGroup billing_config {
  tariffs
  tariff_tiers
  taxes
  penalties
  service_charges
}

TableGroup billing_ops {
  bills
  payments
  notifications
  prepaid_transactions
}
```

---

## Table summary

| Table | Purpose |
|-------|---------|
| `user_accounts` | Login, JWT roles, account status |
| `otp_verifications` | Registration & password-reset OTPs |
| `customers` | Business customer profile linked to user |
| `meters` | Water/electricity meters per customer |
| `meter_readings` | Monthly consumption readings |
| `tariffs` | Versioned consumption pricing |
| `tariff_tiers` | Tiered pricing bands |
| `taxes` | VAT and other tax percentages |
| `penalties` | Late payment penalty config |
| `service_charges` | Fixed monthly charges per meter type |
| `bills` | Generated monthly utility bills |
| `payments` | Partial and full bill payments |
| `prepaid_transactions` | Legacy electricity prepaid top-ups |
| `notifications` | Automated customer messages + email status |

---

## PostgreSQL triggers (not shown in diagram)

| Trigger | Event | Action |
|---------|-------|--------|
| `after_bill_insert_trigger` | INSERT on `bills` | Insert `BILL_GENERATED` notification (PENDING) |
| `after_payment_insert_trigger` | INSERT on `payments` | If balance ≤ 0: set bill PAID + insert `PAYMENT_COMPLETED` notification |

Defined in: `src/main/resources/db/routines.sql`

---

## Useful verification queries

```sql
-- All users and roles
SELECT id, full_names, email, role, status FROM user_accounts ORDER BY id;

-- Customer linked to login user
SELECT c.id, c.full_names, c.email, c.status, c.owner_user_id, u.email AS login_email
FROM customers c
LEFT JOIN user_accounts u ON u.id = c.owner_user_id;

-- Full billing chain
SELECT b.bill_reference, b.status, b.total_amount, b.outstanding_balance,
       c.full_names, m.meter_number
FROM bills b
JOIN customers c ON c.id = b.customer_id
JOIN meters m ON m.id = b.meter_id
ORDER BY b.id;

-- Notifications and email status
SELECT id, customer_id, type, status, sent_at, LEFT(message, 80) AS preview
FROM notifications ORDER BY id;
```
