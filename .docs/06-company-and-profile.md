# Entreprise & profil — paramètres tenant

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, list query params).

---

## Ma entreprise / paramètres (`/company-settings`)

Singleton per tenant — the company info printed on every invoice.

**Model fields** (`CompanySettings`)
| Field | Type | Notes |
|---|---|---|
| name | string | required |
| matriculefisc | string | required — printed on every invoice header |
| address | string | required |
| state | string | governorate/region |
| country | string | required |
| email | string | required |
| phone | string | required |
| website | string | optional |
| taxNumber | string | optional |
| vatNumber | string | optional |
| registrationNumber | string | optional |
| logo | string \| null | URL after upload |

**Endpoints**
- `GET /company-settings` — returns the current tenant's singleton record
- `PATCH /company-settings` — body: any subset of the fields above except `logo`
- `POST /company-settings/logo` — multipart upload, field `file` (PNG/JPG, ≤2 MB per the UI hint). Returns `{ logo: string }`; server also updates the singleton record

---

## Mon profil (`/me`)

The authenticated tenant admin's own profile (non-auth fields only — password change is intentionally excluded per the auth-endpoints exclusion, though note it lives on this same screen in the UI).

**Model fields** (`AdminProfile`)
| Field | Type | Notes |
|---|---|---|
| name | string | required |
| surname | string | required |
| email | string | required, read-only from this screen in the current UI |
| role | `"owner"` | fixed for now |
| photo | string \| null | URL after upload |
| etat | `AdminEtat` | read-only here — only SuperAdmin can change it (see [09-superadmin.md](09-superadmin.md)) |
| planExpiration | ISO date | read-only here — only SuperAdmin can change it |

**Endpoints**
- `GET /me` — current profile
- `PATCH /me` — body: `name?, surname?` (only editable fields exposed by the current UI)
- `POST /me/photo` — multipart upload, field `file`. Returns `{ photo: string }`
