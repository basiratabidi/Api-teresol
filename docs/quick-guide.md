# Quick Guide

Get Mera Apna Bank running and do the everyday tasks in about ten minutes.
For requirements see the [SRS](SRS.md); for design, the [SDS](SDS.md).

---

## 1. Start it (developers)

**You need:** Java 17 or newer, Docker with Docker Compose. Maven is bundled (`./mvnw`).

Open three terminals in the `meraApnaBank` folder.

| # | Command | What it does |
|---|---------|--------------|
| 1 | `docker compose up -d` | Starts the 4 regional databases |
| 2 | `cd dataaccess-ms-accounts && ./mvnw quarkus:dev` | Data service on port 8083 |
| 3 | `cd core-api-accounts && ./mvnw quarkus:dev` | Web app + API on port 8081 |

Then open **<http://localhost:8081>**.

Optional but recommended, so logins survive a restart — before step 3:
```bash
export AUTH_SECRET="$(openssl rand -base64 48)"
```

**Stop everything:** press `q` or Ctrl+C in the two Maven terminals, then `docker compose down` (add `-v` to erase all data).

---

## 2. First sign-in

1. On the login screen click **Sign up**.
2. Enter your full name, email and a password of **8+ characters**. The bar under the password shows its strength. Repeat the password and click **Create account**.
3. You are signed in and land on the **Dashboard**.

Next time use **Log in**. Use **Log out** at the bottom of the left menu. Sessions last 8 hours.

---

## 3. Everyday tasks

**Regions.** Data is split into North, South, East and West. Always pick the
region first. Codes must start with the region's prefix:

| Region | Branch code | Account number |
|--------|-------------|----------------|
| North | `NO-001` | `NO-AC-0001` |
| South | `SO-001` | `SO-AC-0001` |
| East | `EA-001` | `EA-AC-0001` |
| West | `WE-001` | `WE-AC-0001` |

### See the overview
**Dashboard** shows the number of branches and accounts in each region. Click a
quick-access card to jump to that page.

### Branches
| To… | Do this |
|-----|---------|
| List | **Branches** → pick a region → **Load branches** |
| Create | *Create branch*: code (e.g. `NO-003`), name, city, region → **Create branch** |
| Update | *Update or delete*: region, code, new name, new city → **Update** |
| Delete | Same card → enter region and code → **Delete** → confirm. A branch that still has accounts can't be deleted. |

### Accounts
| To… | Do this |
|-----|---------|
| List | **Accounts** → pick a region → **Load accounts** |
| Open | *Open account*: number (`NO-AC-0003`), holder, an **existing** branch code, type (SAVINGS/CURRENT), opening balance, region → **Open account** |
| Update | *Update or close*: region, account number, holder, type, balance → **Update** |
| Close | Same card → region and account number → **Close account** → confirm |

### Money
Go to **Transactions**, choose a tab, pick the region.

| Tab | Fields | Notes |
|-----|--------|-------|
| **Deposit** | account, amount | Adds money; the new balance is shown |
| **Withdraw** | account, amount | Refused if the balance is too low |
| **Transfer** | from, to, amount | Both accounts must be in the **same region**; you'll be asked to confirm |

Amounts are in PKR and must be greater than zero. Press **Enter** in a form to submit it.

---

## 4. Try it with the sample data

Each region starts with two branches and two accounts, for example:

| Account | Holder | Balance |
|---------|--------|---------|
| `NO-AC-0001` | Ayesha Khan | PKR 15,000.00 |
| `NO-AC-0002` | Bilal Ahmed | PKR 42,000.50 |

Quick check: **Transactions → Transfer**, region *North*, from `NO-AC-0001`,
to `NO-AC-0002`, amount `250` → confirm. Then **Accounts → North → Load
accounts** — the balances moved by 250.

---

## 5. Using the API directly

```bash
# 1. get a token
TOKEN=$(curl -s -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"your password"}' \
  http://localhost:8081/auth/login | jq -r .token)

# 2. call the API with it
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/branches/north
curl -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"amount":500}' http://localhost:8081/accounts/north/NO-AC-0001/deposit
```

Full endpoint list: [main README](../README.md#api-overview). Interactive docs
(dev mode): <http://localhost:8081/q/swagger-ui>.

---

## 6. Troubleshooting

| Symptom | Likely cause and fix |
|---------|----------------------|
| Page won't open on 8081 | Core API isn't running, or another program uses port 8081. Start step 3 and read its log. |
| Dashboard shows `-`, or "Connection refused" | Data service (8083) or databases are down. Run steps 1 and 2. |
| "Please sign in to continue." | Session expired, or the API restarted without `AUTH_SECRET`. Log in again. |
| "Incorrect email or password." | Check the email; passwords are case-sensitive. Sign up if you have no account. |
| "An account with this email already exists." | Use **Log in** instead. |
| Password rejected | It must have at least 8 characters. |
| "Insufficient funds…" | The withdrawal or transfer is larger than the balance. |
| "must start with the region prefix" | Use `NO-`, `SO-`, `EA-` or `WE-` matching the selected region. |
| Opening an account gives a long database error | The branch code doesn't exist in that region — create the branch first. |
| No 3D coins | Your browser has WebGL disabled; everything else works. |
| Old page after an update | Hard-refresh: Ctrl+Shift+R. |
| Want a clean slate | `docker compose down -v && docker compose up -d` (erases all bank data). Delete `core-api-accounts/users.json` to remove all users. |

---

## 7. Cheat sheet

| I want to… | Where |
|------------|-------|
| Sign up / log in / log out | Login screen / sidebar bottom |
| See totals per region | Dashboard |
| Add or remove a branch | Branches |
| Open or close an account | Accounts |
| Deposit / withdraw / transfer | Transactions |
| Ports | UI+API `8081`, data service `8083`, DBs `55432–55435` |
| Sign-in lifetime | 8 hours |
