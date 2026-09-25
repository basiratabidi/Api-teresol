# core-api-accounts

The public-facing service of Mera Apna Bank (port **8081**). It:

- serves the web UI (`src/main/resources/META-INF/resources/`),
- handles signup/login and enforces bearer-token authentication,
- exposes the branches/accounts/transactions REST API and forwards it to
  `dataaccess-ms-accounts` (port 8083) through a REST client.

It contains no SQL. For the full picture, endpoints and setup of the whole
project, see the [main README](../../README.md). Related docs:
[authentication](../../docs/authentication.md) and
[frontend](../../docs/frontend.md).

## Run in dev mode

Start the databases and `dataaccess-ms-accounts` first (see the main README), then:

```bash
./mvnw quarkus:dev
```

Open <http://localhost:8081/>. Quarkus reloads Java and static-file changes
automatically.

To keep sessions across restarts, set a signing secret first:

```bash
export AUTH_SECRET="$(openssl rand -base64 48)"
```

## Configuration

`src/main/resources/application.properties`:

| Property | Default | Meaning |
|----------|---------|---------|
| `quarkus.http.port` | `8081` | HTTP port |
| `branch-data-access/mp-rest/url` | `http://localhost:8083` | Data-access service URL |
| `auth.secret` | `${AUTH_SECRET:}` (random if empty) | Token signing key |
| `auth.users-file` | `${AUTH_USERS_FILE:users.json}` | User store file (git-ignored) |
| `auth.token-ttl-seconds` | `28800` | Token lifetime |

## Source layout

```
src/main/java/com/teresol/meraapnabank/
├── auth/        AuthService, AuthResource, AuthFilter
├── resource/    BranchResource, AccountResource
├── service/     BranchService, AccountService
├── client/      BranchClient, AccountClient (REST clients)
├── dto/         request/response types
└── exception/   GlobalExceptionMapper
```

## Package and run

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

An über-jar: `./mvnw package -Dquarkus.package.jar.type=uber-jar`, then
`java -jar target/*-runner.jar`.

Native executable (needs GraalVM, or a container build):

```bash
./mvnw package -Dnative
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

Dockerfiles are in `src/main/docker/`.

## API docs

In dev mode, Swagger UI and the OpenAPI schema are available at
<http://localhost:8081/q/swagger-ui> and <http://localhost:8081/q/openapi>.
Note that Swagger UI's "Try it out" needs an `Authorization: Bearer <token>`
header on the protected endpoints.

## Health check

```bash
curl -i http://localhost:8081/accounts/north   # 401 without a token, 200 with one
```
