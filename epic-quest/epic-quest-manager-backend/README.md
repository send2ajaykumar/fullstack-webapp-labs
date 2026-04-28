# Epic Quest Manager Backend

## Overview

This repository contains the backend API for the Epic Quest Manager assignment. It is implemented as a Spring Boot REST application that manages users, heroes, items, and quests in a small RPG-style domain.

The codebase emphasizes:

- Clear separation of controller, service, repository, DTO, and persistence concerns
- JWT-based authentication with role-based authorization
- Features such as filtering, pagination, caching, and persisted quest history
- Explicit validation and centralized error handling
- API documentation through OpenAPI / Swagger annotations
- Regression-oriented testing for business rules and previously discovered defects
- Safe response serialization that avoids leaking internal or sensitive fields

## Tech Stack

- Java 25
- Spring Boot 4.0.5
- Spring Web MVC
- Spring Data JPA
- Spring Security
- H2 database
- JWT for token handling
- Bucket4j for auth endpoint rate limiting
- Spring Cache with in-memory ConcurrentMap cache
- JUnit 5, Mockito, and MockMvc for tests
- SpringDoc OpenAPI for interactive API docs

## Project Structure

```text
src/main/java/com/ajay/epicquest/
  config/        application and infrastructure configuration
  controller/    REST endpoints and response mapping
  dto/           request and response contracts
  exception/     domain exceptions and global exception handling
  model/         JPA entities and enums
  repository/    Spring Data JPA repositories
  security/      JWT, Spring Security, and rate limiting
  service/       business interfaces and implementations

src/test/java/com/ajay/epicquest/
  integration and service-level regression tests
```

## Domain Summary

### Users

Users authenticate with username and password and are assigned one of two roles:

- `PLAYER`
- `ADMIN`

Admins can create items and quests and may register additional admin users. Public registration defaults to `PLAYER`.

### Heroes

Each hero belongs to a user. A hero:

- has a name, class, and level
- can hold at most three items
- cannot hold the same item twice
- may accept quests if the hero satisfies the rarity requirement

### Items

Items include category, rarity, description, and power value. They are managed by admins and can be filtered and paginated publicly.

### Quests

Quests have a difficulty level, required rarity, and an optional reward item. A hero can accept a quest only when at least one of the hero's items meets or exceeds the quest's required rarity.

Quest acceptance is modeled explicitly through a `QuestAcceptance` entity so acceptance time and many-to-many history are preserved cleanly.

## Key Business Rules

- Public registration creates `PLAYER` accounts by default.
- Only authenticated admins may create `ADMIN` accounts.
- JWT access tokens expire after one hour.
- Only admins can create, update, or delete items.
- Only admins can create quests.
- A hero may have at most three items.
- A hero cannot contain duplicate item associations.
- A hero may accept a quest only if at least one owned item meets the required rarity threshold.
- A hero cannot accept the same quest more than once.
- Hero and quest responses intentionally avoid exposing nested user entities or password hashes.

## Architecture Notes

### Controller Layer

Controllers are responsible for:

- defining API routes
- validating request bodies and parameters
- returning stable DTO contracts where entity graphs would be unsafe
- expressing endpoint-level intent through OpenAPI annotations

Business rules are intentionally not embedded directly in controllers.

### Service Layer

Services contain the core application rules, including:

- admin-only registration semantics
- hero ownership checks
- inventory size and duplicate prevention
- quest rarity qualification
- duplicate quest acceptance protection

### Persistence Layer

JPA entities model the domain, and repositories encapsulate database access. Where necessary, persistence constraints reinforce service rules, such as the unique `(hero_id, item_id)` join-table constraint for hero inventory.

### Response Safety

During review hardening, the API was updated to avoid returning raw entity graphs in places that would expose internal relationships. Hero and quest endpoints now use safe DTOs where nested owner data would otherwise leak `passwordHash`.

## Security Design

### Authentication

- Login returns a bearer token in the auth response.
- JWTs are validated by a custom `JwtFilter` inside the Spring Security chain.
- Tokens include user identity information used by downstream authorization checks.
- Registration supports an optional `role` field, but only an already authenticated admin may create another `ADMIN` user.

