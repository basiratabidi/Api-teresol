# Mera Apna Bank

A region-routed banking demo built on Quarkus, split across two services:

- **`core-api-accounts`** (port `8081`) — public-facing REST API and the web UI.
  Delegates to `dataaccess-ms-accounts` through a REST client. Contains no SQL
  itself; it's a thin pass-through layer (Resource → Service → REST client).
- **`dataaccess-ms-accounts`** (port `8083`) — owns the JDBC connections and
  all the SQL for each region, and talks to four separate Postgres databases
  (one per region: NORTH, SOUTH, EAST, WEST).

Each region is a fully separate Postgres database, so a request always
carries a region (`north`, `south`, `east`, or `west`) telling the
data-access service which datasource to use. Branch codes and account
numbers are prefixed per region (see **Region code prefixes** below), and
each region's database is only ever reached through its own datasource —
there's no cross-region querying.

## Tech stack

- **Quarkus** (Jakarta REST / RESTEasy Reactive, MicroProfile REST Client,
  Agroal connection pooling, `quarkus-jdbc-postgresql`)
- **Plain JDBC — no ORM.** There's no Hibernate, no JPA `@Entity`, no
  Panache. SQL is either built through a small hand-rolled query-builder
  utility (`util/Table`, `util/Selection`, `util/QueryBuilder` — for simple
  equality-based `SELECT`/`INSERT`/`UPDATE`/`DELETE`) or written directly as
  parameterized `PreparedStatement`s (for the deposit/withdraw/transfer
  endpoints, which need conditions like `balance >= ?` that the query
  builder doesn't support). Result rows are mapped to DTOs by hand.
- Money transfers use manual JDBC transactions
  (`connection.setAutoCommit(false)` / `commit()` / `rollback()`), not
  `@Transactional`.
- **Vanilla HTML/CSS/JS** for the web UI — no framework, no build step,
  served as a static resource by Quarkus.
- **Postgres** (one instance per region, via Docker Compose).

## Prerequisites

- JDK 21
- Maven (or use the bundled `./mvnw` wrapper in each module)
- Docker + Docker Compose (for the 4 regional Postgres databases)

## Running it

1. **Start the databases** (from the `meraApnaBank` directory):

   ```bash
   docker compose up -d
This starts 4 Postgres containers (north-db, south-db, east-db,
west-db) on ports 55432–55435, each seeded from the matching
dataaccess-ms-accounts/db/*-init.sql script with a branch table, an
account table (with a foreign key from account.branch_code to
branch.branch_code), and a couple of sample rows.

Start the data-access service (in one terminal):
cd dataaccess-ms-accounts
./mvnw quarkus:dev
Runs on http://localhost:8083.

Start the core API (in another terminal):
cd core-api-accounts
./mvnw quarkus:dev
Runs on http://localhost:8081.

Open the web UI: http://localhost:8081/ — a small multi-page app
with:
Home — live per-region branch/account counts and a feature overview
Branches — list, create, update, delete
Accounts — list, open, update, close
Transactions — deposit, withdraw, and transfer between accounts
Region code prefixes
Branch codes and account numbers must be prefixed for the region they're
created in (enforced server-side on create, 400 Bad Request otherwise):

Region	Prefix	Example branch code	Example account number
NORTH	NO-	NO-001	NO-AC-0001
SOUTH	SO-	SO-001	SO-AC-0001
EAST	EA-	EA-001	EA-AC-0001
WEST	WE-	WE-001	WE-AC-0001
API overview
All endpoints below are served by core-api-accounts on port 8081 (it
forwards to dataaccess-ms-accounts internally). {region} is one of
north, south, east, west (case-insensitive).

Branches
Method	Path	Body
GET	/branches/{region}	—
POST	/branches	NewBranchRequest
PUT	/branches/{region}/{branchCode}	UpdateBranchRequest
DELETE	/branches/{region}/{branchCode}	—
NewBranchRequest: branchCode, branchName, city, regionCode.
branchCode must match the region's prefix (see above).

Accounts
Method	Path	Body
GET	/accounts/{region}	—
POST	/accounts	NewAccountRequest
PUT	/accounts/{region}/{accountNumber}	UpdateAccountRequest
DELETE	/accounts/{region}/{accountNumber}	—
NewAccountRequest: accountNumber, accountHolderName, branchCode,
accountType (SAVINGS or CURRENT), balance, regionCode.
accountNumber must match the region's prefix.

Transactions
Method	Path	Body
POST	/accounts/{region}/{accountNumber}/deposit	{"amount": 500.00}
POST	/accounts/{region}/{accountNumber}/withdraw	{"amount": 500.00}
POST	/accounts/{region}/transfer	{"fromAccountNumber", "toAccountNumber", "amount"}
deposit / withdraw return {"accountNumber", "balance", "status"}.
withdraw fails with 400 if the account can't cover the amount.
transfer is atomic (single JDBC transaction) and both accounts must be
in the same region, since each region is a separate database.
Errors come back as JSON: {"status": <code>, "error": "<message>"}.

Running the tests
cd dataaccess-ms-accounts && ./mvnw test
cd core-api-accounts && ./mvnw test
The current test suite covers pure logic (region parsing and prefixes, the
SQL query-builder helper types) and does not require a running database.

Project layout
meraApnaBank/
├── docker-compose.yml              # 4 regional Postgres containers
├── core-api-accounts/              # public REST API + web UI (port 8081)
│   └── src/main/
│       ├── java/.../
│       │   ├── BranchResource.java     # REST endpoints -> BranchService
│       │   ├── AccountResource.java    # REST endpoints -> AccountService (incl. deposit/withdraw/transfer)
│       │   ├── BranchService.java      # business logic layer
│       │   ├── AccountService.java
│       │   ├── BranchClient.java       # REST client -> dataaccess-ms-accounts
│       │   ├── AccountClient.java
│       │   └── GlobalExceptionMapper.java
│       └── resources/META-INF/resources/
│           └── index.html              # Home / Branches / Accounts / Transactions UI
└── dataaccess-ms-accounts/         # region-routed JDBC service (port 8083)
    ├── db/{north,south,east,west}-init.sql
    └── src/main/java/.../
        ├── BranchResource.java     # SQL via QueryBuilder, enforces branch-code prefix
        ├── AccountResource.java    # SQL via QueryBuilder + raw SQL for deposit/withdraw/transfer
        ├── Datasources.java        # region -> Agroal datasource
        ├── RegionType.java         # region parsing/validation + codePrefix()
        ├── GlobalExceptionMapper.java
        └── util/
            ├── Table.java          # table name + columns
            ├── Selection.java      # column = value pair
            ├── Tables.java         # BRANCH / ACCOUNT table constants
            └── QueryBuilder.java   # builds SELECT/INSERT/UPDATE/DELETE
Known limitations
No authentication — every endpoint is open.
No integration tests against a real database; only pure-logic unit tests.
Transfers are restricted to a single region (no cross-region transfer, by
design — there's no distributed transaction across the separate
databases).