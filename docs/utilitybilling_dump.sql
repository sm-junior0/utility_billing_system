--
-- PostgreSQL database dump
--

\restrict opQTspn99GaSkBYj9gzNJs7PRZyzDp2Jjh0OucDQARPTS26mYBldGMz7irVHfWe

-- Dumped from database version 18.3
-- Dumped by pg_dump version 18.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: approve_bill(bigint, bigint); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.approve_bill(IN p_bill_id bigint, IN p_approver_id bigint)
    LANGUAGE plpgsql
    AS $$ BEGIN UPDATE bills SET status = 'APPROVED', approved_by = p_approver_id, approved_at = NOW(), updated_at = NOW() WHERE id = p_bill_id AND status = 'PENDING_APPROVAL'; END; $$;


--
-- Name: cursor_apply_late_penalties(); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.cursor_apply_late_penalties()
    LANGUAGE plpgsql
    AS $$ DECLARE bill_cursor CURSOR FOR SELECT b.id, b.outstanding_balance, p.percentage FROM bills b CROSS JOIN penalties p WHERE p.active = TRUE AND b.status IN ('APPROVED', 'PARTIALLY_PAID') AND b.due_date < CURRENT_DATE; v_bill_id BIGINT; v_balance NUMERIC(14, 2); v_percentage NUMERIC(6, 2); v_penalty NUMERIC(14, 2); BEGIN OPEN bill_cursor; LOOP FETCH bill_cursor INTO v_bill_id, v_balance, v_percentage; EXIT WHEN NOT FOUND; v_penalty := ROUND((v_balance * v_percentage) / 100, 2); UPDATE bills SET penalty_amount = v_penalty, total_amount = tariff_amount + service_charge_amount + tax_amount + v_penalty, outstanding_balance = (tariff_amount + service_charge_amount + tax_amount + v_penalty) - amount_paid, status = 'OVERDUE', updated_at = NOW() WHERE id = v_bill_id; END LOOP; CLOSE bill_cursor; END; $$;


--
-- Name: generate_monthly_bill(bigint, bigint, bigint); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.generate_monthly_bill(IN p_customer_id bigint, IN p_meter_id bigint, IN p_reading_id bigint)
    LANGUAGE plpgsql
    AS $$ DECLARE v_reading RECORD; v_tariff RECORD; v_service_charge RECORD; v_tax RECORD; v_total NUMERIC(14, 2); v_tariff_amount NUMERIC(14, 2); v_tax_amount NUMERIC(14, 2); BEGIN SELECT * INTO v_reading FROM meter_readings WHERE id = p_reading_id AND meter_id = p_meter_id; SELECT * INTO v_tariff FROM tariffs WHERE meter_type = (SELECT meter_type FROM meters WHERE id = p_meter_id) AND active = TRUE AND effective_from <= v_reading.reading_date ORDER BY version_number DESC LIMIT 1; SELECT * INTO v_service_charge FROM service_charges WHERE meter_type = (SELECT meter_type FROM meters WHERE id = p_meter_id) AND active = TRUE ORDER BY id DESC LIMIT 1; SELECT * INTO v_tax FROM taxes WHERE active = TRUE ORDER BY id DESC LIMIT 1; v_tariff_amount := ROUND(v_reading.consumption * COALESCE(v_tariff.flat_unit_price, 0), 2); v_tax_amount := ROUND((v_tariff_amount + v_service_charge.amount) * v_tax.percentage / 100, 2); v_total := v_tariff_amount + v_service_charge.amount + v_tax_amount; INSERT INTO bills ( bill_reference, customer_id, meter_id, reading_id, billing_month, billing_year, due_date, consumption, tariff_amount, service_charge_amount, tax_amount, penalty_amount, total_amount, amount_paid, outstanding_balance, status, created_at, updated_at ) VALUES ( 'BILL-' || v_reading.reading_year || LPAD(v_reading.reading_month::TEXT, 2, '0') || '-' || SUBSTRING(MD5(RANDOM()::TEXT), 1, 8), p_customer_id, p_meter_id, p_reading_id, v_reading.reading_month, v_reading.reading_year, CURRENT_DATE + INTERVAL '15 day', v_reading.consumption, v_tariff_amount, v_service_charge.amount, v_tax_amount, 0, v_total, 0, v_total, 'PENDING_APPROVAL', NOW(), NOW() ); END; $$;


