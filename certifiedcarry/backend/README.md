# CertifiedCarry Backend

## Overview

This backend is the Spring Boot API for CertifiedCarry, an esports talent discovery platform. Its main design goal is to separate:

- HTTP handling
- business rules
- persistence
- cross-cutting infrastructure such as authentication, auditing, and rate limiting

the backend is organized to make controller responsibilities, service responsibilities, and infrastructure responsibilities distinct.

## Main System Responsibilities

The backend supports these core behaviors:

- Firebase-backed authentication context
- role-aware authorization for `PLAYER`, `RECRUITER`, and `ADMIN`
- backend user creation after Firebase signup
- player profile creation, update, and verification lifecycle
- recruiter pending-approval queue
- rank verification submission and moderation
- leaderboard entry management
- authenticated chat threads and chat messages
- presigned media upload generation
- audit logging for sensitive mutations
- request rate limiting on selected endpoints
- notification workflows such as account approval, unread chat reminders, and rank expiry reminders

## Architectural Structure

```text
src/main/java/certifiedcarry_api/
  auth/          authentication-facing endpoints
  audit/         audit event persistence and recording
  chat/          chat controllers, API models, and services
  config/        security, Firebase, CORS, audit, and rate limiting
  leaderboard/   leaderboard controllers, DTOs, and service logic
  notification/  email and notification orchestration
  profile/       player profile controllers and service collaborators
  queue/         recruiter and rank moderation queues
  shared/        validation, parsing, actor resolution, and error handling
  storage/       presigned upload support
  user/          user DTOs, entities, factories, repositories, and services
```

### Architectural Roles

- **Controllers** handle request/response concerns.
- **Services** contain business rules.
- **Repositories and JDBC gateways** handle persistence.
- **Shared utilities** centralize parsing, validation, and error shaping.
- **Configuration and filters** handle security and other cross-cutting request concerns.

This separation is important because it makes it easier to justify the codebase in terms of single responsibility and maintainability.

## Technology Stack

- Java 21
- Spring Boot 3.5
- Spring Security
- Spring Data JPA
- Hibernate
- JdbcTemplate
- PostgreSQL
- Flyway
- Firebase Admin SDK
- AWS S3 compatible object storage

## Setup and Execution

### Requirements

- Java 21
- PostgreSQL
- Maven wrapper included in the repo

### Environment Configuration

Create `.env` from `.env.example`, or provide environment variables directly.

Typical values:

```bash
SERVER_PORT=8000

DB_URL=jdbc:postgresql://localhost:5432/certifiedcarry_backend
DB_USERNAME=postgres
DB_PASSWORD=postgres

FIREBASE_ENABLED=false
FIREBASE_PROJECT_ID=
FIREBASE_SERVICE_ACCOUNT_PATH=

OBJECT_STORAGE_ENABLED=false
OBJECT_STORAGE_ENDPOINT=
OBJECT_STORAGE_BUCKET=
OBJECT_STORAGE_ACCESS_KEY=
OBJECT_STORAGE_SECRET_KEY=

LEGAL_TERMS_ACTIVE_VERSION=cc-terms-2026-04-04
LEGAL_PRIVACY_ACTIVE_VERSION=cc-privacy-2026-04-04
```

### Run in Development

```bash
./mvnw spring-boot:run
```

Default base URL:

```text
http://localhost:8000/api
```

### Build

```bash
./mvnw clean package
```

### Test

```bash
./mvnw test
```

### Compile Without Tests

```bash
./mvnw -DskipTests compile
```

## Important Technical Decisions

### 1. Security Filter Pipeline

- Files: `SecurityConfig.java`, `FirebaseTokenFilter.java`, `AuditLoggingFilter.java`, `RequestRateLimitFilter.java`
- Authentication, audit logging, and rate limiting are handled before requests reach controllers.

Why this matters:

- controllers stay focused on endpoint logic
- cross-cutting concerns are centralized
- route-level policy is easier to review

