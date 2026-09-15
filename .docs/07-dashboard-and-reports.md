# Tableau de bord & rapports

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, list query params).

Pure read/aggregation endpoints — no stored resources of their own, computed from Invoices, Payments and Expenses ([04-invoicing.md](04-invoicing.md), [05-payments.md](05-payments.md), [03-expenses.md](03-expenses.md)).

---

## Tableau de bord (`/dashboard`)

- `GET /dashboard/summary` — returns: `{ revenue: number, unpaidInvoicesCount: number, clientsCount: number, monthlyExpenses: number }` plus the four KPI sparkline series (`revenueTrend[], unpaidTrend[], clientsTrend[], expensesTrend[]`)
- `GET /dashboard/revenue-series` — query: `months? (default 12)`. Returns `[{ mois/month, revenus/revenue, depenses/expenses }]`
- `GET /dashboard/invoice-status-distribution` — returns `[{ name: paymentStatus, value: percentage }]`
- `GET /dashboard/recent-invoices` — query: `limit? (default 5)`
- `GET /dashboard/recent-activity` — query: `limit? (default 5)`. Returns an activity feed (`{ id, titre, detail, temps/createdAt }`) generated from invoice/payment/client/expense mutations

---

## Rapports (`/reports`)

- `GET /reports/overview` — query: `dateFrom, dateTo`. Returns `{ totalRevenue, totalExpenses }` over the range
- `GET /reports/top-clients` — query: `dateFrom, dateTo, limit? (default 5)`. Returns `[{ clientId, nom, total }]`
- `GET /reports/top-services` — query: `dateFrom, dateTo, limit? (default 5)`. Returns `[{ serviceId, name, price/totalSold }]`
- `GET /reports/export` — query: same filters as overview. Returns a generated PDF report
