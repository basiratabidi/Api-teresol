# Software Requirements Specification (SRS)

**Product:** Mera Apna Bank — regional digital banking platform
**Version:** 1.0 &nbsp;|&nbsp; **Status:** Draft for review &nbsp;|&nbsp; **Date:** 2026-09-25
**Standard:** structured after IEEE 830

| Version | Date | Change |
|---------|------|--------|
| 1.0 | 2026-09-25 | First full specification: banking operations, authentication, web UI |

---

## 1. Introduction

### 1.1 Purpose
This document specifies the requirements of Mera Apna Bank, a web-based
banking administration system. It is written for developers, testers, project
supervisors and anyone who needs to know *what* the system must do. The
companion [SDS](SDS.md) describes *how* it is built, and the
[Quick Guide](quick-guide.md) explains how to use it.

### 1.2 Scope
Mera Apna Bank lets authorised staff manage **branches**, **customer accounts**
and **money movements** (deposit, withdrawal, transfer) across four regions —
North, South, East and West. Each region keeps its data in its own separate
database. Staff sign in through a web interface; every banking operation
requires authentication.

**In scope:** staff signup/login, branch management, account management,
deposits, withdrawals, same-region transfers, a dashboard with per-region
counts, the web UI.
**Out of scope:** customer self-service, cross-region transfers, statements or
transaction history, interest, cards, payments to other banks, role-based
permissions, email/SMS notifications.

### 1.3 Definitions, acronyms and abbreviations

| Term | Meaning |
|------|---------|
| Region | One of NORTH, SOUTH, EAST, WEST; each maps to one PostgreSQL database |
| Branch | A physical bank branch, identified by a branch code such as `NO-001` |
| Account | A customer bank account, identified by an account number such as `NO-AC-0001` |
| Region prefix | `NO-`, `SO-`, `EA-`, `WE-` — codes and numbers must start with their region's prefix |
| Core API | The `core-api-accounts` service (port 8081), the public entry point |
| Data-access service | The `dataaccess-ms-accounts` service (port 8083), which owns all SQL |
| Token | A signed, expiring string that proves a user is signed in |
| PKR | Pakistani Rupee, the ledger currency |
| PBKDF2 | Password-hashing algorithm used to store passwords |
| REST | HTTP-based API style; JSON is the data format |
| SRS / SDS | Software Requirements / Design Specification |

### 1.4 References
1. IEEE Std 830-1998, *Recommended Practice for Software Requirements Specifications*.
2. [Software Design Specification](SDS.md), [Authentication](authentication.md), [Frontend](frontend.md), [main README](../README.md).
3. Quarkus documentation — <https://quarkus.io/guides/>.
4. PostgreSQL 16 documentation — <https://www.postgresql.org/docs/16/>.

### 1.5 Overview
Section 2 describes the product in general terms. Section 3 lists the specific,
testable requirements. Section 4 holds the traceability matrix. Requirement
priorities use MoSCoW: **M**ust, **S**hould, **C**ould.

---

## 2. Overall description

### 2.1 Product perspective
The system is a standalone two-service application.

```
 Browser (web UI)
      │  HTTPS/HTTP, JSON, Bearer token
      ▼
 core-api-accounts  :8081   — UI hosting, authentication, REST API
      │  REST client (JSON)
      ▼
 dataaccess-ms-accounts :8083 — JDBC + SQL, region routing
      │  JDBC
      ▼
 PostgreSQL × 4  (north :55432, south :55433, east :55434, west :55435)
```

- **User interface:** single-page web UI (login/signup, dashboard, branches, accounts, transactions).
- **Software interfaces:** REST/JSON between browser and core API, and between core API and data-access service; JDBC to PostgreSQL.
- **Hardware interfaces:** none.
- **Communications:** HTTP on ports 8081 and 8083; PostgreSQL on 55432–55435.
- **Site adaptation:** databases start from `docker-compose.yml`; each is seeded from `dataaccess-ms-accounts/db/<region>-init.sql`.

### 2.2 Product functions
1. Register and authenticate staff users.
2. Create, list, update and delete branches per region.
3. Open, list, update and close accounts per region.
4. Deposit to and withdraw from accounts.
5. Transfer funds between two accounts in the same region.
6. Show a dashboard with live branch and account counts per region.

