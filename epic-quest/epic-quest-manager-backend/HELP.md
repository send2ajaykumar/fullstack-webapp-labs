# Epic Quest Manager Backend

## Overview

This project is a Spring Boot REST API for managing users, heroes, items, and quests in a small RPG-style domain.

The codebase was structured as a production-style backend assignment with the following goals:

- JWT-based authentication and role-based authorization
- Clear separation between controllers, services, repositories, DTOs, and persistence models
- Input validation and centralized exception handling
- Regression coverage through unit and integration tests
- Reviewer-friendly API documentation via Swagger / OpenAPI annotations

## Tech Stack

- Java 25
- Spring Boot 4.0.5
- Spring Web MVC
- Spring Data JPA
- Spring Security
- H2 database
- JJWT for token generation and validation
- Bucket4j for auth endpoint rate limiting
- JUnit 5, Mockito, and MockMvc for testing

## Project Structure

- `controller`: API endpoints and response mapping
- `service`: business logic and authorization checks
- `repository`: JPA persistence layer
- `dto`: request and response contracts
- `model`: JPA entities and enums
- `security`: JWT auth, filter chain, and rate limiting filter
- `exception`: custom exceptions and global exception handler
- `config`: Swagger, cache, bootstrap admin, and rate-limit configuration

## Key Business Rules

- Public registration creates `PLAYER` users by default.
- Only authenticated admins can create `ADMIN` users.
- JWT access tokens expire after one hour.
- Only admins can create, update, or delete items and create quests.
- A hero may carry at most three items.
- The same item cannot be added to the same hero more than once.
- A hero may accept a quest only if at least one owned item meets the required rarity.
- A hero cannot accept the same quest more than once.

## API Notes

### Authentication

- `POST /auth/register`: register a new user
- `POST /auth/login`: obtain a JWT access token

### Heroes

- `POST /heroes`: create a hero for the authenticated user
- `GET /heroes/{id}`: retrieve a hero
- `POST /heroes/{heroId}/inventory`: add an item to hero inventory
- `DELETE /heroes/{heroId}/inventory/{itemId}`: remove an item from hero inventory
- `GET /heroes/{heroId}/quests/accepted`: list accepted quests for a hero
- `POST /heroes/{heroId}/quests/{questId}/accept`: accept a quest using the nested route required by the assignment

### Items

- `GET /items`: list items with optional filtering and pagination
- `GET /items/{id}`: retrieve a single item
- `POST /items`: create an item (admin only)
- `PUT /items/{id}`: update an item (admin only)
- `DELETE /items/{id}`: delete an item (admin only)

### Quests

- `POST /quests`: create a quest (admin only)
- `GET /quests`: list quests
- `POST /quests/{questId}/accept`: accept a quest by request body `heroId`

## Security Notes

- Authentication is stateless and handled by a custom JWT filter in the Spring Security filter chain.
- Public endpoints are limited to login, registration, and read-only resource retrieval where allowed by the assignment.
- API responses were hardened to avoid leaking nested `owner` entities or `passwordHash` values in hero and quest responses.
- `/error` is explicitly permitted so malformed request bodies can return correct framework-level `400` responses instead of being converted into auth failures.

## Running Locally

### Prerequisites

- Java 25
- Maven wrapper included in the repository

### Environment Variables

- `JWT_SECRET`: required signing secret for JWT generation
- `BOOTSTRAP_ADMIN_USERNAME`: optional username for first admin bootstrap
- `BOOTSTRAP_ADMIN_PASSWORD`: optional password for first admin bootstrap

### Start the Application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

### Default Local Configuration

- App name: `epic-quest-manager`
- Database: `jdbc:h2:file:./data/epicquestdb`
- H2 console: `/h2-console`
- JWT expiration: `3600000` ms (1 hour)

## Testing

Run the full test suite:

```powershell
.\mvnw.cmd test
```

The test suite includes:

- service-level unit tests for core business rules
- integration tests for auth, hero, item, and quest flows
- regression tests for security-sensitive response serialization
- regression tests for duplicate hero inventory prevention
- regression tests for nested hero quest accept routing

## API Documentation

Swagger UI is available when the application is running:

- `/swagger-ui/index.html`

OpenAPI annotations on controllers describe request and response expectations for reviewer exploration.

## Reviewer Notes

Areas that received particular attention during implementation and refinement:

- response contract alignment for auth and register flows
- role-sensitive registration behavior
- bootstrap admin creation for first-run environments
- 401 versus 403 behavior in Spring Security
- safe DTO-based serialization to avoid leaking user password hashes
- clean error responses without stack traces or internal exception dumps
- requirement-aligned nested route for quest acceptance

This file is intended as a quick project guide. A full `README.md` with architecture rationale, endpoint examples, and reviewer walkthrough can be added separately for final submission.

