# Paiements

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, list query params).

Payments are recorded against a single invoice ([04-invoicing.md](04-invoicing.md)); an invoice can have many partial payments.

---

## Paiements (`/payments`)

**Model fields** (`Payment`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| invoice | `{ id, number, year, type, total, currency }` | denormalized snapshot; `invoiceId` accepted on write |
| amountPaid | number | required, must be > 0 and not push `Invoice.paidAmount` past `Invoice.total` |
| paymentMethod | `"Virement bancaire" \| "Espèces" \| "Autres"` | required |
| paymentDate | ISO date | required |
| createdBy | string (tenant id) | server-set |

**Endpoints**
- `GET /payments` — list. Query: `page, limit, invoiceId, paymentMethod, dateFrom, dateTo, sort`
- `GET /payments/:id`
- `POST /payments` — body: `invoiceId, amountPaid, paymentMethod, paymentDate`. Server also recomputes `Invoice.paidAmount` and `Invoice.paymentStatus`
- `DELETE /payments/:id` — reverses the effect on the parent invoice's `paidAmount`/`paymentStatus`
- `GET /invoices/:invoiceId/payments` — convenience alias, same as `GET /payments?invoiceId=`
