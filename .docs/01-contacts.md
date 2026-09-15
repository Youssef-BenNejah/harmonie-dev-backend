# Contacts — Personnes, Entreprises, Clients

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, list query params).

---

## Personnes (`/personnes`)

Individual contacts. May optionally belong to an Entreprise, and may be flagged as a client.

**Model fields** (`Person`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| prenom | string | required |
| nom | string | required |
| email | string | required |
| telephone | string | required |
| pays | string | required |
| cin | string | optional (national ID — often empty for foreign contacts) |
| adresse | string | required |
| isClient | boolean | true once converted to a Client |
| entreprise | `{ id, nom } \| null` | link to an Entreprise this person belongs to |
| createdBy | string (tenant id) | server-set |
| created | ISO date | server-set |

**Endpoints**
- `GET /personnes` — list. Query: `page, limit, search (nom/prenom/email/telephone), isClient, entrepriseId, pays, sort`
- `GET /personnes/:id`
- `POST /personnes` — body: `prenom, nom, email, telephone, pays, cin?, adresse, entrepriseId?`
- `PATCH /personnes/:id` — body: any subset of the above
- `DELETE /personnes/:id`
- `POST /personnes/:id/convert-to-client` — body: none. Sets `isClient=true` and creates the linked `Client` record (`type: "Person"`)

---

## Entreprises (`/entreprises`)

Company contacts. May optionally have a main contact Person, and may be flagged as a client.

**Model fields** (`Entreprise`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| nom | string | required |
| email | string | required |
| telephone | string | required |
| pays | string | required |
| siteweb | string | optional |
| rib | string | bank account (RIB/IBAN) |
| fisc | string | tax/fiscal identification number (matricule fiscal) |
| adresse | string | required |
| isClient | boolean | true once converted to a Client |
| mainContact | `{ id, prenom, nom } \| null` | link to a Person |
| createdBy | string (tenant id) | server-set |
| created | ISO date | server-set |

**Endpoints**
- `GET /entreprises` — list. Query: `page, limit, search (nom/email/telephone/fisc), isClient, pays, sort`
- `GET /entreprises/:id`
- `POST /entreprises` — body: `nom, email, telephone, pays, siteweb?, rib?, fisc, adresse, mainContactId?`
- `PATCH /entreprises/:id`
- `DELETE /entreprises/:id`
- `POST /entreprises/:id/convert-to-client` — body: none. Sets `isClient=true` and creates the linked `Client` record (`type: "Company"`)

---

## Clients (`/clients`)

A thin wrapper that points at either a Person or an Entreprise — the actual billable party on invoices. Created via the "convert to client" actions above, or directly.

**Model fields** (`Client`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| type | `"Person" \| "Company"` | required |
| person | `{ id, prenom, nom, email, telephone, cin, adresse } \| null` | populated when `type="Person"` |
| entreprise | `{ id, nom, email, telephone, fisc, adresse } \| null` | populated when `type="Company"` |
| createdBy | string (tenant id) | server-set |
| created | ISO date | server-set |

**Endpoints**
- `GET /clients` — list. Query: `page, limit, search (name/email/phone/fisc/cin), type, sort`
- `GET /clients/:id`
- `POST /clients` — body: `type, personId?` (required if type=Person) `, entrepriseId?` (required if type=Company)
- `DELETE /clients/:id`

*(No direct `PATCH /clients/:id` — the underlying Person/Entreprise is edited instead, and the Client record's denormalized snapshot is refreshed server-side.)*