--
-- Name: insert_bill_notification(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.insert_bill_notification() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE customer_name TEXT; BEGIN SELECT full_names INTO customer_name FROM customers WHERE id = NEW.customer_id; INSERT INTO notifications (customer_id, bill_id, message, type, status, created_at, updated_at) VALUES ( NEW.customer_id, NEW.id, 'Dear ' || customer_name || E',\nYour ' || LPAD(NEW.billing_month::TEXT, 2, '0') || '/' || NEW.billing_year::TEXT || ' utility bill of ' || NEW.total_amount || ' FRW has been successfully processed.', 'BILL_GENERATED', 'PENDING', NOW(), NOW() ); RETURN NEW; END; $$;


--
-- Name: record_payment(bigint, numeric, character varying, bigint); Type: PROCEDURE; Schema: public; Owner: -
--

CREATE PROCEDURE public.record_payment(IN p_bill_id bigint, IN p_amount numeric, IN p_method character varying, IN p_finance_user_id bigint)
    LANGUAGE plpgsql
    AS $$ BEGIN UPDATE bills SET amount_paid = amount_paid + p_amount, outstanding_balance = outstanding_balance - p_amount, status = CASE WHEN outstanding_balance - p_amount <= 0 THEN 'PAID' ELSE 'PARTIALLY_PAID' END, updated_at = NOW() WHERE id = p_bill_id; INSERT INTO payments (bill_id, amount_paid, payment_method, payment_date, recorded_by, approved_by, approved_at, created_at, updated_at) VALUES (p_bill_id, p_amount, p_method, CURRENT_DATE, p_finance_user_id, p_finance_user_id, NOW(), NOW(), NOW()); END; $$;


--
-- Name: update_bill_after_payment(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.update_bill_after_payment() RETURNS trigger
    LANGUAGE plpgsql
    AS $$ DECLARE v_current_balance NUMERIC(14, 2); v_customer_name TEXT; v_bill_month INTEGER; v_bill_year INTEGER; v_total_amount NUMERIC(14, 2); v_customer_id BIGINT; BEGIN SELECT b.outstanding_balance, b.billing_month, b.billing_year, b.total_amount, c.full_names, b.customer_id INTO v_current_balance, v_bill_month, v_bill_year, v_total_amount, v_customer_name, v_customer_id FROM bills b JOIN customers c ON c.id = b.customer_id WHERE b.id = NEW.bill_id; IF v_current_balance <= 0 THEN UPDATE bills SET status = 'PAID', updated_at = NOW() WHERE id = NEW.bill_id; INSERT INTO notifications (customer_id, bill_id, message, type, status, created_at, updated_at) VALUES ( v_customer_id, NEW.bill_id, 'Dear ' || v_customer_name || E',\nYour ' || LPAD(v_bill_month::TEXT, 2, '0') || '/' || v_bill_year::TEXT || ' utility bill of ' || v_total_amount || ' FRW has been successfully paid.', 'PAYMENT_COMPLETED', 'PENDING', NOW(), NOW() ); END IF; RETURN NEW; END; $$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: bills; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.bills (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    amount_paid numeric(14,2) NOT NULL,
    approved_at timestamp(6) without time zone,
    bill_reference character varying(50) NOT NULL,
    billing_month integer NOT NULL,
    billing_year integer NOT NULL,
    consumption numeric(14,2) NOT NULL,
    due_date date NOT NULL,
    outstanding_balance numeric(14,2) NOT NULL,
    penalty_amount numeric(14,2) NOT NULL,
    service_charge_amount numeric(14,2) NOT NULL,
    status character varying(30) NOT NULL,
    tariff_amount numeric(14,2) NOT NULL,
    tax_amount numeric(14,2) NOT NULL,
    total_amount numeric(14,2) NOT NULL,
    approved_by bigint,
    customer_id bigint NOT NULL,
    meter_id bigint NOT NULL,
    reading_id bigint NOT NULL,
    CONSTRAINT bills_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'PARTIALLY_PAID'::character varying, 'PAID'::character varying, 'OVERDUE'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: bills_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.bills ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.bills_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: customers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.customers (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    address character varying(255) NOT NULL,
    email character varying(120) NOT NULL,
    full_names character varying(120) NOT NULL,
    national_id character varying(16) NOT NULL,
    phone_number character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    owner_user_id bigint,
    CONSTRAINT customers_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: customers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.customers ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.customers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: meter_readings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meter_readings (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    consumption numeric(14,2) NOT NULL,
    current_reading numeric(14,2) NOT NULL,
    previous_reading numeric(14,2) NOT NULL,
    reading_date date NOT NULL,
    reading_month integer NOT NULL,
    reading_year integer NOT NULL,
    captured_by bigint NOT NULL,
    meter_id bigint NOT NULL
);


--
-- Name: meter_readings_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.meter_readings ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.meter_readings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: meters; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.meters (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    installation_date date NOT NULL,
    meter_number character varying(50) NOT NULL,
    meter_type character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    customer_id bigint NOT NULL,
    CONSTRAINT meters_meter_type_check CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[]))),
    CONSTRAINT meters_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: meters_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.meters ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.meters_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    message character varying(500) NOT NULL,
    sent_at timestamp(6) without time zone,
    status character varying(20) NOT NULL,
    type character varying(30) NOT NULL,
    bill_id bigint,
    customer_id bigint NOT NULL,
    CONSTRAINT notifications_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'SENT'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['BILL_GENERATED'::character varying, 'PAYMENT_COMPLETED'::character varying, 'GENERAL'::character varying])::text[])))
);


