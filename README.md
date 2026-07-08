# Word Games

A Quarkus REST API for an impostor-style word game. Players join a game, receive either the authentic or imposed word, take turns, vote on who they think has the imposed word, and play until the impostor is identified or survives with two players remaining.

## Design

The application follows a layered layout:

| Layer | Package | Responsibility |
|-------|---------|----------------|
| REST | `org.learning.games.resource` | HTTP endpoints, request validation, auth checks |
| API | `org.learning.games.api` | Request/response DTOs and mappers |
| Domain | `org.learning.games.domain` | Game rules, voting logic, repository interfaces, shared `domain.model` enums |
| Infrastructure | `org.learning.games.infra` | JPA repositories, rate limiting, exception mappers |
| Entities | `org.learning.games.entity` | JPA persistence models |

Authentication uses OIDC bearer tokens from [customauth.fly.dev](https://customauth.fly.dev/). The BFF (`wordgamebff`) may proxy user requests via the `X-Delegated-User-Id` header. All endpoints except health checks require a valid token.

On first startup, **Flyway** creates the `wrdgm` schema and tables automatically — no manual migration steps required.

Game flow:

1. Admin creates a game and is added as `ADMIN`.
2. Other users join (`MEMBER`).
3. Admin starts the game with a `SecretWord` (requires at least 3 players). One random player receives the imposed word; the rest receive the authentic word.
4. Active players complete their turn, then vote. Tied votes reset (up to 3 times, then a random tied player is eliminated).
5. The highest-voted player is eliminated each round until the impostor is out or only two players remain.

## Dependencies

- **Quarkus 3** — REST, CDI, Hibernate ORM, Hibernate Validator, Flyway
- **PostgreSQL** — persistent storage for games, members, and secret words
- **Quarkus OIDC** — token introspection against the configured OIDC authority
- **Bucket4j** — IP-based rate limiting (30 req/10s, 200 req/min)
- **Micrometer Prometheus** — metrics at `/q/metrics`
- **SmallRye Health** — liveness/readiness probes at `/q/health`

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/auth/me` | Current authenticated user |
| POST | `/games` | Create a game (creator becomes admin) — **201 Created** |
| GET | `/games/{id}` | Get game details (members only) |
| POST | `/games/{id}/members` | Join a game — **201 Created** |
| DELETE | `/games/{id}/members/{userId}` | Leave (self) or kick (admin) |
| POST | `/games/{id}/start` | Start game (admin only, min 3 players) |
| POST | `/games/{id}/turn/complete` | Mark turn complete; supports `Idempotency-Key` header |
| GET | `/games/{id}/my-word` | Get assigned word and type |
| POST | `/games/{id}/vote` | Cast vote; supports `Idempotency-Key` header |
| POST | `/secretwords` | Create a secret word pair — **201 Created** |
| GET | `/secretwords/random` | Random secret word (excludes words already used by admin) |
| GET | `/secretwords/{id}` | Get a secret word (authorized admins only) |
| GET | `/q/health/ready` | Readiness probe (public) |

### Idempotency

Clients should send a unique `Idempotency-Key` header on `POST /games/{id}/vote` and `POST /games/{id}/turn/complete` to safely retry network failures. Replaying the same key with the same request body returns the original response without re-applying game logic.

## Running Locally

### Prerequisites

- Java 25+
- PostgreSQL with database `wordgame`
- OAuth client registered at [customauth.fly.dev](https://customauth.fly.dev/)

### Setup

Copy environment variables and adjust as needed:

```shell
cp .env.example .env
```

Create the database:

```sql
CREATE DATABASE wordgame;
CREATE USER mainuser WITH PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE wordgame TO mainuser;
```

### Dev Mode

```shell
set -a && source .env && set +a
./mvnw quarkus:dev
```

The app listens on port **8081** by default. Flyway applies `V1__create_schema.sql` on startup. Health checks are at `http://localhost:8081/q/health/ready`.

Send `Authorization: Bearer <token>` on protected endpoints.

### Build and Test

```shell
./mvnw clean package
```

This runs 130+ integration tests and produces `target/quarkus-app/quarkus-run.jar`. OpenAPI schema files are generated to `target/openapi/` for reference (not served at runtime).

Run the packaged app:

```shell
set -a && source .env && set +a
java -jar target/quarkus-app/quarkus-run.jar
```

### Optional seed data

After first startup, optionally load word pairs:

```shell
psql -U mainuser -d wordgame -f scripts/seed-nepali-secretwords.sql
```

```shell
psql -h HOST -U USER -d DATABASE -f scripts/seed-nepali-secretwords.sql
```

## Production Deployment

Production uses the `prod` profile (`QUARKUS_PROFILE=prod`). Flyway migrates the `wrdgm` schema; Hibernate validates mappings.

### Fly.io

The app deploys to **https://wordgames-api.fly.dev** via GitHub Actions on every push to `main` (after tests pass).

**Scaling:** up to 2 Machines (512MB shared-cpu-1x each), scale-to-zero when idle (`min_machines_running = 0`), wake on demand.

**One-time setup** (before the first auto-deploy):

```shell
fly apps create wordgames-api

fly secrets set \
  QUARKUS_DATASOURCE_JDBC_URL='jdbc:postgresql://USER:PASSWORD@HOST:5432/DATABASE?sslmode=require' \
  OIDC_CLIENT_SECRET='your-client-secret'

fly tokens create deploy -a wordgames-api -x 999999h
# Add the token output as FLY_API_TOKEN in GitHub → Settings → Secrets → Actions
```

If your provider gives a `postgres://` URL, convert to JDBC first:

```
postgres://user:pass@host:5432/mydb  →  jdbc:postgresql://user:pass@host:5432/mydb?sslmode=require
```

Credentials go in the JDBC URL (`USER:PASSWORD@HOST`). Production does **not** need separate `QUARKUS_DATASOURCE_USERNAME` or `QUARKUS_DATASOURCE_PASSWORD` secrets. Schema `wrdgm` is configured in the app (not in the connection string).

**Fly secrets:**

| Secret | Purpose |
|--------|---------|
| `QUARKUS_DATASOURCE_JDBC_URL` | Full JDBC URL with embedded user and password |
| `OIDC_CLIENT_SECRET` | Introspection secret for `wordgamebff` client |

If you need direct browser access (not via the BFF), set `CORS_ORIGINS` in [`fly.toml`](fly.toml) `[env]` — it is not a secret.

**Manual deploy** (optional):

```shell
fly deploy --remote-only
fly scale count 2
```

**Verify:**

```shell
fly status
fly logs
curl https://wordgames-api.fly.dev/q/health/ready
```

### Docker Compose

```shell
export QUARKUS_DATASOURCE_PASSWORD=your-db-password
export OIDC_AUTH_SERVER_URL=https://customauth.fly.dev/
export OIDC_CLIENT_ID=wordgamebff
export OIDC_CLIENT_SECRET=your-client-secret

docker compose up --build
```

This starts PostgreSQL and the Quarkus app. The app image is built from `src/main/docker/Dockerfile.jvm`.

### Environment Variables

| Variable | Description |
|----------|-------------|
| `QUARKUS_DATASOURCE_JDBC_URL` | JDBC URL (default: `jdbc:postgresql://localhost:5432/wordgame`). In production, embed user and password in the URL. |
| `QUARKUS_DATASOURCE_USERNAME` | Database user (local dev only; optional if credentials are in the JDBC URL) |
| `QUARKUS_DATASOURCE_PASSWORD` | Database password (local dev only; optional if credentials are in the JDBC URL) |
| `QUARKUS_HTTP_PORT` | HTTP port (default: `8081`) |
| `OIDC_AUTH_SERVER_URL` | OIDC authority URL (required) |
| `OIDC_CLIENT_ID` | Introspection client id (default: `wordgamebff`) |
| `OIDC_CLIENT_SECRET` | Introspection client secret (required) |
| `OIDC_TOKEN_AUDIENCE` | Expected token audience (default: `resource_server`) |
| `BFF_CLIENT_ID` | Trusted BFF client for delegated user headers (default: `wordgamebff`) |
| `CORS_ORIGINS` | Comma-separated allowed origins for direct browser clients (local dev default: `http://localhost:3000`; optional in `fly.toml` for production) |

### Security

- All routes except `/q/health` require authentication.
- IP-based rate limiting is enabled by default (`app.rate-limit.enabled=true`).
- Standard security headers (X-Content-Type-Options, X-Frame-Options, etc.) are set on all responses.
- Swagger UI and the `/q/openapi` endpoint are disabled at runtime; API docs are generated at build time only.

## OpenAPI Documentation

Generate reference documentation during build:

```shell
./mvnw clean package
ls target/openapi/
```

The generated YAML/JSON files describe all REST endpoints and the bearer token security scheme.
