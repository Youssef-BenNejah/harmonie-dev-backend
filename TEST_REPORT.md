# Harmonie-dev — Backend wiring & test report

Everything below was run against the **real stack**: Spring Boot 3.5 on Java 21 (built and run inside Docker), a **real MongoDB Atlas** cluster (`harmoniedev` database), a **real Redis** container, and **real Gmail SMTP** — no mocks. Every request in this report is an actual `curl`/browser call against `http://localhost:8090`, and every e-mail was actually sent (`MailService` logs confirm zero send failures).

## 1. New domains scaffolded (clean layered architecture)

Following your `controller / domain{dto/request,dto/response,enums,model} / repository / service` structure:

- **`person`** — full CRUD (`/api/v1/persons`)
- **`entreprise`** — full CRUD (`/api/v1/entreprises`)
- **`client`** — read + convert-to-client, wired from both Person and Entreprise (`/api/v1/clients`, `POST /persons/{id}/convert-to-client`, `POST /entreprises/{id}/convert-to-client`)

All three compiled and were smoke-tested (create person → convert to client → list clients — see §4).

## 2. Auth additions

- `POST /api/v1/auth/admin/users` 🛡️ ADMIN-only — creates a tenant user, generates a password server-side, **e-mails the credentials**, never returns the password over the API.
- `POST /api/v1/auth/forgot-password` — public, always responds success (no email enumeration), sends a 6-digit code by e-mail, stored in Redis with a 10-minute TTL.
- `POST /api/v1/auth/verify-reset-code` — public, exchanges the code for a one-time reset token (Redis, 10-minute TTL).
- `POST /api/v1/auth/reset-password` — public, applies the new password and **revokes every existing refresh token** for that user (forces re-login everywhere).
- `UserDocument` extended with `firstName`, `lastName`, `status` (ACTIVE/SUSPENDED/DISABLED/EXPIRED), `planExpiresAt`, `renewalRequested` — this is the same account a future "SuperAdmin — Tenants" screen would manage; no separate `TenantAdmin` collection was created (a `TenantAdmin` and a `User` are the same real-world entity).

## 3. Join requests (new domain)

- `POST /api/v1/join-requests` 🌐 public — the "Rejoindre" landing form posts here.
- `GET /api/v1/join-requests` 🛡️, `PATCH /{id}/contact`, `PATCH /{id}/reject`, `POST /{id}/convert` 🛡️ — convert creates the real user via the same `AuthService.createUserByAdmin` path (so it also e-mails credentials) and marks the request `CONVERTED`.

## 4. Mail

- `spring-boot-starter-mail` wired to your Gmail SMTP settings (`smtp.gmail.com:587`, STARTTLS, the app-password you provided).
- Two HTML templates (welcome + password-reset code), same deep-ocean-blue Harmonie-dev branding, **logo embedded inline via CID** (not hot-linked, so it renders even with images blocked), responsive single-column email-safe table layout.
- Visual proof: rendered from the exact template and screenshotted in-browser (see chat) — gradient header with the real logo, credentials card, CTA button, all in the Harmonie-dev deep-ocean-blue palette.

## 5. Seed admin

Created automatically on first startup (`AdminAccountSeeder`, only runs if no ADMIN exists yet):

```
email:    admin@harmonie-dev.tn
password: HarmonieDev@2026
```

Set via `SEED_ADMIN_EMAIL` / `SEED_ADMIN_PASSWORD` in `.env` — change or remove those to disable reseeding.

## 6. What was actually tested (all passed)

