# Utility Billing System — High-Level Architecture

Spring Boot backend for WASAC/REG integrated utility billing.  
Stack: **Spring Boot · Spring Security (JWT) · Spring Data JPA · PostgreSQL · Gmail SMTP**

---

## 1. System Context

```mermaid
flowchart TB
    subgraph Clients["Clients"]
        SW["Swagger UI\nlocalhost:8080/swagger-ui.html"]
        PM["Postman Collection"]
        FE["Future Frontend"]
    end

    subgraph App["Spring Boot Application :8080"]
        SEC["JWT Security Filter\n@PreAuthorize RBAC"]
        CTRL["REST Controllers\n13 controllers · 53 endpoints"]
        SVC["Service Layer\nBusiness rules & validation"]
        JPA["Spring Data JPA\nRepositories"]
    end

    subgraph External["External Services"]
        SMTP["Gmail SMTP\nOTP + Notification emails"]
    end

    subgraph DB["PostgreSQL — utilitybilling"]
        TBL["15 Tables"]
        TRG["Triggers\nBill & payment notifications"]
        PROC["Stored Procedures\nApprove bill · Late penalties"]
    end

    SW --> SEC
    PM --> SEC
    FE --> SEC
    SEC --> CTRL
    CTRL --> SVC
    SVC --> JPA
    SVC --> SMTP
    JPA --> TBL
    TRG --> TBL
    PROC --> TBL
```

---

## 2. Layered Architecture

```mermaid
flowchart LR
    subgraph Presentation
        C1["AuthController"]
        C2["UserController"]
        C3["CustomerController"]
        C4["MeterController"]
        C5["TariffController"]
        C6["MeterReadingController"]
        C7["BillController"]
        C8["PaymentController"]
        C9["NotificationController"]
        C10["PrepaidTransactionController"]
        C11["Tax / Penalty / ServiceCharge"]
    end

    subgraph Business
        S1["AuthService"]
        S2["CustomerService"]
        S3["MeterReadingService"]
        S4["BillService"]
        S5["PaymentService"]
        S6["NotificationService"]
        S7["OtpVerificationService"]
    end

    subgraph Persistence
        R["JPA Repositories"]
        PG["PostgreSQL"]
    end

    C1 & C2 & C3 & C4 & C5 & C6 & C7 & C8 & C9 & C10 & C11 --> S1 & S2 & S3 & S4 & S5 & S6 & S7
    S1 & S2 & S3 & S4 & S5 & S6 & S7 --> R --> PG
```

---

## 3. Role-Based Access Control

```mermaid
flowchart TD
    subgraph Public["Public — no JWT"]
        A1["POST /api/auth/signup"]
        A2["POST /api/auth/verify-otp"]
        A3["POST /api/auth/resend-otp"]
        A4["POST /api/auth/forgot-password"]
        A5["POST /api/auth/reset-password"]
        A6["POST /api/auth/login"]
    end

    subgraph Admin["ROLE_ADMIN"]
        AD1["User management"]
        AD2["Customer CRUD + approve"]
        AD3["Meter setup"]
        AD4["Tariff / Tax / Penalty / Service charges"]
        AD5["Record payments"]
    end

    subgraph Operator["ROLE_OPERATOR"]
        OP1["Capture meter readings"]
        OP2["List customers & meters"]
    end

    subgraph Finance["ROLE_FINANCE"]
        FN1["Generate & approve bills"]
        FN2["Record payments"]
        FN3["View notifications"]
        FN4["Prepaid top-up"]
    end

    subgraph Customer["ROLE_CUSTOMER"]
        CU1["Profile request"]
        CU2["View own bills / payments / meters / notifications"]
    end

    Public --> Admin & Operator & Finance & Customer
```

| Role | Key permissions |
|------|-----------------|
| **ROLE_ADMIN** | Users, roles, customers, meters, billing config, payments |
| **ROLE_OPERATOR** | Meter readings, list customers/meters |
| **ROLE_FINANCE** | Bills, payments, notifications, prepaid |
| **ROLE_CUSTOMER** | Own data only (linked via `customers.owner_user_id`) |

---

## 4. End-to-End Billing Flow

```mermaid
sequenceDiagram
    autonumber
    participant C as Customer
    participant A as Admin
    participant O as Operator
    participant F as Finance
    participant API as Spring Boot API
    participant DB as PostgreSQL
    participant Mail as Gmail SMTP

    C->>API: POST /api/auth/signup
    API->>Mail: Send registration OTP
    C->>API: POST /api/auth/verify-otp
    C->>API: POST /api/customers/profile-request
    A->>API: PUT /api/customers/{id}/approve
    API->>DB: Save ACCOUNT_ACTIVATED notification
    API->>Mail: Send activation email

    A->>API: POST /api/meters
    A->>API: POST /api/tariffs, /taxes, /penalties, /service-charges

    O->>API: POST /api/readings
    F->>API: POST /api/bills/generate
    API->>DB: Insert bill
    DB-->>DB: Trigger → BILL_GENERATED notification
    API->>Mail: Send bill email

    F->>API: POST /api/bills/{id}/approve
    F->>API: POST /api/payments (partial + full)
    DB-->>DB: Trigger → PAYMENT_COMPLETED notification
    API->>Mail: Send payment email
```

