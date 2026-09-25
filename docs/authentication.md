# Authentication

`core-api-accounts` has built-in signup and login. It uses only the JDK
(`javax.crypto`) plus Jackson, so there are no extra dependencies. The code is
in `meraApnaBank/core-api-accounts/src/main/java/com/teresol/meraapnabank/auth/`.

| Class | Responsibility |
|-------|----------------|
| `AuthService` | Stores users, hashes/verifies passwords, issues and validates tokens |
| `AuthResource` | `POST /auth/signup`, `POST /auth/login`, `GET /auth/me` |
| `AuthFilter` | JAX-RS request filter that enforces a bearer token on protected paths |

## Flow

```
Browser                         core-api-accounts (8081)
  |  POST /auth/signup|login  ->  AuthService checks/creates user
  |  <-  {token, expiresAt, user}
  |
  |  GET /accounts/north
  |  Authorization: Bearer <token>  ->  AuthFilter verifies token
  |                                     -> AccountResource -> dataaccess-ms (8083)
```

1. The user signs up or logs in and receives a **token**.
2. The web UI keeps the token in `localStorage` (`mab.token`) and sends it as
   `Authorization: Bearer <token>` on every API call.
3. `AuthFilter` runs before each request. It protects `/accounts/*`,
   `/branches/*` and `/auth/me`; `/auth/signup`, `/auth/login`, the static UI
   and `OPTIONS` requests are open. A missing, expired or tampered token gets
   `401 {"status":401,"error":"Please sign in to continue."}`.
4. On a `401` the UI clears the stored token and returns to the login screen.
5. **Log out** deletes the token client-side. Tokens are stateless, so there's
   no server-side session to revoke; a token simply expires.

## Passwords

- Hashed with **PBKDF2-HMAC-SHA256**, 120,000 iterations, a random 16-byte
  salt per user, 256-bit output.
- Verified with a constant-time comparison. Login also hashes when the email
  is unknown, so response time doesn't reveal which emails are registered.
- Rules: at least 8 characters. The UI adds a strength meter (weak → strong).
- The plaintext password is never stored or logged.

## Tokens

Format: `base64url(email|expiryEpochSeconds) . base64url(HMAC-SHA256(payload))`.

- Signed with the server secret; changing any byte invalidates the signature.
- Valid until `expiresAt` (default 8 hours) **and** only while the user still
  exists.
- Not encrypted — the payload contains the email, so don't put anything more
  sensitive in it.

## User storage

Users are kept in memory and persisted to a JSON file (`users.json` in the
working directory by default), keyed by lower-cased email:

```json
{
  "ayesha@example.com": {
    "fullName": "Ayesha Khan",
    "email": "ayesha@example.com",
    "salt": "<base64url>",
    "hash": "<base64url>"
  }
}
```

The file is git-ignored. Delete it to remove all users.

## Configuration

| Property | Env var | Default |
|----------|---------|---------|
| `auth.secret` | `AUTH_SECRET` | random per start (sessions die on restart) |
| `auth.users-file` | `AUTH_USERS_FILE` | `users.json` |
| `auth.token-ttl-seconds` | — | `28800` |

Generate a secret with `openssl rand -base64 48` and export it before
starting the service:

```bash
export AUTH_SECRET="$(openssl rand -base64 48)"
./mvnw quarkus:dev
```

## Validation and error responses

| Situation | Status | Message |
|-----------|--------|---------|
| Name shorter than 2 characters | 400 | Please enter your full name. |
| Malformed email | 400 | Please enter a valid email address. |
| Password shorter than 8 | 400 | Password must be at least 8 characters. |
| Email already registered | 409 | An account with this email already exists. |
| Wrong email or password | 401 | Incorrect email or password. |
| No/invalid/expired token | 401 | Please sign in to continue. |

## Limitations and hardening ideas

- One role: every signed-in user can perform every banking action.
- No login rate limiting or account lockout — add one before exposing this
  publicly.
- No password reset or email verification.
- `users.json` isn't suitable for multiple instances; move users into a
  database for that.
- `dataaccess-ms-accounts` (8083) does not check tokens. Keep it on a private
  network; only the core API should reach it.
- Serve the UI over HTTPS in production, since the token travels in a header.
- For anything beyond a demo, consider Quarkus' `quarkus-smallrye-jwt` or an
  OIDC provider instead of this hand-rolled scheme.
