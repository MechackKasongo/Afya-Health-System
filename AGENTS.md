# AGENTS.md - Afya Health System

Guide for AI agents working on this Spring Boot healthcare system.

## Project Overview
- **Framework**: Spring Boot 3.3.5 with Java 21
- **Architecture**: Service-oriented (SOA) with domain modules
- **Port**: 8090 (configured in `application.properties`)
- **Web UI**: React + TypeScript + Vite in `frontend/` (`npm run dev` on port 5173, proxies `/api` to the backend in dev; set `VITE_API_BASE_URL` for production builds)
- **Database**: **default profile `dev`** → H2 in-memory (`application-dev.properties`) ; profile **`oracle`** → Oracle + Flyway (`application-oracle.properties`). Tests use H2 (`application-test.properties`). See **`docs/FLYWAY_ET_PROFILS.md`**.
- **Package**: `com.afya.afya_health_system` (note: underscores, not hyphens)

## Module Structure & Boundaries

Project organized under `src/main/java/com/afya/afya_health_system/soa/`:
- **patient** - Patient management (CRUD, search, administrative summary)
- **identity** - Authentication/authorization with JWT tokens
- **consultation** - Consultation/event management
- **admission** - Patient admission workflows
- **medicalrecord** - Medical records storage/retrieval
- **reporting** - Report generation from health data
- **urgence** - Emergency/urgent care management
- **common** - Shared exception handling, security config

**Key Cross-Module Point**: `common/config/GlobalExceptionHandler.java` maps **`DomainException`** (and a few infrastructure types) to HTTP. Subtypes such as `NotFoundException` / `ConflictException` live in **`common/exception/`** — register new domain errors there or as `DomainException` subclasses handled by the single `handleDomain` path.

## Critical Patterns & Conventions

### 1. Layered Architecture Within Each Module
```
controller/ → service/ → repository/ → model/
↓ (DTOs in separate folder)
dto/
```

Example: Patient module has `PatientController` → `PatientService` → `PatientRepository` → `Patient` (JPA entity)

### 2. DTOs are Java Records (Not Classes)
- **Location**: `{module}/dto/` folder
- **Pattern**: Use records with validation annotations from `jakarta.validation`
- **Example**: `PatientCreateRequest` is a record with `@NotBlank`, `@Size`, `@Email` constraints
- **Why**: Records enforce immutability and reduce boilerplate; validation happens automatically in controllers via `@Valid` annotation

### 3. Constructor Injection (No @Autowired)
All services use constructor injection:
```java
private final PatientService patientService;

public PatientController(PatientService patientService) {
    this.patientService = patientService;
}
```
This is testable and explicit. Use this pattern for all new services and controllers.

### 4. Exception Handling
- **Domain exceptions**: Prefer `common/exception/` (`NotFoundException`, `ConflictException`, `BadRequestException`, `DomainException`, …)
- **Throw pattern**: Services throw domain exceptions; `GlobalExceptionHandler` converts to standardized HTTP responses
- **Response format** (from GlobalExceptionHandler):
  ```json
  {
    "status": 404,
    "error": "Not Found",
    "message": "Patient introuvable: 123",
    "timestamp": "2026-05-06T10:30:00Z"
  }
  ```
- **Important**: ConflictException used for data conflicts; NotFoundException for missing resources

### 5. JPA Patterns
- **Entities**: Use traditional getters/setters (no Lombok)
- **Queries**: Define custom methods in repositories (e.g., `findByFirstNameContainingIgnoreCase`)
- **ID Strategy**: `@GeneratedValue(strategy = GenerationType.IDENTITY)` for auto-increment

### 6. REST Endpoint Versioning
All endpoints use `/api/v1/` prefix:
```java
@RequestMapping("/api/v1/patients")  // PatientController
@RequestMapping("/api/v1/auth")       // AuthController
```
Future versions use `/api/v2/`, etc.

### 7. Pagination with Safety Defaults
```java
int safePage = page == null || page < 0 ? 0 : page;
int safeSize = size == null || size <= 0 || size > 100 ? 20 : size;
PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("id").descending());
```
This pattern is in PatientService - replicate when adding pagination to other services.

### 8. French Error Messages
Communication strings are in French (e.g., "Patient introuvable", "Identifiants invalides"). Maintain this convention for user-facing messages.

## Authentication & Security

### JWT Token Flow
1. **Login** (`POST /api/v1/auth/login`):
   - User provides username/password
   - `AuthService` validates against persisted **`AppUser`** rows (plus bootstrap account from `BootstrapAccountProvisioner` when enabled)
   - Returns access + refresh tokens and embedded **`me`** (same payload as `GET /auth/me`, including `hospitalServiceIds` / `hospitalServiceNames`)

2. **Token Refresh** (`POST /api/v1/auth/refresh`):
   - Client sends refresh token
   - Validates persisted refresh row, rotates refresh, returns new pair + **`me`**

3. **Auth Endpoint** (`GET /api/v1/auth/me`):
   - Requires valid access token
   - Returns authenticated user info (authoritative after assignment changes)

4. **Logout** (`POST /api/v1/auth/logout`):
   - Revokes refresh token(s) and registers the current access token **`jti`** in **`revoked_access_jti`** so `JwtAuthenticationFilter` rejects it until expiry (server-side deny list)