### 2. Service-Layer Business Logic

- Files: `UserService.java`, `ChatService.java`, `PendingQueueService.java`, `PlayerProfileService.java`, `LeaderboardService.java`
- Controllers delegate to services instead of containing application rules directly.

Why this matters:

- HTTP concerns and business rules stay separate
- logic becomes easier to test and refactor
- controller code remains thin and readable

### 3. Mixed Persistence Strategy

- Files: `UserRepository.java` and several JDBC-backed services/gateways
- The backend uses Spring Data JPA where entity persistence is straightforward and JdbcTemplate where query-heavy logic needs tighter SQL control.

Why this matters:

- the design stays practical
- high-query domains are not forced into awkward ORM-only solutions
- repository responsibility still stays separate from controller logic

### 4. Typed API Boundaries

- Files: `user/api/*`, `chat/api/*`, `queue/api/*`, `leaderboard/api/*`, `profile/api/*`
- Request and response shapes are defined explicitly at the API boundary.

Why this matters:

- API contracts are clearer
- controller logic is easier to follow
- payload handling is less error-prone than ad hoc maps everywhere

## Implemented Design Patterns

### Creational Pattern: Factory

- **Where:** `src/main/java/certifiedcarry_api/user/factory/UserCreationFactory.java`, `PlayerUserCreationFactory.java`, `RecruiterUserCreationFactory.java`, `UserFactorySelector.java`
- **What it is:** role-specific user creation is delegated to different factory implementations selected at runtime.
- **Why it is useful here:** player and recruiter accounts follow different creation rules, so the backend avoids placing all construction logic inside one large registration method.

### Structural Pattern: Facade

- **Where:** `ChatService.java`, `PendingQueueService.java`, `UserService.java`, `PlayerProfileService.java`
- **What it is:** each facade exposes a simplified public service API while delegating detailed work to narrower collaborators.
- **Why it is useful here:** controllers call one clear entry point instead of coordinating multiple lower-level classes directly.

### Behavioral Pattern: Chain of Responsibility

- **Where:** `SecurityConfig.java` with `FirebaseTokenFilter.java`, `AuditLoggingFilter.java`, and `RequestRateLimitFilter.java`
- **What it is:** a request passes through a sequence of handlers, each responsible for one concern.
- **Why it is useful here:** authentication, auditing, and throttling are cleaner as ordered request handlers than as duplicated controller logic.

### `src/main/java/certifiedcarry_api/config/SecurityConfig.java`

- central authorization policy
- defines which endpoints are public and which roles may access protected ones
- shows how request filters are ordered

### `src/main/java/certifiedcarry_api/config/firebase/FirebaseTokenFilter.java`

- verifies Firebase bearer tokens
- resolves linked backend user information
- attaches the current actor context to the request

### `src/main/java/certifiedcarry_api/user/service/UserRegistrationService.java`

- shows user creation flow
- uses the Factory pattern for role-specific account construction

### `src/main/java/certifiedcarry_api/notification/service/NotificationOrchestratorService.java`

- coordinates cross-cutting notification workflows
- good example of orchestration and façade-style design

### `src/main/java/certifiedcarry_api/shared/ApiExceptionHandler.java`

- centralizes API error shaping
- demonstrates consistent exception handling across controllers

## Database and Migrations

- Flyway migrations live in `src/main/resources/db/migration`
- the schema includes users, player profiles, recruiter and rank queues, leaderboard entries, chat data, audit events, notification state, and deferred account deletion support

## Notes

- authentication and authorization are centralized
- controllers are thin compared with services
- service responsibilities are separated from persistence concerns
- typed API models are used across major domains
- there is a clear Factory pattern, a clear Facade pattern, and a clear Chain of Responsibility pattern in the codebase

## Additional Notes

- Firebase, email, and object storage integrations are environment-controlled and can remain disabled locally.
- The backend is designed to run safely with the frontend mounted against `/api`.
