# User Service Database Ownership

## Goal

User Service is the system of record for application users and roles. It owns its PostgreSQL database and exposes user information through authenticated service APIs instead of allowing other services to query its tables.

## Local database

The local service connects to `user_service_db` on the existing PostgreSQL Docker container. Its owned tables are:

- `users`
- `roles`
- `user_roles`

The separate EMS database continues to own its unrelated domain tables, such as `employees` and `departments`.

## One-time migration from `emsdb`

Run the following from the User Service repository:

```powershell
.\scripts\migrate-emsdb-to-user-service-db.ps1
```

The script performs these safeguards:

1. Uses an existing empty target database, but refuses to overwrite one that already contains tables.
2. Creates a local, Git-ignored backup containing only User Service tables.
3. Copies only `users`, `roles`, and `user_roles`.
4. Preserves primary keys, so existing downstream references—for example Finance Tracker's `owner_user_id`—continue to point to the same user.
5. Verifies record counts after restore.

After a successful migration, restart User Service. Auth Service and Finance Tracker continue to use User Service's stable internal user ID through APIs; neither accesses this database directly.
