# Harmonie-dev — Backend API

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