### Authorization

Role and ownership checks are enforced through a combination of:

- Spring Security route rules in `SecurityConfig`
- service-level authorization for hero and quest actions

Examples:

- item mutation is admin-only
- quest creation is admin-only
- hero inventory changes are restricted to the hero owner or an admin
- quest acceptance is restricted to the hero owner or an admin

### Rate Limiting

Auth endpoints are protected by Bucket4j-based IP rate limiting to reduce brute-force login or registration attempts. Rate limiting can be disabled by configuration in test or troubleshooting scenarios.

### Error Handling

The application uses a centralized `GlobalExceptionHandler` to:

- return consistent `400`, `401`, `403`, `404`, and `405` style responses
- suppress stack traces and exception leakage from API responses
- map malformed request bodies to cleaner client-visible errors

## API Overview

### Authentication

- `POST /auth/register`
- `POST /auth/login`

Registration behavior:

- unauthenticated callers create `PLAYER` accounts by default
- authenticated admins may send `role=ADMIN` to create additional admin users

### Heroes

- `POST /heroes`
- `GET /heroes/{id}`
- `POST /heroes/{heroId}/inventory`
- `DELETE /heroes/{heroId}/inventory/{itemId}`
- `GET /heroes/{heroId}/quests/accepted`
- `POST /heroes/{heroId}/quests/{questId}/accept`

### Items

- `GET /items`
- `GET /items/{id}`
- `POST /items`
- `PUT /items/{id}`
- `DELETE /items/{id}`

Supported `GET /items` query parameters:

- `category`
- `rarity`
- `page`
- `limit`

Default item ordering is by rarity from `COMMON` to `LEGENDARY`, then by descending `powerValue` within the same rarity tier.

### Quests

- `POST /quests`
- `GET /quests`
- `POST /quests/{questId}/accept`

Supported `GET /quests` query parameters:

- `page`
- `limit`

Two quest-accept route shapes are supported:

- `POST /quests/{questId}/accept` with `{ "heroId": ... }`
- `POST /heroes/{heroId}/quests/{questId}/accept`

The nested route was added to align with the assignment-style URL expectation while reusing the same business logic.

## Running the Application

### Prerequisites

- Java 25 installed and available on the path
- no external database required for local development

### Environment Variables

The application expects:

- `JWT_SECRET`: required JWT signing secret
- `BOOTSTRAP_ADMIN_USERNAME`: optional first-admin username
- `BOOTSTRAP_ADMIN_PASSWORD`: optional first-admin password

If bootstrap admin values are provided and no admin already exists, the application creates the initial admin user on startup.

### Local Configuration

Current default application properties:

- application name: `epic-quest-manager`
- database: `jdbc:h2:file:./data/epicquestdb`
- h2 console enabled at `/h2-console`
- JWT expiration: `3600000` ms

### Start Command

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## API Documentation

When the application is running, interactive Swagger documentation is available at:

```text
/swagger-ui/index.html
```

The controllers are annotated with OpenAPI metadata so a reviewer can inspect the API interactively without reading all controller code first.

## Testing Strategy

### Goals

The test suite is intended to cover both the happy path and the defect-prone areas that commonly break during backend assignments:

- auth contract correctness
- role and ownership enforcement
- inventory constraints
- quest acceptance rules
- item filtering, pagination, and cache invalidation behavior
- admin-only role creation and accepted-quest history retrieval
- input validation and malformed request handling
- security-sensitive serialization behavior
- endpoint regression for assignment-specific route shapes

### Test Types

#### Unit / Service Tests

Service tests validate rule enforcement close to the business layer, including registration logic, duplicate inventory prevention, pagination validation, and service-specific failure scenarios.

#### Integration Tests

Integration tests use Spring Boot, MockMvc, security, and an H2-backed context to verify:

- real route behavior
- JWT-protected flows
- persistence interactions
- feature behavior such as filtering, pagination, admin role creation, and accepted quest listing
- API response shape
- regression scenarios that were discovered and fixed during implementation

### Run Tests

```powershell
.\mvnw.cmd test
```

