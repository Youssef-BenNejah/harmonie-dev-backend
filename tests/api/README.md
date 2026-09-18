# API test suite

Black-box checks against a running backend: anonymous access, roles, tenant isolation for every
resource, invoices/payments, validation, uploads, web security (CORS, Swagger, headers), sessions
(cookie flags, refresh rotation and reuse detection, logout), rate limits and pagination.

**Run it only against a disposable environment** — it registers users and creates data, and the
rate-limit and session sections deliberately trip limits (it waits 61 s once for the login limiter).

```bash
ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD='the-seeded-admin-password' \
FRONTEND_ORIGIN=https://app.example.com \
BASE_URL=http://localhost:8090 \
bash tests/api/run.sh
```

| Variable | Default | Meaning |
|---|---|---|
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | required | The seeded ADMIN (used to give the two test customers the Pro plan) |
| `BASE_URL` | `http://localhost:8090` | Backend root |
| `FRONTEND_ORIGIN` | `http://127.0.0.1:5190` | Must be listed in `ALLOWED_ORIGINS`; another origin must be refused |
| `A_EMAIL`, `C_EMAIL` | `tests-a@example.com`, `tests-b@example.com` | The two customers (registered if missing) |
| `CUSTOMER_PASSWORD` | `Client#Pass2026` | Their password |
| `BULK` | `230` | Persons created for the pagination checks (`0` skips them) |

Leave `TRUSTED_PROXIES` unset when running the suite: it checks that a rotating `X-Forwarded-For`
header cannot dodge the rate limits.

The backend must run with `APP_ENV=prod` (so CORS and Swagger are locked down) and a reachable
Redis and MongoDB. Exit status is non-zero if any check fails. Uploading a real image to Cloudinary
is not covered (needs a Cloudinary account).
