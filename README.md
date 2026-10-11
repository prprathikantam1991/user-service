# User Service

User Service is the canonical internal identity directory for the application ecosystem. It assigns a stable internal UUID to each person, stores their application profile and roles, and exposes protected APIs only to trusted backend services.

## Internal API authentication

User management endpoints under `/api/**` are internal service-to-service APIs. They require the `X-Internal-Api-Key` request header to match `USER_SERVICE_API_KEY`. Auth Service must be configured with the same environment value. The key is never committed to this repository; missing configuration fails closed for `/api/**` requests.

This service does not authenticate browser users directly. Auth Service authenticates the person with Google, then calls User Service to find or create the durable internal user record. Finance Tracker uses that stable UUID as `owner_user_id` for finance records, but never reads this database directly.

Centralized User Management Service for handling user creation and role management.

## Overview

The User Service provides a single source of truth for user data and role management across the microservices architecture. It handles:

- User creation with default role assignment
- User information updates
- Role assignment and removal
- User and role queries
- Spring Security authority generation

## Architecture

```
Auth Service → User Service (creates user + assigns default role)
EMS Service → User Service (queries users, manages roles)
Finance Tracker → uses the stable User Service UUID through Auth Service's verified handoff
```

For the detailed three-service contract, see [docs/FINANCE_TRACKER_IDENTITY_CONTRACT.md](docs/FINANCE_TRACKER_IDENTITY_CONTRACT.md).

## Port

- **8082** - User Service REST API

## Database

- Dedicated PostgreSQL database (`user_service_db`)
- User Service owns only its identity and role tables: `users`, `roles`, and `user_roles`.
- Other applications integrate through the User Service API; they do not read this database directly.

## API Endpoints

### User Management

- `POST /api/users` - Create user with default role
- `POST /api/users/create-or-update` - Create or update user (idempotent)
- `PUT /api/users/{email}` - Update user info
- `GET /api/users/{email}` - Get user by email with roles
- `GET /api/users/google/{googleId}` - Get user by Google ID with roles

### Role Management

- `POST /api/users/{email}/roles` - Assign role to user
- `DELETE /api/users/{email}/roles/{roleName}` - Remove role from user
- `GET /api/users/{email}/roles` - Get user roles
- `GET /api/users/{email}/authorities` - Get Spring Security authorities
- `GET /api/users/google/{googleId}/authorities` - Get authorities by Google ID

## Default Roles

The service automatically initializes the following roles:

- **ADMIN** - Administrator with full system access
- **HR** - Human Resources with employee management access
- **MANAGER** - Manager with department management access
- **USER** - Regular user with basic access (default role for new users)

## Configuration

### application.properties

```properties
server.port=8082
spring.application.name=user-service

# Database Configuration (owned by User Service)
spring.datasource.url=jdbc:postgresql://localhost:5432/user_service_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# CORS Configuration
cors.allowed-origins=http://localhost:4200,http://localhost:8080,http://localhost:8081
```

## Dependencies

- Spring Boot Web
- Spring Data JPA
- PostgreSQL Driver
- Lombok
- SpringDoc OpenAPI (API documentation)
- Spring Boot Validation
- Spring Boot Cache (distributed caching)
- Spring Data Redis (Redis integration)

## Running the Service

```bash
mvn spring-boot:run
```

The service will be available at `http://localhost:8082`

## API Documentation

Once the service is running, API documentation is available at:
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8082/v3/api-docs`

## Integration

### Auth Service Integration

Auth Service calls User Service to create/update users after OAuth2 authentication:

```java
POST /api/users/create-or-update
{
  "email": "user@example.com",
  "googleId": "google-user-id",
  "name": "User Name",
  "picture": "https://..."
}
```

The operation is idempotent and safe for a first-login retry. A caller must use the internal API key; it is not a public registration endpoint.

### EMS Service Integration

EMS Service calls User Service to get user authorities for JWT authentication:

```java
GET /api/users/{email}/authorities
// Returns: { "authorities": ["ROLE_USER", "ROLE_HR"] }
```

## Error Handling

The service includes global exception handling for:
- User not found (404)
- Role not found (404)
- Duplicate user (409)
- Validation errors (400)
- Internal server errors (500)

## Distributed Caching

The service uses Redis for distributed caching to improve performance and reduce database load. All instances of the user-service share the same Redis cache.

### Cache Configuration

**Cache TTL (Time To Live):**
- Default: 1 hour (3,600,000 milliseconds)
- Configurable via `cache.redis.time-to-live` property
- Cache entries automatically expire after TTL

**Cached Operations:**
- `getUserByEmail()` - Cached with key `"email:" + email`
- `getUserByGoogleId()` - Cached with key `"googleId:" + googleId`

**Cache Invalidation:**
- User creation/updates automatically invalidate related cache entries
- Role assignments/removals invalidate user cache entries
- Ensures data consistency across all service instances

### Redis Server Configuration

**Important:** Redis memory limits must be configured at the Redis server level, not in the Spring application.

#### Setting Redis Memory Limits

Configure Redis server with memory limits and eviction policies:

**Option 1: Redis Configuration File (`redis.conf`)**
```conf
# Set maximum memory (adjust based on available RAM)
maxmemory 256mb

# Eviction policy when memory limit is reached
maxmemory-policy allkeys-lru
```

**Option 2: Redis CLI**
```bash
redis-cli CONFIG SET maxmemory 256mb
redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

**Option 3: Docker/Container**
```yaml
# docker-compose.yml
redis:
  image: redis:7-alpine
  command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru
```

#### Eviction Policy Options

- **`allkeys-lru`** (Recommended for caching) - Evicts least recently used keys when memory limit is reached
- **`allkeys-lfu`** - Evicts least frequently used keys
- **`volatile-lru`** - Evicts LRU keys that have TTL set only
- **`volatile-lfu`** - Evicts LFU keys that have TTL set only
- **`noeviction`** - Returns errors when memory limit is reached (not recommended for caching)

#### Memory Limit Recommendations

- **Development:** 128MB - 256MB
- **Production:** 512MB - 2GB (depending on user count and available RAM)
- Monitor Redis memory usage: `redis-cli INFO memory`

#### Cache Behavior

- **No Maximum Entry Count:** Unlike in-memory caches (Caffeine), Redis doesn't have a built-in maximum entry count in Spring Cache
- **Memory-Based Eviction:** Redis evicts entries based on memory usage and eviction policy
- **TTL-Based Expiration:** Entries also expire based on configured TTL (1 hour default)
- **Shared Cache:** All service instances share the same Redis cache, providing consistency

### Application Configuration

Cache settings in `application.yaml`:

```yaml
cache:
  redis:
    time-to-live: 3600000  # 1 hour in milliseconds
    cache-null-values: true  # Cache null values to avoid cache penetration
```

Redis connection settings:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
```

### Monitoring

Monitor Redis cache performance:
- Redis memory usage: `redis-cli INFO memory`
- Cache hit/miss rates: Use Spring Actuator metrics (if enabled)
- Redis keys: `redis-cli KEYS "users:*"` (use SCAN in production)