### 2.3 User characteristics
Bank staff and administrators comfortable with a web browser and basic banking
terms. No programming knowledge is needed to use the UI. Developers and
testers use the REST API directly.

### 2.4 Constraints
- **CON-TECH-001** Java 17 or newer, Quarkus 3.x, PostgreSQL 16.
- **CON-TECH-002** Each region shall use its own database; no cross-region queries or transactions.
- **CON-TECH-003** No ORM; data access uses plain JDBC.
- **CON-TECH-004** The web UI is plain HTML/CSS/JavaScript with no build step.
- **CON-SEC-001** Passwords shall never be stored in plain text.

### 2.5 Assumptions and dependencies
- Docker and Docker Compose are available to run the four databases.
- The data-access service is reachable from the core API on a private network.
- Users have a modern browser (current Chrome, Firefox, Edge or Safari). WebGL is desirable for the 3D visuals but not required.
- Internet access is desirable for the Montserrat web font; the system falls back to system fonts without it.

---

## 3. Specific requirements

### 3.1 External interface requirements

**User interface**
- **UI-001 (M)** The UI shall show a login/signup screen to unauthenticated visitors and the banking application only after sign-in.
- **UI-002 (M)** The UI shall use the Montserrat typeface.
- **UI-003 (S)** The UI shall include 3D animated visuals (rotating logo coins) and animated page transitions, and shall reduce or disable motion when the user's system requests reduced motion.
- **UI-004 (M)** The UI shall display the bank logo on the login screen and in the navigation sidebar.
- **UI-005 (M)** The UI shall be usable at phone width (≥ 360 px) and desktop widths.