---

## 5. Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant API as AuthController
    participant OTP as OtpVerificationService
    participant Mail as Gmail SMTP
    participant DB as PostgreSQL

    U->>API: POST /api/auth/signup
    API->>DB: Create user (PENDING_VERIFICATION, ROLE_CUSTOMER)
    API->>OTP: createAndSendOtp()
    OTP->>Mail: 6-digit OTP email
    API-->>U: userId + message

    U->>API: POST /api/auth/verify-otp
    API->>DB: Set user ACTIVE
    API-->>U: Success

    U->>API: POST /api/auth/login
    API-->>U: JWT (email + role)

    Note over U,API: After admin role change → user must login again

    U->>API: POST /api/auth/forgot-password
    OTP->>Mail: Password reset OTP
    U->>API: POST /api/auth/reset-password
    API-->>U: Password updated
```

---

## 6. Customer Data Isolation

```mermaid
flowchart LR
    UA["user_accounts\nid = 2"] -->|owner_user_id| CU["customers\nid = 1"]
    CU --> M["meters"]
    CU --> B["bills"]
    CU --> P["payments via bills"]
    CU --> N["notifications"]

    JWT["Customer JWT\nemail = juniormanene@gmail.com"] --> CHECK["CurrentUserService\nvalidateCustomerOwnership()"]
    CHECK --> CU
```

Customers can only access records where `customers.owner_user_id` matches their logged-in `user_accounts.id`.

---

## 7. Database Routines Integration

```mermaid
flowchart TD
    BILL_INS["INSERT into bills"] --> TRG1["after_bill_insert_trigger"]
    TRG1 --> NOTIF1["INSERT notification\nBILL_GENERATED · PENDING"]

    PAY_INS["INSERT into payments\n(balance reaches 0)"] --> TRG2["after_payment_insert_trigger"]
    TRG2 --> UPD["UPDATE bill status = PAID"]
    TRG2 --> NOTIF2["INSERT notification\nPAYMENT_COMPLETED · PENDING"]

    NOTIF1 & NOTIF2 --> JAVA["NotificationService\nsendAllPendingNotificationEmails()"]
    JAVA --> MAIL["Gmail SMTP → customer email"]
```

---

## 8. API Module Map

| # | Swagger Section | Base Path | Roles |
|---|-----------------|-----------|-------|
| 01 | Authentication | `/api/auth` | Public + authenticated logout |
| 02 | Admin User Management | `/api/users` | ADMIN |
| 03 | Customer Setup | `/api/customers` | ADMIN, CUSTOMER (profile-request) |
| 04 | Meter Setup | `/api/meters` | ADMIN (create), all staff (list) |
| 05 | Billing Configuration | `/api/tariffs`, `/api/taxes`, `/api/penalties`, `/api/service-charges` | ADMIN |
| 06 | Meter Reading Operations | `/api/readings` | OPERATOR (capture), staff (list) |
| 07 | Billing Operations | `/api/bills` | FINANCE, ADMIN |
| 08 | Payment Operations | `/api/payments` | FINANCE, ADMIN |
| 09 | Customer Self-Service | `/api/customers`, `/api/bills`, `/api/payments`, `/api/meters`, `/api/notifications`, `/api/prepaid` | CUSTOMER (own data) |
| 10 | Notifications | `/api/notifications` | FINANCE, ADMIN |
| 11 | Legacy Prepaid | `/api/prepaid` | FINANCE, CUSTOMER |

**Total secured endpoints:** 53

---

## 9. Key Business Rules

| Rule | Enforced in |
|------|-------------|
| OTP required before login | `AuthService` |
| One reading per meter per month/year | `MeterReadingService` |
| Reading date ≥ meter installation date | `MeterReadingService` |
| Current reading > previous reading | `MeterReadingRequest` + service |
| Inactive customers cannot receive bills | `BillService` |
| Tariffs versioned; new versions for future cycles | `TariffService` |
| Partial and full payments update balance | `PaymentService` |
| Bill marked PAID when balance = 0 | `PaymentService` + DB trigger |
| Customer sees only own data | `CurrentUserService` |
| Payment date cannot be in the future | `PaymentRequest` validation |

---

## 10. Deployment View

```mermaid
flowchart TB
    DEV["Developer Machine"]
    DEV --> APP["utilitybilling.jar\nSpring Boot :8080"]
    APP --> PG["PostgreSQL :5432\nutilitybilling DB"]
    APP --> GMAIL["smtp.gmail.com :587"]
    DEV --> SWAGGER["Swagger UI"]
    DEV --> POSTMAN["Postman + ubs-local env"]
```
