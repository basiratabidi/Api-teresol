# Mera Apna Bank

A region-routed banking demo built on Quarkus, split across two services:

- **`core-api-accounts`** (port `8081`) — public-facing REST API and the web UI.
  Delegates to `dataaccess-ms-accounts` through a REST client. Contains no SQL
  itself; it's a thin pass-through layer (Resource → Service → REST client).
- **`dataaccess-ms-accounts`** (port `8083`) — owns the JDBC connections and
  all the SQL for each region, and talks to four separate Postgres databases
  (one per region: NORTH, SOUTH, EAST, WEST).

Each region is a fully separate Postgres database, so a request always carries
a region (`north`, `south`, `east`, or `west`) telling the data-access service
which datasource to use. Branch codes and account numbers are prefixed per
region (see [Region code prefixes](#region-code-prefixes)), and each region's
database is only ever reached through its own datasource — there's no
cross-region querying.

## Tech stack

- **Quarkus** — Jakarta REST (RESTEasy Reactive), MicroProfile REST Client,
  Agroal connection pooling, `quarkus-jdbc-postgresql`.
- **Plain JDBC, no ORM** — no Hibernate, JPA `@Entity`, or Panache. SQL is
  either built with a small hand-rolled query builder (`util/Table`,
  `util/Selection`, `util/QueryBuilder`, for simple equality-based
  `SELECT`/`INSERT`/`UPDATE`/`DELETE`) or written directly as parameterized
  `PreparedStatement`s (for deposit/withdraw/transfer, which need conditions
  like `balance >= ?` that the query builder doesn't support). Result rows are
  mapped to DTOs by hand.
- **Manual JDBC transactions** for transfers
  (`setAutoCommit(false)` / `commit()` / `rollback()`), not `@Transactional`.
- **Vanilla HTML/CSS/JS** web UI — no framework, no build step, served as a
  static resource by Quarkus. Uses the Montserrat font and Three.js for the 3D
  scenes (bundled locally, see [docs/frontend.md](docs/frontend.md)).
- **Built-in authentication** — signup/login with PBKDF2-hashed passwords and
  HMAC-signed bearer tokens, no extra dependencies (see
  [docs/authentication.md](docs/authentication.md)).
- **Postgres 16** — one container per region, via Docker Compose.

## Documentation

- [docs/authentication.md](docs/authentication.md) — how signup, login, tokens and route protection work, and how to harden them
- [docs/frontend.md](docs/frontend.md) — the web UI: structure, fonts, 3D scenes, animations, assets, validation
- [meraApnaBank/core-api-accounts/README.md](meraApnaBank/core-api-accounts/README.md) — building and running the core API

## Prerequisites

- JDK 21
- Maven (or the bundled `./mvnw` wrapper in each module)
- Docker + Docker Compose

## Running it

All commands are run from the `meraApnaBank` directory.

1. **Start the databases**

   ```bash
   docker compose up -d
   ```

   This starts 4 Postgres containers (`north-db`, `south-db`, `east-db`,
   `west-db`) on ports `55432`–`55435`. On first start each is seeded from the
   matching `dataaccess-ms-accounts/db/*-init.sql` script with a `branch`
   table, an `account` table (with a foreign key from `account.branch_code` to
   `branch.branch_code`), and a couple of sample rows.

   > The init scripts only run when a database's volume is empty. After
   > changing a script, apply the change by hand or reset with
   > `docker compose down -v && docker compose up -d` (this deletes all data).

2. **Start the data-access service** (in one terminal)

   ```bash
   cd dataaccess-ms-accounts
   ./mvnw quarkus:dev
   ```

   Runs on <http://localhost:8083>.

3. **Start the core API** (in another terminal)

   ```bash
   cd core-api-accounts
   ./mvnw quarkus:dev
   ```

   Runs on <http://localhost:8081>.

4. **Open the web UI** at <http://localhost:8081/>. You'll see a login screen:
   choose **Sign up** to create an account, then log in. After signing in there
   are four pages:
   - **Home** — live per-region branch/account counts and a feature overview
   - **Branches** — list, create, update, delete
   - **Accounts** — list, open, update, close
   - **Transactions** — deposit, withdraw, and transfer between accounts

   Deletes, account closures and transfers ask for confirmation first, forms
   are validated before anything is sent, and **Log out** is at the bottom of
   the sidebar.

### Configuration (core-api-accounts)

| Property / env var | Default | Purpose |
|--------------------|---------|---------|
| `AUTH_SECRET` (`auth.secret`) | random per start | Key used to sign session tokens. **Set it in production** — otherwise every restart logs everyone out. |
| `AUTH_USERS_FILE` (`auth.users-file`) | `users.json` (working dir) | Where registered users are stored. Git-ignored. |
| `auth.token-ttl-seconds` | `28800` (8 h) | How long a session token stays valid. |
| `quarkus.http.port` | `8081` | HTTP port. |
| `branch-data-access/mp-rest/url` | `http://localhost:8083` | Address of `dataaccess-ms-accounts`. |

## Region code prefixes

Branch codes and account numbers must start with their region's prefix. This
is enforced server-side on create; anything else gets `400 Bad Request`.

| Region | Prefix | Example branch code | Example account number |
|--------|--------|---------------------|------------------------|
| NORTH  | `NO-`  | `NO-001`            | `NO-AC-0001`           |
| SOUTH  | `SO-`  | `SO-001`            | `SO-AC-0001`           |
| EAST   | `EA-`  | `EA-001`            | `EA-AC-0001`           |
| WEST   | `WE-`  | `WE-001`            | `WE-AC-0001`           |

## API overview

All endpoints below are served by `core-api-accounts` on port `8081`, which
forwards them to `dataaccess-ms-accounts`. `{region}` is one of `north`,
`south`, `east`, `west` (case-insensitive).

**Every `/branches/*` and `/accounts/*` request requires authentication** —
send `Authorization: Bearer <token>`, using the token returned by
`/auth/signup` or `/auth/login`. Requests without a valid token get
`401 {"status":401,"error":"Please sign in to continue."}`.

### Authentication

| Method | Path           | Auth   | Body                                  | Returns |
|--------|----------------|--------|---------------------------------------|---------|
| POST   | `/auth/signup` | none   | `{"fullName", "email", "password"}`   | session |
| POST   | `/auth/login`  | none   | `{"email", "password"}`               | session |
| GET    | `/auth/me`     | bearer | —                                     | `{"fullName", "email"}` |

A *session* is `{"token", "expiresAt", "user": {"fullName", "email"}}`
(`expiresAt` is epoch seconds).

```bash
# sign up, then use the token
TOKEN=$(curl -s -H 'Content-Type: application/json' \
  -d '{"fullName":"Ayesha Khan","email":"ayesha@example.com","password":"correct horse 9"}' \
  http://localhost:8081/auth/signup | jq -r .token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/accounts/north
```

Rules: full name at least 2 characters, a valid email, password at least 8
characters. Emails are case-insensitive. See
[docs/authentication.md](docs/authentication.md) for the design.

### Branches

| Method | Path                              | Body                  |
|--------|-----------------------------------|-----------------------|
| GET    | `/branches/{region}`              | —                     |
| POST   | `/branches`                       | `NewBranchRequest`    |
| PUT    | `/branches/{region}/{branchCode}` | `UpdateBranchRequest` |
| DELETE | `/branches/{region}/{branchCode}` | —                     |

- `NewBranchRequest`: `branchCode`, `branchName`, `city`, `regionCode`.
  `branchCode` must use the region's prefix.
- `UpdateBranchRequest`: `branchName`, `city`.

### Accounts

| Method | Path                                 | Body                   |
|--------|--------------------------------------|------------------------|
| GET    | `/accounts/{region}`                 | —                      |
| POST   | `/accounts`                          | `NewAccountRequest`    |
| PUT    | `/accounts/{region}/{accountNumber}` | `UpdateAccountRequest` |
| DELETE | `/accounts/{region}/{accountNumber}` | —                      |

- `NewAccountRequest`: `accountNumber`, `accountHolderName`, `branchCode`,
  `accountType` (`SAVINGS` or `CURRENT`), `balance` (zero or positive),
  `regionCode`. `accountNumber` must use the region's prefix, and `branchCode`
  must be an existing branch in the same region.
- `UpdateAccountRequest`: `accountHolderName`, `accountType`, `balance`.

### Transactions

| Method | Path                                          | Body                                                 |
|--------|-----------------------------------------------|------------------------------------------------------|
| POST   | `/accounts/{region}/{accountNumber}/deposit`  | `{"amount": 500.00}`                                 |
| POST   | `/accounts/{region}/{accountNumber}/withdraw` | `{"amount": 500.00}`                                 |
| POST   | `/accounts/{region}/transfer`                 | `{"fromAccountNumber", "toAccountNumber", "amount"}` |

- `amount` must be positive.
- `deposit` / `withdraw` return `{"accountNumber", "balance", "status"}`.
- `withdraw` fails with `400` if the account can't cover the amount.
- `transfer` returns `{"fromAccountNumber", "toAccountNumber", "amount", "status"}`.
  It is atomic (single JDBC transaction), and both accounts must be in the same
  region, since each region is a separate database.

### Errors

Errors come back as JSON: `{"status": <code>, "error": "<message>"}`.

| Status | When |
|--------|------|
| `400`  | Unknown region, missing/invalid field, wrong code prefix, insufficient funds; signup with an invalid name, email or short password |
| `401`  | Missing, expired or tampered token; wrong email or password at login |
| `404`  | Branch not found (update/delete); account not found (deposit/withdraw/transfer) |
| `409`  | Duplicate branch code, deleting a branch that still has accounts, or signing up with an email that already exists |

## Running the tests

```bash
cd dataaccess-ms-accounts
./mvnw test
```

The tests cover pure logic (region parsing and prefixes, the query-builder
helper types) and do not need a running database. `core-api-accounts` has no
tests yet.

## Project layout

```
meraApnaBank/
├── docker-compose.yml                  # 4 regional Postgres containers
├── core-api-accounts/                  # public REST API + web UI (port 8081)
│   └── src/main/
│       ├── java/com/teresol/meraapnabank/
│       │   ├── auth/                   # AuthService, AuthResource, AuthFilter (signup/login/tokens)
│       │   ├── resource/               # BranchResource, AccountResource (incl. deposit/withdraw/transfer)
│       │   ├── service/                # BranchService, AccountService
│       │   ├── client/                 # BranchClient, AccountClient (REST clients -> port 8083)
│       │   ├── dto/                    # *Dto, New*Request, Update*Request, AmountRequest, TransferRequest
│       │   └── exception/              # GlobalExceptionMapper (relays data-access error messages)
│       └── resources/META-INF/resources/
│           ├── index.html              # login/signup + Home / Branches / Accounts / Transactions UI
│           └── assets/                 # logo.png, skyline/card/shield SVGs, three.min.js
└── dataaccess-ms-accounts/             # region-routed JDBC service (port 8083)
    ├── db/{north,south,east,west}-init.sql
    └── src/
        ├── main/java/com/teresol/meraapnabank/
        │   ├── BranchResource.java     # SQL via QueryBuilder, enforces branch-code prefix
        │   ├── AccountResource.java    # QueryBuilder + raw SQL for deposit/withdraw/transfer
        │   ├── Datasources.java        # region -> Agroal datasource
        │   ├── RegionType.java         # region parsing/validation + codePrefix()
        │   ├── GlobalExceptionMapper.java
        │   ├── *Dto.java, *Request.java
        │   └── util/
        │       ├── Table.java          # table name + columns
        │       ├── Selection.java      # column = value pair
        │       ├── Tables.java         # BRANCH / ACCOUNT table constants
        │       └── QueryBuilder.java   # builds SELECT/INSERT/UPDATE/DELETE
        └── test/java/com/teresol/meraapnabank/
            ├── RegionTypeTest.java
            └── util/TableAndSelectionTest.java
```

## Known limitations

- **Authentication is basic** — one shared role: any registered user can do
  every banking action (no roles or per-user permissions), there's no login
  rate limiting or password reset, and users live in a local `users.json`
  rather than a database. Fine for a demo; harden before real use.
- **`dataaccess-ms-accounts` (port 8083) has no auth** — only the core API
  enforces it, so port 8083 must not be exposed publicly.
- **No integration tests** against a real database; only pure-logic unit tests.
- **Single-region transfers only** — by design, since there's no distributed
  transaction across the separate databases.
- **Some account errors aren't mapped yet:**
  - creating a duplicate account number, or an account for a branch that
    doesn't exist, returns `500` with the raw Postgres error;
  - updating or deleting an account that doesn't exist returns `200` with
    `rowsUpdated`/`rowsDeleted` of `0` instead of `404`.
