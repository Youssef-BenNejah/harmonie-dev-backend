# Facturation — Factures / Devis / Bons de livraison

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, list query params).

Covers all three document types (the printed document type lives in `status`, not a separate resource) and both invoice "families" (`type`: `Standard` sales/purchase invoices vs `Proforma` quotes-that-can-convert).

---

## Factures (`/invoices`)

**Model fields** (`Invoice`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| client | `Client` (populated) | `clientId` accepted on write |
| number | number | server-assigned, sequential per `(type, year)` |
| year | number | server-set from `date` |
| currency | `Currency` (populated) | `currencyId` accepted on write |
| status | `"Facture" \| "Devis" \| "Bon de livraison"` | **document type**, not workflow state |
| paymentStatus | `"impayé" \| "Partiellement payé" \| "Payé" \| "Retard"` | server-derived from `paidAmount` vs `total` (and due date for `Retard`) — not client-writable |
| type | `"Standard" \| "Proforma"` | required, immutable after creation |
| isConverted | boolean | true once a Proforma has been converted into a Standard invoice |
| date | ISO date | required (issue date) |
| expirationDate | ISO date | required (due date) |
| note | string | optional |
| items | `InvoiceItem[]` | see below, at least one required |
| timbre | number | Tunisian fiscal stamp duty, flat amount |
| subtotal | number | server-computed: tax-inclusive sum of items + `timbre` |
| taxAmount | number | server-computed: sum of item-level `taxAmount` |
| total | number | server-computed: `subtotal` (kept identical, `timbre` already included) |
| paidAmount | number | server-computed, sum of linked `Payment.amountPaid` |
| createdBy | string (tenant id) | server-set |
| created | ISO date | server-set |
| factureImage | string \| null | optional scanned/attached image URL |

### `InvoiceItem` sub-fields (embedded array, each item snapshots its own tax)

| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| ref | string | optional |
| article | string | required |
| description | string | optional |
| quantity | number | required |
| price | number | required, unit price |
| taxId | string | optional link to `Tax`; empty = no tax |
| taxRate | number | server-snapshotted from `Tax.taxvalue` at save time |
| taxAmount | number | server-computed: `quantity*price*taxRate/100` |
| taxName | string | server-snapshotted from `Tax.name` |
| total | number | server-computed: `quantity*price + taxAmount` |

## Endpoints

- `GET /invoices` — list. Query: `page, limit, search (number/client name), type (Standard/Proforma), status, paymentStatus, clientId, dateFrom, dateTo, sort`
- `GET /invoices/:id`
- `POST /invoices` — body: `clientId, currencyId, status, type, date, expirationDate, note?, timbre, items: [{ ref?, article, description?, quantity, price, taxId? }]`
- `PATCH /invoices/:id` — same body shape as create, partial
- `DELETE /invoices/:id`
- `POST /invoices/:id/duplicate` — body: none. Returns a new invoice with a fresh `id`/`number`, `paymentStatus="impayé"`, `paidAmount=0`, `isConverted=false`
- `POST /invoices/:id/convert` — body: none. Only valid when `type="Proforma"`. Marks source `isConverted=true`, creates a duplicate with `type="Standard"`
- `POST /invoices/:id/send` — body: `email? (defaults to client's email)`. Emails the invoice PDF to the client
- `GET /invoices/:id/pdf` — streams/returns the generated invoice PDF (server-side render of the same layout the frontend currently builds client-side)
- `GET /invoices/export/summary` — body/query: `ids[]` or same filters as the list endpoint. Returns a summary PDF (or the JSON the frontend renders into one)
- `GET /invoices/export/zip` — query: `ids[]`. Returns a ZIP of individual invoice PDFs

Payments recorded against invoices are covered in [05-payments.md](05-payments.md).
