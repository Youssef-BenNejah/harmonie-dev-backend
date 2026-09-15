# SuperAdmin — Tenants, renouvellement, demandes d'adhésion

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, list query params, the 🛡️ / 🌐 markers).

---

## Utilisateurs (`/admin/tenants`) 🛡️

Platform-wide management of every tenant account. SuperAdmin-only.

**Model fields** (`TenantAdmin`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| name | string | required |
| surname | string | required |
| email | string | required, unique |
| etat | `"Active" \| "Suspendue" \| "Désactivé" \| "expiré"` | required |
| planExpiration | ISO date | required |
| created | ISO date | server-set |
| renewalRequested | boolean | server default false, set by the tenant-side renewal request |

**Endpoints**
- `GET /admin/tenants` 🛡️ — list. Query: `page, limit, search (name/email), etat, renewalRequested, sort`
- `GET /admin/tenants/:id` 🛡️
- `POST /admin/tenants` 🛡️ — body: `name, surname, email, etat, planExpiration`. Server generates an initial password and returns it once: `{ tenant: TenantAdmin, password: string }`
- `PATCH /admin/tenants/:id` 🛡️ — body: `etat?, planExpiration?`
- `DELETE /admin/tenants/:id` 🛡️
- `POST /admin/tenants/:id/reset-password` 🛡️ — body: none. Regenerates and returns a new password: `{ password: string }`

---

## Renewal workflow (tenant-facing + SuperAdmin-facing)

- `POST /subscription/request-renewal` — (tenant-facing, no 🛡️) body: none. Sets `renewalRequested=true` on the current tenant and fires a `renewal_request` notification to SuperAdmin (see [08-notifications.md](08-notifications.md))
- `POST /admin/tenants/:id/approve-renewal` 🛡️ — body: `extensionDays? (default 30)`. Extends `planExpiration`, sets `etat="Active"`, clears `renewalRequested`, clears related notifications
- `POST /admin/tenants/:id/reject-renewal` 🛡️ — body: none. Clears `renewalRequested` without extending the plan

---

## Demandes d'adhésion (`/join-requests`)

Public lead-capture from the marketing landing page, reviewed and converted by SuperAdmin.

**Model fields** (`JoinRequest`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| nom | string | required |
| prenom | string | required |
| email | string | required |
| telephone | string | required |
| entreprise | string | required, free-text company name |
| message | string | optional |
| status | `"En attente" \| "Contacté" \| "Converti" \| "Rejeté"` | server default `"En attente"` |
| created | ISO date | server-set |

**Endpoints**
- `POST /join-requests` 🌐 *(public, no auth)* — body: `nom, prenom, email, telephone, entreprise, message?`. Fires a `join_request` notification to SuperAdmin
- `GET /join-requests` 🛡️ — list. Query: `page, limit, search, status, sort`
- `GET /join-requests/:id` 🛡️
- `PATCH /join-requests/:id/contact` 🛡️ — body: none. Sets `status="Contacté"`
- `PATCH /join-requests/:id/reject` 🛡️ — body: none. Sets `status="Rejeté"`
- `POST /join-requests/:id/convert` 🛡️ — body: `etat? (default "Active"), trialDays? (default 15)`. Creates a new `TenantAdmin` from the request's contact info, sets `status="Converti"`, generates credentials, and **sends them by e-mail**. Returns `{ tenant: TenantAdmin, password: string }`