**Software interfaces** — the REST API is specified in the [main README](../README.md#api-overview) and summarised in FR-* below. Errors are returned as `{"status": <code>, "error": "<message>"}`.

### 3.2 Functional requirements

#### 3.2.1 Authentication and session

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-AUTH-001 | The system shall let a new user sign up with full name, email and password. | M |
| FR-AUTH-002 | The system shall reject signup if the full name is shorter than 2 characters, the email is malformed, or the password is shorter than 8 characters (`400`). | M |
| FR-AUTH-003 | The system shall reject signup for an email that is already registered, ignoring letter case (`409`). | M |
| FR-AUTH-004 | The system shall let a registered user log in with email and password and return a session token with its expiry time. | M |
| FR-AUTH-005 | The system shall reject login with a wrong email or password with a single generic message that does not reveal which was wrong (`401`). | M |
| FR-AUTH-006 | The system shall store only a salted PBKDF2 hash of each password. | M |
| FR-AUTH-007 | The system shall reject every request to `/branches/*`, `/accounts/*` and `/auth/me` that has no valid, unexpired token (`401`). | M |
| FR-AUTH-008 | The system shall expire tokens after a configurable time (default 8 hours). | M |
| FR-AUTH-009 | The system shall reject tokens that were modified after issue. | M |
| FR-AUTH-010 | The UI shall keep the user signed in across page reloads until the token expires or the user logs out. | S |
| FR-AUTH-011 | The UI shall return to the login screen when the server reports the session invalid or expired. | M |
| FR-AUTH-012 | The UI shall provide a Log out action that discards the token. | M |
| FR-AUTH-013 | The UI shall show a password-strength indicator on signup and require the two password entries to match. | S |

#### 3.2.2 Branch management

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-BR-001 | The system shall list all branches of a chosen region. | M |
| FR-BR-002 | The system shall create a branch from branch code, name, city and region. | M |
| FR-BR-003 | The system shall reject a branch whose code does not start with its region's prefix (`400`). | M |
| FR-BR-004 | The system shall reject a duplicate branch code (`409`). | M |
| FR-BR-005 | The system shall update a branch's name and city (`404` if the branch does not exist). | M |
| FR-BR-006 | The system shall delete a branch (`404` if missing) and shall refuse if accounts still belong to it (`409`). | M |
| FR-BR-007 | The system shall reject an unknown region (`400`). | M |

#### 3.2.3 Account management

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-AC-001 | The system shall list all accounts of a chosen region. | M |
| FR-AC-002 | The system shall open an account from account number, holder name, branch code, type (SAVINGS or CURRENT), opening balance (zero or more) and region. | M |
| FR-AC-003 | The system shall reject an account number that does not start with the region's prefix, a negative balance, or an account type other than SAVINGS/CURRENT (`400`). | M |
| FR-AC-004 | The system shall reject an account whose branch does not exist in that region with a `4xx` error and a readable message. **Known deviation:** currently returns `500` with the raw database message (see Appendix C). | M |
| FR-AC-005 | The system shall update an account's holder name, type and balance, and shall return `404` if the account does not exist. **Known deviation:** currently returns `200` with `rowsUpdated: 0`. | M |
| FR-AC-006 | The system shall close (delete) an account, and shall return `404` if it does not exist. **Known deviation:** currently returns `200` with `rowsDeleted: 0`. | M |
| FR-AC-007 | The UI shall display balances formatted as `PKR 15,000.00`. | S |

#### 3.2.4 Transactions

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-TX-001 | The system shall deposit a positive amount into an account and return the new balance. | M |
| FR-TX-002 | The system shall withdraw a positive amount from an account and return the new balance. | M |
| FR-TX-003 | The system shall reject a withdrawal that exceeds the balance (`400`) and leave the balance unchanged. | M |
| FR-TX-004 | The system shall reject a non-positive amount (`400`). | M |
| FR-TX-005 | The system shall return `404` when a deposit, withdrawal or transfer names an account that does not exist. | M |
| FR-TX-006 | The system shall transfer a positive amount between two accounts of the same region as a single atomic operation: either both balances change or neither does. | M |
| FR-TX-007 | The system shall reject a transfer from an account to itself (`400`, enforced by the server and by the UI). | M |
| FR-TX-008 | The UI shall ask the user to confirm a transfer, showing amount and both accounts, before sending it. | S |

#### 3.2.5 Dashboard and user interface behaviour

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-UI-001 | The dashboard shall show, for each region, the current number of branches and accounts, loaded from the API. | M |
| FR-UI-002 | The dashboard shall greet the signed-in user by first name. | C |
| FR-UI-003 | The UI shall validate forms before sending: required fields non-empty, amounts greater than zero (opening and updated balances may be zero), with the offending field highlighted and a message shown. | S |
| FR-UI-004 | The UI shall ask for confirmation before deleting a branch or closing an account. | S |
| FR-UI-005 | Tables shall show a loading indicator, an empty-state message when there are no rows, and an error message on failure. | S |
| FR-UI-006 | Pressing Enter in a form shall trigger that form's primary action. | C |
| FR-UI-007 | The UI shall escape all data received from the API before displaying it (protection against script injection). | M |

### 3.3 Performance requirements
| ID | Requirement |
|----|-------------|
| NFR-PERF-001 | For up to 1,000 rows per region, list operations shall respond within 2 seconds under normal load (single user, local network). |
| NFR-PERF-002 | Deposit, withdrawal and transfer shall respond within 2 seconds under the same conditions. |
| NFR-PERF-003 | The login screen shall be interactive within 3 seconds on a broadband connection. |
| NFR-PERF-004 | Password hashing shall take long enough to slow guessing (PBKDF2, ≥ 100,000 iterations) yet keep login under 1 second. |

### 3.4 Design constraints
See section 2.4. Additionally: the core API shall contain no SQL, and the data-access service shall reach each region only through that region's own datasource.

### 3.5 Software system attributes

| ID | Attribute | Requirement |
|----|-----------|-------------|
| NFR-SEC-001 | Security | Passwords are hashed with a per-user random salt; comparison is constant-time. |
| NFR-SEC-002 | Security | Login timing shall not reveal whether an email is registered. |
| NFR-SEC-003 | Security | The token-signing secret shall be configurable (`AUTH_SECRET`). |
| NFR-SEC-004 | Security | All database statements shall be parameterised (no string-built SQL from user input values). |
| NFR-SEC-005 | Security | The data-access service shall be deployed on a private network only, since it does not authenticate callers. |
| NFR-REL-001 | Reliability | A failed transfer shall roll back completely (no partial update). |
| NFR-REL-002 | Reliability | Errors from the data-access service shall be relayed with their original, readable message. |
| NFR-USA-001 | Usability | A new user shall be able to sign up and open an account without training, using the [Quick Guide](quick-guide.md). |
| NFR-USA-002 | Usability | Error messages shall say what is wrong in plain language. |
| NFR-MAINT-001 | Maintainability | The project shall be documented (README, SRS, SDS, guides) and use a clear layered structure (Resource → Service → Client). |
| NFR-PORT-001 | Portability | The system shall run on Linux, macOS and Windows with a JDK and Docker. |
| NFR-ACC-001 | Accessibility | Animations shall respect the "reduce motion" system setting; form fields shall have labels. |

### 3.6 Other requirements
- **Database:** each region has `branch` and `account` tables (see [SDS §5](SDS.md#5-data-design)); codes and numbers are unique; an account references an existing branch.
- **Seed data:** each region is created with two sample branches and accounts.
- **Internationalisation:** English UI; amounts in PKR.

---

## 4. Appendices

### A. Use cases

| ID | Use case | Actor | Main flow |
|----|----------|-------|-----------|
| UC-01 | Sign up | Staff | Enter name, email, password → account created → signed in |
| UC-02 | Log in / out | Staff | Enter email, password → dashboard; Log out → login screen |
| UC-03 | Manage branches | Staff | Choose region → list; create / update / delete |
| UC-04 | Manage accounts | Staff | Choose region → list; open / update / close |
| UC-05 | Deposit / withdraw | Staff | Choose region, account, amount → new balance shown |
| UC-06 | Transfer | Staff | Choose region, from, to, amount → confirm → done |

### B. Requirements traceability matrix

Test cases are manual/API tests (T-xx); IDs are referenced by the [Quick Guide checks](quick-guide.md) and can be automated later.

| Requirement(s) | Use case | Verification |
|----------------|----------|--------------|
| FR-AUTH-001..003, 013 | UC-01 | T-01 signup valid; T-02 short password → 400; T-03 duplicate email → 409 |
| FR-AUTH-004..006 | UC-02 | T-04 valid login; T-05 wrong password → 401; inspect `users.json` (no plain text) |
| FR-AUTH-007..009, 011 | UC-02 | T-06 no token → 401; T-07 tampered token → 401; T-08 expired token → 401 |
| FR-AUTH-010, 012 | UC-02 | T-09 reload stays signed in; T-10 logout returns to login |
| FR-BR-001..007 | UC-03 | T-11 list; T-12 create; T-13 wrong prefix → 400; T-14 duplicate → 409; T-15 update; T-16 delete with accounts → 409 |
| FR-AC-001..007 | UC-04 | T-17 list; T-18 open; T-19 wrong prefix → 400; T-20 unknown branch rejected (currently 500, known deviation); T-21 update; T-22 close |
| FR-TX-001..005 | UC-05 | T-23 deposit; T-24 withdraw; T-25 overdraw → 400 unchanged; T-26 amount 0 → 400; T-27 unknown account → 404 |
| FR-TX-006..008 | UC-06 | T-28 transfer moves both balances; T-29 failure leaves both unchanged; T-30 same account refused; T-31 confirm dialog |
| FR-UI-001..007 | all | T-32 dashboard counts; T-33 validation highlights; T-34 confirmations; T-35 empty/loading states; T-36 HTML in a holder name is shown as text |
| NFR-PERF-* | — | T-37 timed requests |
| NFR-SEC-* | — | T-38 code review + T-04..T-08 |
| NFR-REL-001 | UC-06 | T-29 |

### C. Open issues (known gaps)
1. All signed-in users have the same permissions (no roles).
2. No login rate limiting, password reset or email verification.
3. No transaction history or statements.
4. Account creation with an unknown branch (or a duplicate account number) returns `500` with a raw database message, and updating or deleting a non-existent account returns `200` with zero rows affected instead of `404` (FR-AC-004..006). These were verified against the running system on 2026-09-25.
5. Automated integration tests do not exist yet.