| # | Test | Result |
|---|---|---|
| 1 | Compile main + test sources (JDK 17 proxy for 21, since no JDK 21 is installed locally) | ✅ clean |
| 2 | `mvn test` — 10 existing unit/controller tests | ✅ 10/10 pass |
| 3 | Real Docker build (JDK 21) + boot against Mongo Atlas + Redis | ✅ started in ~9s, connected to Atlas replica set |
| 4 | `AdminAccountSeeder` ran on fresh boot | ✅ seeded `admin@harmonie-dev.tn` |
| 5 | `POST /auth/login` with seeded admin | ✅ 200, JWT + refresh cookie issued |
| 6 | `POST /auth/admin/users` (create a normal user) | ✅ 200, user created, **welcome e-mail sent** (log-confirmed) |
| 7 | `POST /auth/forgot-password` → real Redis code → `POST /auth/verify-reset-code` → `POST /auth/reset-password` | ✅ full chain, **e-mail sent**, login with the new password succeeded |
| 8 | `POST /join-requests` (public, no token) → `GET` (admin) → `PATCH /contact` → `POST /convert` | ✅ full chain, new user created, **welcome e-mail sent** |
| 9 | Unauthenticated access to `/join-requests` and `/auth/admin/users` | ✅ 401 as expected |
| 10 | A plain `USER` token hitting admin-only endpoints | ✅ 403 as expected (role gating works, not just "any authenticated user") |
| 11 | `POST /persons` → `POST /persons/{id}/convert-to-client` → `GET /clients` | ✅ full chain |
| 12 | Frontend: real login through the UI (`/login`) against the running backend | ✅ redirected to `/superadmin`, correct role-based routing |
| 13 | Frontend: "Ajouter un utilisateur" in Super Admin → real `POST /auth/admin/users` | ✅ toast success, **e-mail sent** (log-confirmed) |
| 14 | Frontend: `/rejoindre` public form → real `POST /join-requests` | ✅ "Demande envoyée" screen |
| 15 | Frontend: Super Admin → Demandes d'adhésion → list (real data) → Convertir | ✅ "Compte créé et identifiants envoyés par e-mail", **e-mail sent** (log-confirmed) |
| 16 | `bun run build` (frontend) after all wiring changes | ✅ clean build |

Total: **5 real e-mails sent** during this test pass, zero send failures logged.

## 7. Frontend wiring

- `src/lib/api.ts` — new thin client: JWT in memory + `sessionStorage`, auto-refresh-on-401 (calls `/auth/refresh` once via the httpOnly cookie, retries), typed helpers for every endpoint above.
- `login.tsx` — real `POST /auth/login`, routes ADMIN → `/superadmin`, USER → `/tableau-de-bord`.
- `mot-de-passe-oublie.tsx` — real 3-step forgot-password flow (no more fake `setTimeout`).
- `rejoindre.tsx` — real `POST /join-requests`.
- `superadmin_.demandes.tsx` — rewired to `@tanstack/react-query` against the real backend (list/contact/reject/convert); the password-preview UI was removed since the backend never returns the password over the API (it's e-mailed).
- `superadmin.tsx` — "Ajouter un utilisateur" now calls the real `POST /auth/admin/users`; the client-side password generator/preview UI was removed for the same reason.

## 8. Known gaps / next steps (explicitly out of scope this pass)

- The **"Utilisateurs actuels" table** in Super Admin still reads from the old local mock store (`useTenantAdmins`) — a user created via the real endpoint won't appear there yet. No `GET /admin/users` list endpoint exists on the backend yet; that's the natural next step (mirrors the `join-requests` list pattern already built).
- `Personnes` / `Entreprises` / `Clients` screens in the frontend are **not yet wired** to the new real endpoints — only the backend domains + a curl/API smoke test were done this pass, per your "let's start first with auth" prioritization.
- The `Approuver`/`Rejeter` renewal-request workflow and `PATCH /admin/tenants/:id` (état/planExpiration edit) are documented in `docs/09-superadmin.md` but not implemented yet.
- Frontend is currently pointed at `http://localhost:8090` (`new/front/.env`, `VITE_API_URL`) — the Netlify production deployment still runs on the old mock store; the real backend isn't deployed anywhere public yet.

## 9. How to run it yourself

```bash
cd new/back
docker compose up --build -d   # Spring Boot (Java 21) + Redis, real Mongo Atlas
```

Backend on `http://localhost:8090`. Frontend:

```bash
cd new/front
bun run dev -- --port 5180
```

Then log in at `http://localhost:5180/login` with `admin@harmonie-dev.tn` / `HarmonieDev@2026`.
