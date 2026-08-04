# Rules.md — AI Coding Rules & Boundaries

These rules govern how the AI assistant must behave while building this codebase. They apply to every phase and every generated file.

### 1. Stack Discipline
- Backend: **Spring Boot 3.x + Java 17+** only. Do not introduce Kotlin, Micronaut, Quarkus, or other frameworks.
- Frontend: **Angular** (latest stable, standalone components) only. Do not introduce React/Vue components into this codebase.
- Database: **MySQL** only. Do not use Mongo, Postgres, or SQLite, even "temporarily."
- ORM: **Spring Data JPA / Hibernate** — no raw JDBC unless explicitly requested for a specific performance-critical query, and even then it must be isolated in a `repository/` custom implementation, not scattered.

### 2. Approved Libraries (use these, don't substitute)
- Auth: `spring-boot-starter-security`, `jjwt` or `nimbus-jose-jwt` for JWT.
- Validation: `spring-boot-starter-validation`.
- QR generation: `com.google.zxing:core` + `javase`.
- Migrations: Flyway (preferred) — do not hand-edit schema in production.
- Charts (frontend): Chart.js or ApexCharts — pick one and stay consistent across the app.
- File upload (frontend): standard Angular `HttpClient` multipart — no third-party upload widgets unless requested.
- Image storage SDK: AWS SDK v2 (`software.amazon.awssdk:s3`) or Cloudinary Java SDK — never store images as BLOBs in MySQL.

### 3. Disallowed Practices
- No storing plaintext passwords, ever.
- No storing images/binary blobs in MySQL — only `image_url` strings.
- No hardcoded secrets, API keys, or DB credentials in source — use environment variables / `application.yml` with placeholders (`${DB_PASSWORD}`).
- No `System.out.println` for logging in backend — use SLF4J (`Logger`).
- No disabling CSRF/CORS/security globally "to make it work" — fix the actual auth flow instead.
- No skipping input validation on any endpoint that accepts a request body.
- No writing a controller that talks directly to the database — always go through a service layer.
- No cross-tenant data leaks: every query touching tenant data must filter by `restaurant_id` resolved from the authenticated principal, never from a client-supplied field, for writes and for reads outside the public menu endpoints.
- No silent `catch (Exception e) {}` blocks — every exception must be logged and re-thrown as a typed `ApiException`/mapped to a proper HTTP response via `GlobalExceptionHandler`.

### 4. Error Handling Standard
- All API errors return a consistent JSON shape:
```json
{
  "timestamp": "...",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Human readable message",
  "path": "/api/menu-items"
}
```
- Use a single `GlobalExceptionHandler` (`@ControllerAdvice`) — do not scatter try/catch error-formatting logic across controllers.
- Validation errors return field-level detail (which field failed and why).
- Never leak stack traces or internal exception messages to the client in production.

### 5. Security Rules (non-negotiable)
- Every admin/staff endpoint must be behind JWT auth and role-checked (`@PreAuthorize("hasRole(...)")`).
- Every mutating endpoint (`POST`/`PUT`/`DELETE`) must validate the requester's `restaurant_id` matches the resource's `restaurant_id` before acting.
- Passwords: BCrypt only, minimum cost factor 12.
- Rate-limit login and password-reset endpoints.
- Log security-relevant events (failed logins, role changes, deletions) to an audit table — never delete audit logs.

### 6. Code Style & Structure
- Follow the package structure defined in `Architecture.md` — do not invent new top-level packages without updating that file.
- One responsibility per class: Controller → Service → Repository. No business logic in controllers or repositories.
- DTOs for all API input/output — never expose JPA entities directly in responses.
- Angular: one component per feature folder, services for API calls, no direct `HttpClient` calls inside components.

### 7. What the AI Should Do
- Always ask for clarification if a requirement conflicts with an existing phase's contract (e.g., a DB column that was already defined differently).
- Always update `Memory.md` after completing a meaningful unit of work (see `Memory.md` rules).
- Always generate accompanying unit/integration tests for new service-layer logic where feasible.
- Always scope work to the current phase in `Phases.md` — do not jump ahead and build later-phase features unless explicitly asked.

### 8. What the AI Should NOT Do
- Should not regenerate or rewrite unrelated files "while it's at it."
- Should not change the DB schema defined in `Architecture.md`/PRD without flagging the change explicitly.
- Should not introduce a new third-party service (payment provider, storage provider, analytics tool) without explicit approval.
- Should not remove security checks, soft-delete logic, or audit logging to "simplify" a fix.
