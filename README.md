# Mera Apna Bank

A small region-routed banking demo built on Quarkus, split across two services:

- **`core-api-accounts`** (port `8081`) — public-facing REST API and the web UI.
  Delegates to `dataaccess-ms-accounts` through a REST client.
- **`dataaccess-ms-accounts`** (port `8083`) — owns the JDBC connections and the
  SQL for each region and talks to four separate Postgres databases (one per
  region: NORTH, SOUTH, EAST, WEST).

Each region is a fully separate Postgres database, so a request always carries
a region (`north`, `south`, `east`, or `west`) telling the data-access service
which datasource to use.

## Prerequisites

- JDK 21
- Maven (or use the bundled `./mvnw` wrapper in each module)
- Docker + Docker Compose (for the 4 regional Postgres databases)

## Running it

All commands below are run from the `meraApnaBank` directory.

1. **Start the databases**

   ```bash
   docker compose up -d
   ```

   This starts 4 Postgres containers (`north-db`, `south-db`, `east-db`,
   `west-db`) on ports `55432`–`55435`. On first start each one is seeded from
   the matching `dataaccess-ms-accounts/db/*-init.sql` script with a `branch`
   and an `account` table and a couple of sample rows.

   The init scripts only run when a database's volume is empty. After changing
   a script, either apply the change by hand or reset the data with
   `docker compose down -v && docker compose up -d` (this deletes all rows).

2. **Start the data-access service** (in one terminal)

   ```bash
   cd dataaccess-ms-accounts
   ./mvnw quarkus:dev
   ```

   Runs on `http://localhost:8083`.

3. **Start the core API** (in another terminal)

   ```bash
   cd core-api-accounts
   ./mvnw quarkus:dev
   ```

   Runs on `http://localhost:8081`.

4. **Open the web UI** at `http://localhost:8081/` to list, create, update, and
   delete branches and accounts per region.

## API overview

All endpoints below are served by `core-api-accounts` on port `8081`, which
forwards them to `dataaccess-ms-accounts`.

### Branches

| Method | Path                              | Body                  |
|--------|-----------------------------------|-----------------------|
| GET    | `/branches/{region}`              | —                     |
| POST   | `/branches`                       | `NewBranchRequest`    |
| PUT    | `/branches/{region}/{branchCode}` | `UpdateBranchRequest` |
| DELETE | `/branches/{region}/{branchCode}` | —                     |

### Accounts

| Method | Path                                 | Body                   |
|--------|--------------------------------------|------------------------|
| GET    | `/accounts/{region}`                 | —                      |
| POST   | `/accounts`                          | `NewAccountRequest`    |
| PUT    | `/accounts/{region}/{accountNumber}` | `UpdateAccountRequest` |
| DELETE | `/accounts/{region}/{accountNumber}` | —                      |

- `{region}` is one of `north`, `south`, `east`, `west` (case-insensitive).
- `accountType` must be `SAVINGS` or `CURRENT`.
- An account's `branchCode` must be an existing branch in the same region.

### Money movement

| Method | Path                                          | Body                                                   |
|--------|-----------------------------------------------|--------------------------------------------------------|
| POST   | `/accounts/{region}/{accountNumber}/deposit`  | `{"amount": 500.00}`                                   |
| POST   | `/accounts/{region}/{accountNumber}/withdraw` | `{"amount": 500.00}`                                   |
| POST   | `/accounts/{region}/transfer`                 | `{"fromAccountNumber", "toAccountNumber", "amount"}`   |

- `amount` must be positive with at most 2 decimal places.
- Deposit and withdraw return the new `balance`; transfer returns both new balances.
- A withdrawal or transfer that would take a balance below zero fails with 400.
- A transfer is atomic: either both balances change or neither does. Both
  accounts must be in the same region, since each region is a separate database.

### Errors

Errors come back as JSON: `{"status": 400, "error": "message"}`.

| Status | When                                                              |
|--------|-------------------------------------------------------------------|
| 400    | Unknown region, missing/invalid field, account for a non-existent branch, or insufficient funds |
| 404    | Updating, deleting, or moving money in/out of a branch/account that does not exist |
| 409    | Creating a duplicate branch/account, or deleting a branch that still has accounts |

## Running the tests

```bash
cd dataaccess-ms-accounts && ./mvnw test
```

The tests cover pure logic (region parsing and the query-builder helper types)
and do not need a running database.

## Project layout

```
meraApnaBank/
├── docker-compose.yml                  # 4 regional Postgres containers
├── core-api-accounts/                  # public REST API + web UI (port 8081)
│   └── src/main/
│       ├── java/com/teresol/meraapnabank/
│       │   ├── resource/               # BranchResource, AccountResource (REST endpoints)
│       │   ├── service/                # BranchService, AccountService
│       │   ├── client/                 # BranchClient, AccountClient (REST clients -> 8083)
│       │   ├── dto/                    # *Dto, New*Request, Update*Request
│       │   └── exception/              # GlobalExceptionMapper (JSON errors)
│       └── resources/META-INF/resources/index.html   # web UI
└── dataaccess-ms-accounts/             # region-routed JDBC service (port 8083)
    ├── db/{north,south,east,west}-init.sql
    └── src/
        ├── main/java/com/teresol/meraapnabank/
        │   ├── resource/               # BranchResource, AccountResource (SQL via QueryBuilder)
        │   ├── datasource/             # Datasources (region -> datasource), RegionType
        │   ├── dto/                    # *Dto, New*Request, Update*Request
        │   ├── exception/              # GlobalExceptionMapper (JSON errors)
        │   └── util/                   # Table, Tables, Selection, QueryBuilder
        └── test/java/com/teresol/meraapnabank/
            ├── datasource/RegionTypeTest.java
            └── util/TableAndSelectionTest.java
```
