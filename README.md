# User Service

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
```

## Port

- **8082** - User Service REST API

## Database

- Shared PostgreSQL database (`emsdb`)
- Same database used by Auth Service and EMS Service

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

# Database Configuration (shared with Auth and EMS services)
spring.datasource.url=jdbc:postgresql://localhost:5432/emsdb
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

