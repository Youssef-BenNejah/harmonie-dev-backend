# Harmonie-dev — Backend API Audit

Full inventory of every backend endpoint implied by the current frontend (`new/front`), derived directly from the mock data models (`src/lib/mock-data.ts`), the client-side store (`src/lib/store.ts`), and every screen/form in `src/routes/*`. Field names match the frontend types exactly — those types were themselves built to mirror the original backend models (`Person.adresse`, `Entreprise.fisc`, `Tax.taxvalue`, etc.), so this doc should translate 1:1 into real schemas/controllers.

**Auth endpoints are intentionally excluded** (login, register, forgot/reset password, OTP, session/token issuance) per request — those live in a separate auth spec.

## Conventions used throughout this doc set

- All routes are prefixed `/api/v1` (adjust to taste).
- All routes except the public ones (marked 🌐) require an authenticated tenant session and are implicitly scoped to that tenant (`createdBy` / tenant id) — i.e. a tenant only ever sees its own Personnes, Entreprises, Factures, etc.
- SuperAdmin-only routes are marked 🛡️ and require a platform-admin session, scoped across *all* tenants.
- `id` fields are opaque string identifiers (Mongo ObjectId or similar).
- Standard list endpoints support: `?page=&limit=&search=&sort=` plus the resource-specific filters noted per section.
- `created` / `createdAt` fields are server-set on create and never client-writable. `createdBy` is server-set from the authenticated session.
- Money fields are plain numbers (no currency baked in unless noted); currency is a separate relation.

## Document map

| File | Covers |
|---|---|
| [01-contacts.md](01-contacts.md) | Personnes, Entreprises, Clients |
| [02-catalog.md](02-catalog.md) | Catégories de services, Services, Devises, Taxes |
| [03-expenses.md](03-expenses.md) | Catégories de dépenses, Dépenses |
| [04-invoicing.md](04-invoicing.md) | Factures / Devis / Bons de livraison (+ line items) |
| [05-payments.md](05-payments.md) | Paiements |
| [06-company-and-profile.md](06-company-and-profile.md) | Ma entreprise (paramètres), Mon profil |
| [07-dashboard-and-reports.md](07-dashboard-and-reports.md) | Tableau de bord, Rapports |
| [08-notifications.md](08-notifications.md) | Notifications in-app |
| [09-superadmin.md](09-superadmin.md) | SuperAdmin — Tenants, renouvellement, demandes d'adhésion |
| [10-plans.md](10-plans.md) | Plans d'abonnement (référence publique) |

## Summary — resource → endpoint count

| Resource | Endpoints | Doc |
|---|---|---|
| Personnes | 6 | 01-contacts.md |
| Entreprises | 6 | 01-contacts.md |
| Clients | 4 | 01-contacts.md |
| Catégories de services | 5 | 02-catalog.md |
| Services | 5 | 02-catalog.md |
| Devises | 5 | 02-catalog.md |
| Taxes | 6 | 02-catalog.md |
| Catégories de dépenses | 5 | 03-expenses.md |
| Dépenses | 5 | 03-expenses.md |
| Factures (invoices) | 10 | 04-invoicing.md |
| Paiements | 5 | 05-payments.md |
| Ma entreprise | 3 | 06-company-and-profile.md |
| Mon profil | 3 | 06-company-and-profile.md |
| Dashboard & rapports | 9 | 07-dashboard-and-reports.md |
| Notifications | 3 | 08-notifications.md |
| SuperAdmin — Tenants | 6 | 09-superadmin.md |
| Renewal workflow | 3 | 09-superadmin.md |
| Join requests | 6 | 09-superadmin.md |
| Plans | 2 | 10-plans.md |
| **Total** | **97** | |
