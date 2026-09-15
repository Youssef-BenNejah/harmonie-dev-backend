# Plans d'abonnement

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, the 🌐 marker).

Read-only reference data describing the pricing tiers shown on the public landing page and the in-app Abonnement screen.

---

## Plans (`/plans`)

**Model fields** (`Plan`)
| Field | Type | Notes |
|---|---|---|
| id | string | e.g. `"starter" \| "pro" \| "entreprise"` |
| nom | string | required |
| tagline | string | required |
| prixMensuel | number | required |
| prixAnnuel | number | required |
| populaire | boolean | optional, highlights one plan |
| fonctionnalites | string[] | required |

**Endpoints**
- `GET /plans` 🌐 — public, used by both the landing page and the in-app Abonnement screen
- `GET /plans/:id` 🌐

*(No write endpoints from this frontend — plan management would be a separate internal/admin tool if needed.)*
