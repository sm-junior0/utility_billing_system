-- Reference schema for the Utility Billing System.
-- JPA entities generate the runtime schema, but this file documents the expected PostgreSQL design.

CREATE TABLE user_accounts (
    id BIGSERIAL PRIMARY KEY,
    full_names VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE otp_verifications (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_names VARCHAR(120) NOT NULL,
    national_id VARCHAR(16) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner_user_id BIGINT UNIQUE REFERENCES user_accounts(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE meters (
    id BIGSERIAL PRIMARY KEY,
    meter_number VARCHAR(50) NOT NULL UNIQUE,
    meter_type VARCHAR(20) NOT NULL,
    installation_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE meter_readings (
    id BIGSERIAL PRIMARY KEY,
    meter_id BIGINT NOT NULL REFERENCES meters(id),
    previous_reading NUMERIC(14,2) NOT NULL,
    current_reading NUMERIC(14,2) NOT NULL,
    consumption NUMERIC(14,2) NOT NULL,
    reading_date DATE NOT NULL,
    reading_month INTEGER NOT NULL,
    reading_year INTEGER NOT NULL,
    captured_by BIGINT NOT NULL REFERENCES user_accounts(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (meter_id, reading_month, reading_year)
);

CREATE TABLE tariffs (
    id BIGSERIAL PRIMARY KEY,
    meter_type VARCHAR(20) NOT NULL,
    tariff_type VARCHAR(20) NOT NULL,
    version_number INTEGER NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    active BOOLEAN NOT NULL,
    flat_unit_price NUMERIC(14,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE tariff_tiers (
    id BIGSERIAL PRIMARY KEY,
    tariff_id BIGINT NOT NULL REFERENCES tariffs(id),
    min_units NUMERIC(14,2) NOT NULL,
    max_units NUMERIC(14,2),
    price_per_unit NUMERIC(14,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE taxes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    percentage NUMERIC(6,2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE penalties (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    percentage NUMERIC(6,2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE service_charges (
    id BIGSERIAL PRIMARY KEY,
    meter_type VARCHAR(20) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE bills (
    id BIGSERIAL PRIMARY KEY,
    bill_reference VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    meter_id BIGINT NOT NULL REFERENCES meters(id),
    reading_id BIGINT NOT NULL UNIQUE REFERENCES meter_readings(id),
    billing_month INTEGER NOT NULL,
    billing_year INTEGER NOT NULL,
    due_date DATE NOT NULL,
    consumption NUMERIC(14,2) NOT NULL,
    tariff_amount NUMERIC(14,2) NOT NULL,
    service_charge_amount NUMERIC(14,2) NOT NULL,
    tax_amount NUMERIC(14,2) NOT NULL,
    penalty_amount NUMERIC(14,2) NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    amount_paid NUMERIC(14,2) NOT NULL,
    outstanding_balance NUMERIC(14,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    approved_by BIGINT REFERENCES user_accounts(id),
    approved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (meter_id, billing_month, billing_year)
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    bill_id BIGINT NOT NULL REFERENCES bills(id),
    amount_paid NUMERIC(14,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    payment_date DATE NOT NULL,
    recorded_by BIGINT NOT NULL REFERENCES user_accounts(id),
    approved_by BIGINT NOT NULL REFERENCES user_accounts(id),
    approved_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE prepaid_transactions (
    id BIGSERIAL PRIMARY KEY,
    meter_id BIGINT NOT NULL REFERENCES meters(id),
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    amount NUMERIC(14,2) NOT NULL,
    token VARCHAR(60) NOT NULL,
    units_purchased NUMERIC(14,2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    bill_id BIGINT REFERENCES bills(id),
    message VARCHAR(500) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
