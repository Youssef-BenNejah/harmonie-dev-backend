# Catalogue — Catégories de services, Services, Devises, Taxes

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, list query params).

---

## Catégories de services (`/service-categories`)

**Model fields** (`ProductCategory`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| name | string | required |
| description | string | optional |
| color | string | hex color, required |
| enabled | boolean | required, default true |
| createdBy | string (tenant id) | server-set |

**Endpoints**
- `GET /service-categories` — list. Query: `page, limit, search, enabled`
- `GET /service-categories/:id`
- `POST /service-categories` — body: `name, description?, color, enabled`
- `PATCH /service-categories/:id`
- `DELETE /service-categories/:id`

---

## Services (`/services`)

**Model fields** (`Product`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| name | string | required |
| currency | string | currency code, required |
| price | number | required |
| description | string | optional |
| reference | string | optional SKU/ref code |
| categoryId | string | optional link to `ProductCategory` |
| createdBy | string (tenant id) | server-set |
| created | ISO date | server-set |

**Endpoints**
- `GET /services` — list. Query: `page, limit, search (name/reference), categoryId, currency, sort`
- `GET /services/:id`
- `POST /services` — body: `name, currency, price, description?, reference?, categoryId?`
- `PATCH /services/:id`
- `DELETE /services/:id`

---

## Devises (`/currencies`)

**Model fields** (`Currency`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| name | string | required (e.g. "Dinar") |
| code | string | required, unique per tenant (e.g. "TND") |
| symbol | string | required (e.g. "DT") |
| createdBy | string (tenant id) | server-set |

**Endpoints**
- `GET /currencies` — list. Query: `page, limit, search`
- `GET /currencies/:id`
- `POST /currencies` — body: `name, code, symbol`
- `PATCH /currencies/:id`
- `DELETE /currencies/:id`

---

## Taxes (`/taxes`)

**Model fields** (`Tax`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| name | string | required (e.g. "TVA standard") |
| taxvalue | number | required, percentage |
| isActive | boolean | required |
| isDefault | boolean | required — only one tax per tenant should be default (server enforces) |
| createdBy | string (tenant id) | server-set |

**Endpoints**
- `GET /taxes` — list. Query: `page, limit, search, isActive`
- `GET /taxes/:id`
- `POST /taxes` — body: `name, taxvalue, isActive, isDefault`
- `PATCH /taxes/:id`
- `DELETE /taxes/:id`
- `POST /taxes/:id/set-default` — body: none. Unsets `isDefault` on every other tax for the tenant.