--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.notifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: otp_verifications; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.otp_verifications (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email character varying(120) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    otp_code character varying(6) NOT NULL,
    used boolean NOT NULL
);


--
-- Name: otp_verifications_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.otp_verifications ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.otp_verifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: payments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.payments (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    amount_paid numeric(14,2) NOT NULL,
    approved_at timestamp(6) without time zone NOT NULL,
    payment_date date NOT NULL,
    payment_method character varying(30) NOT NULL,
    approved_by bigint NOT NULL,
    bill_id bigint NOT NULL,
    recorded_by bigint NOT NULL,
    CONSTRAINT payments_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'MOBILE_MONEY'::character varying, 'BANK_TRANSFER'::character varying, 'CARD'::character varying])::text[])))
);


--
-- Name: payments_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.payments ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.payments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: penalties; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.penalties (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    active boolean NOT NULL,
    name character varying(100) NOT NULL,
    percentage numeric(6,2) NOT NULL
);


--
-- Name: penalties_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.penalties ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.penalties_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: prepaid_transactions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.prepaid_transactions (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    amount numeric(14,2) NOT NULL,
    payment_method character varying(30) NOT NULL,
    token character varying(60) NOT NULL,
    transaction_date date NOT NULL,
    units_purchased numeric(14,2) NOT NULL,
    customer_id bigint NOT NULL,
    meter_id bigint NOT NULL,
    CONSTRAINT prepaid_transactions_payment_method_check CHECK (((payment_method)::text = ANY ((ARRAY['CASH'::character varying, 'MOBILE_MONEY'::character varying, 'BANK_TRANSFER'::character varying, 'CARD'::character varying])::text[])))
);


--
-- Name: prepaid_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.prepaid_transactions ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.prepaid_transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: service_charges; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.service_charges (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    active boolean NOT NULL,
    amount numeric(14,2) NOT NULL,
    meter_type character varying(20) NOT NULL,
    CONSTRAINT service_charges_meter_type_check CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[])))
);


--
-- Name: service_charges_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.service_charges ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.service_charges_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tariff_tiers; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tariff_tiers (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    max_units numeric(14,2),
    min_units numeric(14,2) NOT NULL,
    price_per_unit numeric(14,2) NOT NULL,
    tariff_id bigint NOT NULL
);


--
-- Name: tariff_tiers_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tariff_tiers ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tariff_tiers_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: tariffs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tariffs (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    active boolean NOT NULL,
    effective_from date NOT NULL,
    effective_to date,
    flat_unit_price numeric(14,2),
    meter_type character varying(20) NOT NULL,
    tariff_type character varying(20) NOT NULL,
    version_number integer NOT NULL,
    CONSTRAINT tariffs_meter_type_check CHECK (((meter_type)::text = ANY ((ARRAY['WATER'::character varying, 'ELECTRICITY'::character varying])::text[]))),
    CONSTRAINT tariffs_tariff_type_check CHECK (((tariff_type)::text = ANY ((ARRAY['FLAT'::character varying, 'TIERED'::character varying])::text[])))
);


