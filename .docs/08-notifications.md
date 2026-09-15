# Notifications

See [00-overview.md](00-overview.md) for conventions (prefix, auth scoping, list query params).

In-app notification feed for the current tenant (renewal requests the tenant sent are visible to SuperAdmin; a tenant sees notifications relevant to them, e.g. their own renewal approval/rejection). See [09-superadmin.md](09-superadmin.md) for the renewal and join-request flows that populate this feed.

---

## Notifications (`/notifications`)

**Model fields** (`AppNotification`)
| Field | Type | Notes |
|---|---|---|
| id | string | server-generated |
| type | `"renewal_request" \| "join_request"` | extensible enum |
| tenantAdminId | string | subject of the notification (for `join_request`, this holds the `JoinRequest.id` instead) |
| title | string | server-set |
| message | string | server-set |
| createdAt | ISO datetime | server-set |
| read | boolean | server default false |

**Endpoints**
- `GET /notifications` — list for the current session (tenant sees their own; SuperAdmin sees platform-wide). Query: `page, limit, unreadOnly, type`
- `PATCH /notifications/:id/read` — body: none
- `PATCH /notifications/read-all` — body: none
