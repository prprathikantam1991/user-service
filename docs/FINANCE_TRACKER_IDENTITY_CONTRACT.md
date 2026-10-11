# User Service Identity Contract for Finance Tracker

## Goal

User Service provides the durable internal identity that allows Finance Tracker to separate every person's accounts, transactions, imports, Assistant conversations, and future Plaid connections.

## Stable identity boundary

Google's subject identifies a person to Google. It is not the Finance database key. After Google sign-in, Auth Service calls User Service's idempotent `create-or-update` endpoint. User Service returns or preserves an immutable internal UUID.

That UUID is the only identity value Finance Tracker persists as `owner_user_id`.

```text
Google subject → Auth Service → User Service UUID → Finance owner_user_id
```

This avoids coupling finance ownership to a mutable email address or to a Google token. User Service remains the source of truth for the UUID and profile; Finance Tracker does not access User Service tables directly.

## Caller security

All `/api/**` routes are backend-only. A trusted caller supplies:

```text
X-Internal-Api-Key: <USER_SERVICE_API_KEY>
```

If `USER_SERVICE_API_KEY` is missing, User Service fails closed. The browser must never receive this key or call the internal API directly.

## Provisioning behavior

1. Google completes sign-in with Auth Service.
2. Auth Service sends the verified profile to `POST /api/users/create-or-update`.
3. User Service finds the existing Google-subject mapping or safely creates it with the default role.
4. Auth Service receives the stable internal UUID.
5. Auth Service passes the UUID to Finance Tracker only through the protected one-time handoff exchange.

The create-or-update operation makes retries safe when a network failure occurs during first login. It does not grant a browser the ability to choose an internal UUID or role.

## Data ownership

User Service owns `users`, `roles`, and `user_roles` in the `user_service_db` database. Finance Tracker owns finance data in its separate database. The services communicate through APIs, preserving independent schema evolution and limiting direct database access.

See [DATABASE_OWNERSHIP.md](DATABASE_OWNERSHIP.md) for the database migration and ownership details.

## Production evolution

The internal API-key boundary is suitable for the local three-service environment. A production deployment should add network isolation, secret rotation, service identity (mTLS or signed workload assertions), least-privilege API scopes, structured audit events, and service availability/error handling policies.