--
-- Name: tariffs_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.tariffs ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.tariffs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: taxes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.taxes (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    active boolean NOT NULL,
    name character varying(100) NOT NULL,
    percentage numeric(6,2) NOT NULL
);


--
-- Name: taxes_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.taxes ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.taxes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: user_accounts; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_accounts (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    email character varying(120) NOT NULL,
    full_names character varying(120) NOT NULL,
    password character varying(255) NOT NULL,
    phone_number character varying(20) NOT NULL,
    role character varying(30) NOT NULL,
    status character varying(20) NOT NULL,
    CONSTRAINT user_accounts_role_check CHECK (((role)::text = ANY ((ARRAY['ROLE_ADMIN'::character varying, 'ROLE_OPERATOR'::character varying, 'ROLE_FINANCE'::character varying, 'ROLE_CUSTOMER'::character varying])::text[]))),
    CONSTRAINT user_accounts_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'PENDING_VERIFICATION'::character varying, 'INACTIVE'::character varying])::text[])))
);


--
-- Name: user_accounts_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.user_accounts ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.user_accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Data for Name: bills; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.bills (id, created_at, updated_at, amount_paid, approved_at, bill_reference, billing_month, billing_year, consumption, due_date, outstanding_balance, penalty_amount, service_charge_amount, status, tariff_amount, tax_amount, total_amount, approved_by, customer_id, meter_id, reading_id) FROM stdin;
1	2026-06-05 12:42:05.463709	2026-06-05 13:02:14.30747	14632.00	2026-06-05 12:46:27.596197	BILL-202606-2B6360C6	6	2026	35.00	2026-06-20	0.00	0.00	500.00	PAID	11900.00	2232.00	14632.00	4	1	1	1
2	2026-06-05 12:56:15.200361	2026-06-05 13:17:01.777224	20414.00	2026-06-05 12:56:48.899882	BILL-202607-5E3C0AD7	7	2026	120.00	2026-06-20	0.00	0.00	800.00	PAID	16500.00	3114.00	20414.00	4	1	2	2
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.customers (id, created_at, updated_at, address, email, full_names, national_id, phone_number, status, owner_user_id) FROM stdin;
1	2026-06-05 12:27:23.690524	2026-06-05 12:27:23.690524	Kigali, Gasabo	jj3032205@gmail.com	Alice Uwase	1199912345678901	250788123456	ACTIVE	2
\.


--
-- Data for Name: meter_readings; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.meter_readings (id, created_at, updated_at, consumption, current_reading, previous_reading, reading_date, reading_month, reading_year, captured_by, meter_id) FROM stdin;
1	2026-06-05 12:37:21.68769	2026-06-05 12:37:21.68769	35.00	135.00	100.00	2026-06-01	6	2026	3	1
2	2026-06-05 12:55:04.800905	2026-06-05 12:55:04.800905	120.00	120.00	0.00	2026-07-01	7	2026	3	2
\.


--
-- Data for Name: meters; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.meters (id, created_at, updated_at, installation_date, meter_number, meter_type, status, customer_id) FROM stdin;
1	2026-06-05 12:29:44.435013	2026-06-05 12:29:44.435013	2026-01-10	WTR-0001	WATER	ACTIVE	1
2	2026-06-05 12:30:32.311949	2026-06-05 12:30:32.311949	2026-01-10	ELC-0001	ELECTRICITY	ACTIVE	1
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.notifications (id, created_at, updated_at, message, sent_at, status, type, bill_id, customer_id) FROM stdin;
2	2026-06-05 12:56:15.142563	2026-06-05 12:56:19.959392	Dear Alice Uwase,\nYour 07/2026 utility bill of 20414.00 FRW has been successfully processed.	2026-06-05 12:56:19.958308	SENT	BILL_GENERATED	2	1
1	2026-06-05 12:42:05.404948	2026-06-05 13:04:51.887274	Dear Alice Uwase,\nYour 06/2026 utility bill of 14632.00 FRW has been successfully processed.	2026-06-05 13:04:51.886256	SENT	BILL_GENERATED	1	1
3	2026-06-05 13:17:01.777224	2026-06-05 13:31:46.957265	Dear Alice Uwase,\nYour 07/2026 utility bill of 20414.00 FRW has been successfully paid.	2026-06-05 13:17:07.479755	SENT	PAYMENT_COMPLETED	2	1
\.


