# Harmonie-dev — Backend API

## Getting started (clone & run)

Only requirement: **Docker Desktop**. `.env` is already committed with everything needed (MongoDB Atlas URI, Redis password, JWT keys, Cloudinary, mail) — no local Mongo/Redis install required, they're either cloud-hosted or containerized.

```bash
git clone <this-repo-url>
cd new/back   # if cloning the monorepo — skip this line if this repo IS new/back
docker compose up --build -d
```

The API comes up on **http://localhost:8090**. A seed Super Admin account is created automatically on first boot:

```
email:    admin@harmonie-dev.tn
password: HarmonieDev@2026
```

Check it's healthy: `curl http://localhost:8090/actuator/health` → `{"status":"UP"}`.

Logs: `docker compose logs app -f`. Stop: `docker compose down` (add `-v` to also wipe the Redis rate-limit data volume).

### Running outside Docker (e.g. from IntelliJ)

Needs a local Java 21 + Maven, **and** a Redis reachable at `REDIS_HOST:REDIS_PORT` in `.env` (currently `localhost:6380`, not the Docker-internal `6379`, to sidestep any other local Redis you might already have on the default port). Easiest way to get that: `docker compose up -d redis` starts just the Redis container with its host port mapping, then run `HarmonieDevApiApplication` normally — MongoDB is Atlas either way, no local DB needed.

## Summary (Current State)

- Spring Boot 3.5 auth API with JWT RS256, MongoDB, Redis.
- Access token via Authorization header.
- Refresh token via HttpOnly cookie and stored hashed in Mongo (now SHA-256).
- Login rate limiting (Bucket4j) and account lockout on failed attempts.
- Audit logging for auth events.
- Security headers configured.

### Strengths

- Access/refresh token separation
- Rate limiting + lockout
- Token blacklisting on logout
- Auditing and structured error responses
- Security headers

## Tests

Run all tests (uses Java 21):

```bash
.\mvnw test
```

Run tests with the test profile:

```bash
.\mvnw test -Dspring.profiles.active=test
```

Controller tests (MockMvc):

- `AuthControllerTest` covers `/register`, `/login`, `/refresh`, `/logout`, `/me`

Service tests:

- `AuthServiceTest` covers register, duplicate email, login success, lockout, refresh rotation

## CI Pipeline (GitHub Actions)

- Build & Test — compiles your code and runs all 10 unit tests. If this fails, Code Coverage and Docker jobs are skipped entirely.
- Code Quality — runs Checkstyle (code style) and SpotBugs (bug detection).
- Security Scan (OWASP) — scans your dependencies for known CVEs.
- Code Coverage (JaCoCo) — measures what % of your code is covered by tests and generates the HTML report artifact.
- Docker Build Validation — builds your Docker image and scans it with Trivy for vulnerabilities.

Artifacts:

- `jacoco-report` (225 KB) — an HTML report you can download and open in a browser to see exactly which lines of code are covered by tests and which aren't, highlighted in green/red.
- `test-results` (10.3 KB) — the XML test results showing pass/fail for each of your 10 tests.
