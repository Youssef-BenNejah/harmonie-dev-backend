# Dépenses — Catégories de dépenses, Dépenses

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, list query params).

---

## Catégories de dépenses (`/expense-categories`)

**Model fields** (`DepenseCategory`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| name | string | required |
| description | string | optional |
| color | string | hex color, required |
| enabled | boolean | required |
| createdBy | string (tenant id) | server-set |

**Endpoints**
- `GET /expense-categories` — list. Query: `page, limit, search, enabled`
- `GET /expense-categories/:id`
- `POST /expense-categories` — body: `name, description?, color, enabled`
- `PATCH /expense-categories/:id`
- `DELETE /expense-categories/:id`

---

## Dépenses (`/expenses`)

**Model fields** (`Depense`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| name | string | required |
| depenseCategory | `{ id, name, color }` | denormalized snapshot of `DepenseCategory` at creation time; `depenseCategoryId` accepted on write |
| currency | `"USD" \| "EUR" \| "GBP" \| "JPY" \| "CNY" \| "INR"` | required, closed enum (independent of the tenant's `Currency` table) |
| price | number | required |
| description | string | optional |
| reference | string | optional |
| createdBy | string (tenant id) | server-set |
| created | ISO date | server-set |

**Endpoints**
- `GET /expenses` — list. Query: `page, limit, search (name/reference), depenseCategoryId, currency, dateFrom, dateTo, sort`
- `GET /expenses/:id`
- `POST /expenses` — body: `name, depenseCategoryId, currency, price, description?, reference?`
- `PATCH /expenses/:id`
- `DELETE /expenses/:id`