--
-- Data for Name: otp_verifications; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.otp_verifications (id, created_at, email, expires_at, otp_code, used) FROM stdin;
1	2026-06-05 12:20:46.539401	jj3032205@gmail.com	2026-06-05 12:30:46.538382	800458	t
2	2026-06-05 12:22:23.927674	operator@utility.rw	2026-06-05 12:32:23.927674	240247	t
3	2026-06-05 12:22:39.222198	juniormanene@gmail.com	2026-06-05 12:32:39.222198	380757	t
\.


--
-- Data for Name: payments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.payments (id, created_at, updated_at, amount_paid, approved_at, payment_date, payment_method, approved_by, bill_id, recorded_by) FROM stdin;
2	2026-06-05 12:59:24.67217	2026-06-05 12:59:24.67217	5000.00	2026-06-05 00:00:00	2026-06-05	MOBILE_MONEY	4	1	4
3	2026-06-05 13:02:14.305832	2026-06-05 13:02:14.305832	9632.00	2026-06-05 00:00:00	2026-06-05	MOBILE_MONEY	4	1	4
4	2026-06-05 13:17:01.788898	2026-06-05 13:17:01.788898	20414.00	2026-06-05 00:00:00	2026-06-05	MOBILE_MONEY	1	2	1
\.


--
-- Data for Name: penalties; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.penalties (id, created_at, updated_at, active, name, percentage) FROM stdin;
1	2026-06-05 12:35:15.262456	2026-06-05 12:35:15.262456	t	Late Payment Penalty	5.00
\.


--
-- Data for Name: prepaid_transactions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.prepaid_transactions (id, created_at, updated_at, amount, payment_method, token, transaction_date, units_purchased, customer_id, meter_id) FROM stdin;
\.


--
-- Data for Name: service_charges; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.service_charges (id, created_at, updated_at, active, amount, meter_type) FROM stdin;
1	2026-06-05 12:34:38.912482	2026-06-05 12:34:38.912482	t	500.00	WATER
2	2026-06-05 12:34:45.440629	2026-06-05 12:34:45.440629	t	800.00	ELECTRICITY
\.


--
-- Data for Name: tariff_tiers; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tariff_tiers (id, created_at, updated_at, max_units, min_units, price_per_unit, tariff_id) FROM stdin;
1	2026-06-05 12:33:46.041395	2026-06-05 12:33:46.041395	50.00	0.00	100.00	2
2	2026-06-05 12:33:46.044509	2026-06-05 12:33:46.044509	100.00	50.00	150.00	2
3	2026-06-05 12:33:46.045452	2026-06-05 12:33:46.045452	\N	100.00	200.00	2
\.


--
-- Data for Name: tariffs; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tariffs (id, created_at, updated_at, active, effective_from, effective_to, flat_unit_price, meter_type, tariff_type, version_number) FROM stdin;
1	2026-06-05 12:33:18.38293	2026-06-05 12:33:18.38293	t	2026-01-01	\N	340.00	WATER	FLAT	1
2	2026-06-05 12:33:46.03939	2026-06-05 12:33:46.03939	t	2026-01-01	\N	\N	ELECTRICITY	TIERED	1
\.


--
-- Data for Name: taxes; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.taxes (id, created_at, updated_at, active, name, percentage) FROM stdin;
1	2026-06-05 12:34:07.889706	2026-06-05 12:34:07.889706	t	VAT	18.00
\.