The test profile uses `src/test/resources/application.properties`, which supplies a test JWT secret and disables auth rate limiting with `rate.limit.enabled=false` so security and controller tests remain stable.

## Postman Collection Testing

A pre-configured Postman collection is included for reviewers who prefer interactive endpoint testing:

### Import the Collection

1. Open Postman
2. Click **Import** → **File**
3. Select `Epic-Quest-Manager.postman_collection.json` from the project root

### Run the Testing Sequence

The collection includes the following requests that cover all major API flows:

| # | Request Name | Description |
|---|---|---|
| 1 | **Add User ADMIN** | Registers an ADMIN account |
| 2 | **Access Token** | Logs in and obtains a JWT bearer token |
| 3 | **Add user PLAYER** | Registers a PLAYER account |
| 4 | **Add Items** | Admin creates an item |
| 5 | **Get Items** | Lists items with optional filters |
| 6 | **Update Items** | Admin updates an existing item |
| 7 | **Delete Items** | Admin deletes an item |
| 8 | **Add Heroes** | Player creates a hero |
| 9 | **Get Heroes** | Retrieves a hero by ID |
| 10 | **Add Hero's inventory** | Adds an item to hero inventory |
| 11 | **Delete Hero's inventory** | Removes an item from hero inventory |
| 12 | **Get Quests** | Lists all quests |
| 13 | **Add Quests** | Admin creates a quest |
| 14 | **Add Hero accept quest** | Hero accepts quest via `POST /quests/{questId}/accept` |
| 15 | **Add Hero accept quest-1** | Hero accepts quest via `POST /heroes/{heroId}/quests/{questId}/accept` |

Each request includes expected response assertions that validate:
- Correct HTTP status codes
- Required response fields
- Data integrity and business rule enforcement
- Safe serialization (no password hashes or internal entities exposed)


## Example Local Flow

### 1. Register a Player

```json
POST /auth/register
{
  "username": "player1",
  "password": "playerPass123"
}
```

### 2. Login

```json
POST /auth/login
{
  "username": "player1",
  "password": "playerPass123"
}
```

### 3. Create a Hero

```json
POST /heroes
{
  "name": "Arthas",
  "heroClass": "fighter",
  "level": 5
}
```

### 4. Create a Quest as Admin

```json
POST /quests
{
  "title": "Dragon Hunt",
  "description": "Slay the ancient dragon in Ember Peak.",
  "difficultyLevel": 10,
  "requiredRarity": "EPIC",
  "rewardItemId": 1
}
```

### 5. Accept a Quest

Body-based route:

```json
POST /quests/2/accept
{
  "heroId": 1
}
```

Nested route:

```text
POST /heroes/1/quests/2/accept
```

## Security and Serialization Hardening Notes

- JWT auth responses use explicit API contracts
- invalid tokens result in clean `401` responses
- auth endpoints are rate limited independently from gameplay endpoints
- malformed bodies no longer produce noisy framework traces
- hero responses expose `owner_user_id` instead of the nested owner entity
- quest responses expose safe nested hero DTOs instead of leaking user credentials
- duplicate hero inventory entries are blocked in both service logic and the database join table

## Tradeoffs and Future Improvements

The implementation is assignment-focused and production-shaped, but a few improvements would be natural next steps in a real system:

- move from file-based H2 to PostgreSQL or another production database
- serve the API over HTTPS with proper TLS certificate management in deployed environments
- externalize more environment-specific configuration
- add audit logging and structured observability
- introduce broader DTO coverage for items if future response-shape isolation becomes necessary
- extend read caching to additional endpoints such as hero lookups if profiling shows value
- add metadata envelopes for paginated responses
- add refresh-token and logout flows if longer-lived client sessions are needed
- add database migrations through Flyway or Liquibase
- tighten H2 console exposure for non-local environments
- add CI automation and code-quality gates

## Known Intentional Simplifications

- The project uses a simple two-role model (`PLAYER`, `ADMIN`) rather than a more granular permission system.
- The domain is intentionally compact to keep the assignment focused on API quality and business-rule enforcement.
- H2 is used to minimize reviewer setup cost.