### JWT Implementation (JwtService)
- **Secrets**: Configured via environment variables (`JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`)
- **Algorithm**: HS512 (HMAC with SHA-512)
- **Secret Format**: Minimum 64 characters (checked in properties: `at-least-64-characters`)
- **Claims**: Access tokens include `roles` and `fullName`; refresh tokens include `type: "refresh"`

### Bootstrap User (Temporary)
Initial admin is provisioned by **`BootstrapAccountProvisioner`** from `app.bootstrap.*` when no row exists for that username (`app.bootstrap.auto-provision`, default true). Replace or disable in production once users are seeded via DB/Flyway.

### Security Config
- **CSRF**: Disabled (REST API, token-based auth)
- **Public Endpoints**: `/api/v1/auth/**` and `/actuator/health` require no auth
- **Protected Endpoints**: Everything else requires authentication
- **Auth Type**: JWT Bearer (`JwtAuthenticationFilter`); access tokens carry `jti` for logout revocation

### Hospital scope (admissions & urgences)
- **`UserHospitalScopeService`** centralizes who may see which **service names** (from `hospital_services`) for admissions and whether the **Urgences** module applies (`HospitalCatalogConstants.URGENCES_SERVICE_NAME`).
- Rules and ops checklist: **`docs/PERIMETRE_HOSPITALIER.md`**.
- **Audit logs**: refused urgences access is logged at **WARN** (username, operation code, numeric `urgenceId` only — no clinical content); **logout** at **INFO** (username, revocation strategy).

## Build, Test & Development Commands

### Maven Wrapper
```bash
./mvnw clean package         # Full build with tests
./mvnw spring-boot:run       # Run locally (default profile dev = H2)
# Oracle: SPRING_PROFILES_ACTIVE=oracle ./mvnw spring-boot:run
./mvnw test                  # Run tests only
./mvnw clean                 # Clean build artifacts
```

### Development
- **Spring Boot DevTools** enabled: auto-reload on classpath changes
- **Show SQL**: `spring.jpa.show-sql=true` in properties (logs all JPA queries)
- **Database**: H2 in-memory with PostgreSQL compatibility mode
- **Actuator**: `/actuator/health` and `/actuator/info` accessible without auth

### Testing
- Framework: JUnit 5 (via `spring-boot-starter-test`)
- Integration tests: `src/test/java/.../integration/*` with `@SpringBootTest` + `@ActiveProfiles("test")` (H2, Flyway off)
- Run: `./mvnw test`

## Configuration & Environment

**File**: `src/main/resources/application.properties` (common) + profile files `application-dev.properties`, `application-oracle.properties`, `application-test.properties` (tests only).

**Profiles**: default `dev` = H2 local. Oracle + Flyway: set `SPRING_PROFILES_ACTIVE=oracle` and `ORACLE_URL` / `ORACLE_USERNAME` / `ORACLE_PASSWORD`. Full guide: **`docs/FLYWAY_ET_PROFILS.md`**.

**Hospital assignments & Urgences scope** (catalog service name, prod checklist): **`docs/PERIMETRE_HOSPITALIER.md`**.

**Secrets** (via environment variables, NOT in properties):
```
JWT_ACCESS_SECRET=your-64+-char-secret
JWT_REFRESH_SECRET=your-64+-char-secret
JWT_ACCESS_EXPIRATION_SECONDS=3600
JWT_REFRESH_EXPIRATION_SECONDS=2592000
APP_BOOTSTRAP_USERNAME=...
APP_BOOTSTRAP_PASSWORD=...
```

**Datasource**: see `application-dev.properties` (H2) vs `application-oracle.properties` (Oracle, Flyway migrations under `classpath:db/migration/oracle`).

## Common Implementation Checklist

When adding a new feature or module:

1. **Model**: Create JPA entity under `{module}/model/` with `@Entity` and `@Table`
2. **Repository**: Extend `JpaRepository` under `{module}/repository/` with custom query methods
3. **DTOs**: Create request/response records under `{module}/dto/` with validation
4. **Service**: Implement business logic under `{module}/service/`
5. **Controller**: Define REST endpoints under `{module}/controller/` with `@RestController` and path prefixed with `/api/v1/`
6. **Exceptions**: If needed, define in `{module}/exception/` and register in `GlobalExceptionHandler.java`
7. **Testing**: Add integration tests under `src/test/java/` matching package structure

## Known Issues & TODOs

1. **Bootstrap provisioning**: Disable or tailor `app.bootstrap.auto-provision` when all accounts come from migrations/OPS-only processes.
2. **Future**: Optional central audit store (SIEM) beyond application logs; token **outbox** if cross-service revocation is required.

## Useful File References

| File | Purpose |
|------|---------|
| `GlobalExceptionHandler.java` | Central exception → HTTP response mapping |
| `SecurityConfig.java` | JWT security setup |
| `JwtAuthenticationFilter.java` | Bearer parsing + `jti` revocation check |
| `UserHospitalScopeService.java` | Per-user hospital service scope (admissions, urgences) |
| `JwtService.java` | Token generation/parsing (access JWT includes `jti` for logout) |
| `PatientService.java` | Example of pagination + search patterns |
| `application.properties` | All externalized config |
| `pom.xml` | Maven dependencies (JJWT 0.11.5, Spring Boot 3.3.5) |

## Language & Communication

- **Error Messages**: Use French for user-facing text
- **Code Comments**: English preferred for code logic
- **Method Naming**: English (follow Spring/Java conventions)