--
-- Data for Name: user_accounts; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.user_accounts (id, created_at, updated_at, email, full_names, password, phone_number, role, status) FROM stdin;
1	2026-06-05 12:19:29.757941	2026-06-05 12:19:29.757941	admin@utility.rw	System Administrator	$2a$10$RiwJHCIzvHtu2kzTMa7sfuQU7Sdwriyac/AFRbEG2viXwzidbJXLu	250788000001	ROLE_ADMIN	ACTIVE
2	2026-06-05 12:20:46.515292	2026-06-05 12:23:29.905921	jj3032205@gmail.com	Alice Uwase	$2a$10$SPZGb5PChboWnpsr.GWvgeqJX9EV43Zs/O.W16vGd64krL/gdbfty	250788123456	ROLE_CUSTOMER	ACTIVE
3	2026-06-05 12:22:23.922703	2026-06-05 12:26:08.504411	operator@utility.rw	Eric Operator	$2a$10$Q9dnFWq7mKObXrqh3E6ERuoC8/tU58XildeFFummECOV6ywIGp8Ui	250788111111	ROLE_OPERATOR	ACTIVE
4	2026-06-05 12:22:39.218031	2026-06-05 12:26:32.064544	juniormanene@gmail.com	Fiona Finance	$2a$10$FCwckZA8NNDzGRN8.wIv8O1hpZ6MT7.a/yYRasde.19IO80zr4h.S	250788222222	ROLE_FINANCE	ACTIVE
\.


--
-- Name: bills_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.bills_id_seq', 2, true);


--
-- Name: customers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.customers_id_seq', 1, true);


--
-- Name: meter_readings_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.meter_readings_id_seq', 2, true);


--
-- Name: meters_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.meters_id_seq', 2, true);


--
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.notifications_id_seq', 3, true);


--
-- Name: otp_verifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.otp_verifications_id_seq', 3, true);


--
-- Name: payments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.payments_id_seq', 4, true);


--
-- Name: penalties_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.penalties_id_seq', 1, true);


--
-- Name: prepaid_transactions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.prepaid_transactions_id_seq', 1, false);


--
-- Name: service_charges_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.service_charges_id_seq', 2, true);


--
-- Name: tariff_tiers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tariff_tiers_id_seq', 3, true);


--
-- Name: tariffs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tariffs_id_seq', 2, true);


--
-- Name: taxes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.taxes_id_seq', 1, true);


--
-- Name: user_accounts_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.user_accounts_id_seq', 4, true);


--
-- Name: bills bills_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT bills_pkey PRIMARY KEY (id);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: meter_readings meter_readings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT meter_readings_pkey PRIMARY KEY (id);


--
-- Name: meters meters_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT meters_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: otp_verifications otp_verifications_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.otp_verifications
    ADD CONSTRAINT otp_verifications_pkey PRIMARY KEY (id);


--
-- Name: payments payments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT payments_pkey PRIMARY KEY (id);


--
-- Name: penalties penalties_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.penalties
    ADD CONSTRAINT penalties_pkey PRIMARY KEY (id);


--
-- Name: prepaid_transactions prepaid_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prepaid_transactions
    ADD CONSTRAINT prepaid_transactions_pkey PRIMARY KEY (id);


--
-- Name: service_charges service_charges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.service_charges
    ADD CONSTRAINT service_charges_pkey PRIMARY KEY (id);


--
-- Name: tariff_tiers tariff_tiers_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariff_tiers
    ADD CONSTRAINT tariff_tiers_pkey PRIMARY KEY (id);


--
-- Name: tariffs tariffs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariffs
    ADD CONSTRAINT tariffs_pkey PRIMARY KEY (id);


--
-- Name: taxes taxes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.taxes
    ADD CONSTRAINT taxes_pkey PRIMARY KEY (id);


--
-- Name: bills uk_bill_meter_period; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT uk_bill_meter_period UNIQUE (meter_id, billing_month, billing_year);


--
-- Name: bills uk_bill_reading; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT uk_bill_reading UNIQUE (reading_id);


--
-- Name: bills uk_bill_reference; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT uk_bill_reference UNIQUE (bill_reference);


--
-- Name: customers uk_customer_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT uk_customer_email UNIQUE (email);


--
-- Name: customers uk_customer_national_id; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT uk_customer_national_id UNIQUE (national_id);


--
-- Name: customers uk_customer_owner_user; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT uk_customer_owner_user UNIQUE (owner_user_id);


--
-- Name: customers uk_customer_phone; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT uk_customer_phone UNIQUE (phone_number);


--
-- Name: meter_readings uk_meter_month_year; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT uk_meter_month_year UNIQUE (meter_id, reading_month, reading_year);


--
-- Name: meters uk_meter_number; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT uk_meter_number UNIQUE (meter_number);


--
-- Name: user_accounts uk_user_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_accounts
    ADD CONSTRAINT uk_user_email UNIQUE (email);


