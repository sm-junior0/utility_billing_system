-- PostgreSQL triggers, functions, and stored procedures for bill notifications and billing workflows.

CREATE OR REPLACE FUNCTION insert_bill_notification()
RETURNS TRIGGER AS $$
DECLARE
    customer_name TEXT;
BEGIN
    SELECT full_names INTO customer_name FROM customers WHERE id = NEW.customer_id;

    INSERT INTO notifications (customer_id, bill_id, message, type, status, created_at, updated_at)
    VALUES (
        NEW.customer_id,
        NEW.id,
        'Dear ' || customer_name || E',\nYour ' ||
        LPAD(NEW.billing_month::TEXT, 2, '0') || '/' || NEW.billing_year::TEXT ||
        ' utility bill of ' || NEW.total_amount || ' FRW has been successfully processed.',
        'BILL_GENERATED',
        'PENDING',
        NOW(),
        NOW()
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
@@

DROP TRIGGER IF EXISTS after_bill_insert_trigger ON bills;
@@
CREATE TRIGGER after_bill_insert_trigger
AFTER INSERT ON bills
FOR EACH ROW
EXECUTE FUNCTION insert_bill_notification();
@@

CREATE OR REPLACE FUNCTION update_bill_after_payment()
RETURNS TRIGGER AS $$
DECLARE
    v_current_balance NUMERIC(14, 2);
    v_customer_name TEXT;
    v_bill_month INTEGER;
    v_bill_year INTEGER;
    v_total_amount NUMERIC(14, 2);
    v_customer_id BIGINT;
BEGIN
    SELECT b.outstanding_balance, b.billing_month, b.billing_year, b.total_amount, c.full_names, b.customer_id
    INTO v_current_balance, v_bill_month, v_bill_year, v_total_amount, v_customer_name, v_customer_id
    FROM bills b
    JOIN customers c ON c.id = b.customer_id
    WHERE b.id = NEW.bill_id;

    IF v_current_balance <= 0 THEN
        UPDATE bills
        SET status = 'PAID', updated_at = NOW()
        WHERE id = NEW.bill_id;

        INSERT INTO notifications (customer_id, bill_id, message, type, status, created_at, updated_at)
        VALUES (
            v_customer_id,
            NEW.bill_id,
            'Dear ' || v_customer_name || E',\nYour ' ||
            LPAD(v_bill_month::TEXT, 2, '0') || '/' || v_bill_year::TEXT ||
            ' utility bill of ' || v_total_amount || ' FRW has been successfully paid.',
            'PAYMENT_COMPLETED',
            'PENDING',
            NOW(),
            NOW()
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
@@

DROP TRIGGER IF EXISTS after_payment_insert_trigger ON payments;
@@
CREATE TRIGGER after_payment_insert_trigger
AFTER INSERT ON payments
FOR EACH ROW
EXECUTE FUNCTION update_bill_after_payment();
@@

CREATE OR REPLACE PROCEDURE approve_bill(IN p_bill_id BIGINT, IN p_approver_id BIGINT)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE bills
    SET status = 'APPROVED',
        approved_by = p_approver_id,
        approved_at = NOW(),
        updated_at = NOW()
    WHERE id = p_bill_id
      AND status = 'PENDING_APPROVAL';
END;
$$;
@@

CREATE OR REPLACE PROCEDURE record_payment(
    IN p_bill_id BIGINT,
    IN p_amount NUMERIC(14, 2),
    IN p_method VARCHAR(30),
    IN p_finance_user_id BIGINT
)
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE bills
    SET amount_paid = amount_paid + p_amount,
        outstanding_balance = outstanding_balance - p_amount,
        status = CASE
            WHEN outstanding_balance - p_amount <= 0 THEN 'PAID'
            ELSE 'PARTIALLY_PAID'
        END,
        updated_at = NOW()
    WHERE id = p_bill_id;

    INSERT INTO payments (bill_id, amount_paid, payment_method, payment_date, recorded_by, approved_by, approved_at, created_at, updated_at)
    VALUES (p_bill_id, p_amount, p_method, CURRENT_DATE, p_finance_user_id, p_finance_user_id, NOW(), NOW(), NOW());
END;
$$;
@@

CREATE OR REPLACE PROCEDURE cursor_apply_late_penalties()
LANGUAGE plpgsql
AS $$
DECLARE
    bill_cursor CURSOR FOR
        SELECT b.id, b.outstanding_balance, p.percentage
        FROM bills b
        CROSS JOIN penalties p
        WHERE p.active = TRUE
          AND b.status IN ('APPROVED', 'PARTIALLY_PAID')
          AND b.due_date < CURRENT_DATE;
    v_bill_id BIGINT;
    v_balance NUMERIC(14, 2);
    v_percentage NUMERIC(6, 2);
    v_penalty NUMERIC(14, 2);
BEGIN
    OPEN bill_cursor;
    LOOP
        FETCH bill_cursor INTO v_bill_id, v_balance, v_percentage;
        EXIT WHEN NOT FOUND;

        v_penalty := ROUND((v_balance * v_percentage) / 100, 2);
        UPDATE bills
        SET penalty_amount = v_penalty,
            total_amount = tariff_amount + service_charge_amount + tax_amount + v_penalty,
            outstanding_balance = (tariff_amount + service_charge_amount + tax_amount + v_penalty) - amount_paid,
            status = 'OVERDUE',
            updated_at = NOW()
        WHERE id = v_bill_id;
    END LOOP;
    CLOSE bill_cursor;
END;
$$;
@@

CREATE OR REPLACE PROCEDURE generate_monthly_bill(
    IN p_customer_id BIGINT,
    IN p_meter_id BIGINT,
    IN p_reading_id BIGINT
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_reading RECORD;
    v_tariff RECORD;
    v_service_charge RECORD;
    v_tax RECORD;
    v_total NUMERIC(14, 2);
    v_tariff_amount NUMERIC(14, 2);
    v_tax_amount NUMERIC(14, 2);
BEGIN
    SELECT * INTO v_reading FROM meter_readings WHERE id = p_reading_id AND meter_id = p_meter_id;
    SELECT * INTO v_tariff
    FROM tariffs
    WHERE meter_type = (SELECT meter_type FROM meters WHERE id = p_meter_id)
      AND active = TRUE
      AND effective_from <= v_reading.reading_date
    ORDER BY version_number DESC
    LIMIT 1;

    SELECT * INTO v_service_charge
    FROM service_charges
    WHERE meter_type = (SELECT meter_type FROM meters WHERE id = p_meter_id)
      AND active = TRUE
    ORDER BY id DESC
    LIMIT 1;

    SELECT * INTO v_tax FROM taxes WHERE active = TRUE ORDER BY id DESC LIMIT 1;

    v_tariff_amount := ROUND(v_reading.consumption * COALESCE(v_tariff.flat_unit_price, 0), 2);
    v_tax_amount := ROUND((v_tariff_amount + v_service_charge.amount) * v_tax.percentage / 100, 2);
    v_total := v_tariff_amount + v_service_charge.amount + v_tax_amount;

    INSERT INTO bills (
        bill_reference, customer_id, meter_id, reading_id, billing_month, billing_year,
        due_date, consumption, tariff_amount, service_charge_amount, tax_amount, penalty_amount,
        total_amount, amount_paid, outstanding_balance, status, created_at, updated_at
    )
    VALUES (
        'BILL-' || v_reading.reading_year || LPAD(v_reading.reading_month::TEXT, 2, '0') || '-' || SUBSTRING(MD5(RANDOM()::TEXT), 1, 8),
        p_customer_id,
        p_meter_id,
        p_reading_id,
        v_reading.reading_month,
        v_reading.reading_year,
        CURRENT_DATE + INTERVAL '15 day',
        v_reading.consumption,
        v_tariff_amount,
        v_service_charge.amount,
        v_tax_amount,
        0,
        v_total,
        0,
        v_total,
        'PENDING_APPROVAL',
        NOW(),
        NOW()
    );
END;
$$;
@@
