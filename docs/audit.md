I want you to perform a comprehensive audit of this project against the original requirements that were provided. Carefully analyze the requirements, compare them with the implemented solution, and provide an objective assessment of how closely the project matches what was requested.

Your review should include:

- Requirement-by-requirement compliance analysis.
- Identification of completed, partially completed, missing, or incorrectly implemented features.
- Functional testing of all major workflows and modules.
- Review of frontend, backend, database, APIs, integrations, security, validations, and user experience.
- Detection of bugs, broken functionality, inconsistencies, edge cases, and performance concerns.
- Identification of conflicting business logic, unclear implementations, and requirement misunderstandings.
- Verification that role-based permissions and access controls behave as intended.
- Verification that all dashboards, reports, forms, and CRUD operations work correctly.
- Review of error handling, notifications, email functionality, and authentication flows.
- Identification of any mock, placeholder, or hardcoded data that should be replaced with real data.
- Review of code quality, architecture, maintainability, and scalability.

Provide your independent opinion on whether the project truly satisfies the requirements and would be acceptable for submission. Do not assume anything is correct—verify everything critically.

Finally, produce a detailed audit report containing:

- Overall project score (%).
- Requirements coverage analysis.
- Critical issues.
- Major issues.
- Minor issues.
- Missing features.
- Bugs and defects.
- Security concerns.
- UX/UI concerns.
- Recommendations and priorities for fixing the remaining gaps.

Here are the original project requirements for comparison:

Water and Sanitation Corporation (WASAC) and Rwanda Energy Group... A National Utility Company (WASAC), responsible for the distribution of water and electricity in Rwanda, serves thousands of customers across different regions. Each customer is assigned one or more utility meters to track monthly consumption. While water services are billed on a postpaid model, electricity is currently provided on a prepaid model. To unify billing and improve customer experience, the utility company plans to transition electricity services to a postpaid system similar to water.

The current billing process is partly manual, which often leads to mistakes, delayed bills, and a lack of clear information for customers. To address this, the utility company aims to develop a secure and automated Utility Billing System that will manage customers, meter readings, postpaid and prepaid billing, monthly bills, payments, and notifications.

TASKS
You have been hired as a Java Backend Developer to design and implement the backend of this system using Spring Boot and Advanced Java technologies, ensuring strict business rules, data integrity, and automated messaging with the following minimum features:

Task 1: User Management and Security (JWT)
Implement a secure user management system using JWT-based authentication and authorization.

Functional Requirements:

Allow user signup and login.

Secure all endpoints except for public authentication endpoints (registration/login).

RBAC (Role-Based Access Control) Requirements:

ROLE_ADMIN: Configures tariffs, approves bills, and manages all users.

ROLE_OPERATOR: Captures meter readings.

ROLE_FINANCE: Approves bills and processes payments.

ROLE_CUSTOMER: Views personal bills and payment history.

Task 2: Customer and Meter Management
Store and manage customer details:

Full names

National ID (unique)

Email

Phone number

Address

Status (Active / Inactive)

Rules:

Prevent duplicate customer registration.

Inactive customers cannot receive bills.

Meter Management:

Each customer may have one or more meters with:

Meter number (unique)

Meter type (Water / Electricity)

Installation date

Status (Active / Inactive)

Task 3: Meter Reading Management
Allow operators to capture meter readings with:

Meter reference

Previous reading

Current reading

Reading date

Business Rules:

Current reading must be greater than previous reading.

Only one reading per meter per Month/Year.

Meter must be active.

Task 4: Tariff, Tax, and Penalty Configuration
Allow admins to configure:

Consumption tariffs (flat or tier-based)

Fixed service charges

VAT or other taxes

Late payment penalties

Rules:

Tariffs must be versioned.

New tariffs apply only to future billing cycles.

Task 5: Billing and Payments
Support partial and full payments.

Automatically update outstanding balance.

Mark bill as PAID when balance reaches zero.

Task 6: Database Routines and Messaging
Configure database-level routines (Trigger, Stored Procedure, or Cursor).

Required behaviors:

On bill generation, insert an automated notification message.

On full payment, update bill status and notify customer.

Message format:

Dear <CustomerName>,
Your <Month/Year> utility bill of <Amount> FRW has been successfully processed.

Instructions
Prior to start coding, design the Entity Relationship Diagram (ERD) of the expected Database based on the relational DBMS of your choice.

The backend must be designed using Spring Boot. Spring Data JPA should be used to configure the database and generate APIs to be used for backend implementation.

Design the Spring Boot Flow Diagram indicating the functional flow of the system.

After proper configuration of the backend, the records can be manually processed using either Postman, Swagger.