--
-- Name: user_accounts uk_user_phone; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_accounts
    ADD CONSTRAINT uk_user_phone UNIQUE (phone_number);


--
-- Name: user_accounts user_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_accounts
    ADD CONSTRAINT user_accounts_pkey PRIMARY KEY (id);


--
-- Name: bills after_bill_insert_trigger; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER after_bill_insert_trigger AFTER INSERT ON public.bills FOR EACH ROW EXECUTE FUNCTION public.insert_bill_notification();


--
-- Name: payments after_payment_insert_trigger; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER after_payment_insert_trigger AFTER INSERT ON public.payments FOR EACH ROW EXECUTE FUNCTION public.update_bill_after_payment();


--
-- Name: bills fk1rtnpgvhs9hqthvu89tl36fe7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fk1rtnpgvhs9hqthvu89tl36fe7 FOREIGN KEY (reading_id) REFERENCES public.meter_readings(id);


--
-- Name: notifications fk28wkse7ifkf33ani76n85rglf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk28wkse7ifkf33ani76n85rglf FOREIGN KEY (bill_id) REFERENCES public.bills(id);


--
-- Name: payments fk2f9up2bpr4gpfq6plo8oumso2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk2f9up2bpr4gpfq6plo8oumso2 FOREIGN KEY (approved_by) REFERENCES public.user_accounts(id);


--
-- Name: notifications fk30dp6ycner3dgso3scgc9vghy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fk30dp6ycner3dgso3scgc9vghy FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: prepaid_transactions fk3bnjqcn0q2jn6835emi2mhjc7; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prepaid_transactions
    ADD CONSTRAINT fk3bnjqcn0q2jn6835emi2mhjc7 FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: tariff_tiers fk90xk77nst2f05cmrod4d5q42h; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tariff_tiers
    ADD CONSTRAINT fk90xk77nst2f05cmrod4d5q42h FOREIGN KEY (tariff_id) REFERENCES public.tariffs(id);


--
-- Name: payments fk9565r6579khpdjxnyla0l2ycd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fk9565r6579khpdjxnyla0l2ycd FOREIGN KEY (bill_id) REFERENCES public.bills(id);


--
-- Name: prepaid_transactions fkaphtrchmeseirm8t2gljngaad; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.prepaid_transactions
    ADD CONSTRAINT fkaphtrchmeseirm8t2gljngaad FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: customers fkbu5gpcg5xuvpnq3ymknu3i0ir; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT fkbu5gpcg5xuvpnq3ymknu3i0ir FOREIGN KEY (owner_user_id) REFERENCES public.user_accounts(id);


--
-- Name: meters fkdgg79dhtsr0eumbce7ipw58lj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meters
    ADD CONSTRAINT fkdgg79dhtsr0eumbce7ipw58lj FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: bills fkfes5685l6y4urtsc0cq3cobo1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkfes5685l6y4urtsc0cq3cobo1 FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: bills fkgi58kf78p9b9o4wvrb9phqxc6; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkgi58kf78p9b9o4wvrb9phqxc6 FOREIGN KEY (approved_by) REFERENCES public.user_accounts(id);


--
-- Name: payments fkl7dyk9cddc6wgom3h5po56ij0; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.payments
    ADD CONSTRAINT fkl7dyk9cddc6wgom3h5po56ij0 FOREIGN KEY (recorded_by) REFERENCES public.user_accounts(id);


--
-- Name: meter_readings fknalaulqjlf29g1dlukdeyg0g4; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT fknalaulqjlf29g1dlukdeyg0g4 FOREIGN KEY (meter_id) REFERENCES public.meters(id);


--
-- Name: bills fkoy9sc2dmxj2qwjeiiilf3yuxp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.bills
    ADD CONSTRAINT fkoy9sc2dmxj2qwjeiiilf3yuxp FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: meter_readings fkqjupbvijj7hdbwotpkwh2drba; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.meter_readings
    ADD CONSTRAINT fkqjupbvijj7hdbwotpkwh2drba FOREIGN KEY (captured_by) REFERENCES public.user_accounts(id);


--
-- PostgreSQL database dump complete
--

\unrestrict opQTspn99GaSkBYj9gzNJs7PRZyzDp2Jjh0OucDQARPTS26mYBldGMz7irVHfWe

