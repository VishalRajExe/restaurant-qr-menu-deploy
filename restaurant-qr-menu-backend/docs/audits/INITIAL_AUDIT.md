# INITIAL_AUDIT.md — Phase 0 Initial Audit Report
**Date:** 2026-07-20  
**Auditor:** Phase 0 Automated Audit  
**Scope:** Backend only — `com.restaurantqr.platform`  
**Build baseline:** `mvn clean compile` → **SUCCESS** (76 files)  
**Test baseline:** `mvn clean test` → **FAIL** (test compile error)

---

## 1. Architecture Detected

**Framework:** Spring Boot 3.x (POM version 3.2.3), Java 17  
**Database:** H2 (dev/test), MySQL 8 (prod via `DB_URL` env var)  
**Auth:** JJWT (JWT), BCrypt (cost factor 12)  
**Storage:** Cloudinary  
**QR:** ZXing  
**Migration:** Flyway (`V1__init.sql`)  
**Rate limiting:** Bucket4j (implemented)  
**Email:** Spring Mail (JavaMailSender)  
**Package root:** `com.restaurantqr.platform`

---

## 2. Modules Detected

| Module | Package | Status |
|--------|---------|--------|
| Auth | `modules.auth` | Implemented |
| Users | `users` | Implemented |
| Restaurant | `modules.restaurant` | Implemented |
| Branch | `modules.branch` | Implemented |
| Category | `modules.category` | Implemented |
| MenuItem | `modules.menuitem` | Implemented |
| Offer | `modules.offer` | Implemented |
| QR Code | `modules.qr` | Implemented |
| Analytics | `analytics` | Implemented |
| Subscription | `modules.subscription` | Implemented (no payment gateway webhooks) |
| Security | `security`, `config` | Implemented |
| Common | `common` | Implemented |

---

## 3. Entities & Relationships

```
Restaurant (root tenant)
  ├── Branch (restaurant_id FK, soft-delete)
  ├── Category (restaurant_id FK, soft-delete)
  │     └── MenuItem (category_id + restaurant_id FK, soft-delete)
  ├── Offer (restaurant_id FK, soft-delete)
  ├── QrCode (branch_id + restaurant_id FK, soft-delete)
  │     └── ScanEvent (qr_code_id + restaurant_id FK, NO soft-delete)
  └── Subscription (restaurant_id FK)

User (restaurant_id FK nullable — null for SUPER_ADMIN)
```

All tenant entities extend `BaseEntity` which provides: `id`, `createdAt`, `updatedAt`, `isDeleted`, `deletedAt`.

---

## 4. Security Architecture

- **Filter chain order:** `RateLimitFilter` → `SecurityHeadersFilter` → `LoggingFilter` → `JwtAuthenticationFilter` → Spring Security
- **Session:** STATELESS
- **CSRF:** Disabled (stateless JWT — acceptable for REST API)
- **CORS:** Configured to allowed origins from env vars + localhost:4200/4201
- **BCrypt:** Cost factor 12 ✓
- **JWT:** Access token (1 day), Refresh token (7 days) — HMAC-SHA256
- **Rate limiting:** auth=20/min, public=120/min per IP (Bucket4j)
- **Security headers:** HSTS, CSP, X-Frame-Options, X-Content-Type-Options

---

## 5. Tests Found

| File | Type | Status |
|------|------|--------|
| `AuthServiceTest.java` | Unit (Mockito) | **BROKEN** — uses old `RegisterRequest` instead of `UserRegistrationDto` |
| `PublicMenuControllerIntegrationTest.java` | Integration (MockMvc) | **COMPILES** — not executed due to AuthServiceTest failure |
| `ControllerTest.java` | Unknown | Present |
| `SuperAdminControllerTest.java` | Unknown | Present |

**Baseline test result:** BUILD FAILURE — 2 test compile errors in `AuthServiceTest`

---

## 6. Findings by Priority

---

### 🔴 P0 — CRITICAL

#### P0-1: JWT Secret — No default fallback (CORRECT but must verify env)
**File:** `application.yml` line 62  
```yaml
jwt:
  secret: ${JWT_SECRET}
```
**Status:** No default value — app will fail to start if `JWT_SECRET` not set. This is CORRECT behavior. However, the previous Memory.md indicates a secret was committed in plaintext in an earlier version. **The historical secret must be treated as compromised and rotated.**  
**Action:** Verify `JWT_SECRET` is sufficiently long (≥256 bits / 32 chars for HMAC-SHA256).

#### P0-2: Subscription Activation Has No Payment Verification
**File:** `SubscriptionController.java` line 45-50, `SubscriptionService.java` line 47-82  
```java
@PostMapping("/restaurants/{restaurantId}/activate")
@PreAuthorize("hasAnyRole('RESTAURANT_OWNER','SUPER_ADMIN')")
public ResponseEntity<...> activate(@PathVariable Long restaurantId, @Valid @RequestBody SubscriptionRequest request) {
    return ResponseEntity.ok(ApiResponse.success("Subscription activated",
            subscriptionService.activate(restaurantId, request)));
}
```
**Risk:** Any authenticated RESTAURANT_OWNER can call this endpoint directly and activate any plan (ENTERPRISE, etc.) without paying — no Razorpay/PayPal webhook signature verification. The `paymentId` field is optional/unverified.  
**Severity:** P0 — complete payment bypass.

#### P0-3: Cross-Tenant Access via `RestaurantController.getById`
**File:** `RestaurantController.java` line 30-33  
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<Restaurant>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(restaurantService.findById(id)));
}
```
`findById` calls `assertRestaurantAccess` which correctly blocks cross-tenant access **for authenticated users**. However, in `SecurityConfig`, the rule `requestMatchers(HttpMethod.GET, prependContextPath("/restaurants/*")).permitAll()` makes this endpoint **publicly accessible** — which means unauthenticated users can enumerate restaurant data by ID. While it may be intentional for customer-facing use, it leaks internal restaurant data to anyone.  
**Note:** This also conflicts with the access-control design — the rule "permit all" for `GET /restaurants/*` undermines `assertRestaurantAccess` for authenticated managers from Restaurant B trying to access Restaurant A's data.

#### P0-4: `GET /restaurants/{id}` Exposes All Restaurant Data Publicly
**File:** `SecurityConfig.java` line 91-93  
```java
.requestMatchers(HttpMethod.GET, prependContextPath("/restaurants/*")).permitAll()
```
This also unintentionally makes `GET /restaurants/{id}` public even though the route is intended for the admin panel, not the public customer menu. The public menu only needs `GET /restaurants/slug/{slug}`.

#### P0-5: Access Token Expiry Is 24 Hours (Architecture Says 15 Minutes)
**File:** `application.yml` line 63  
```yaml
access-token-expiration: 86400000  # 1 day (ms)
```
**Architecture.md** specifies: _"access token short-lived (~15 min)"_. 86400000 ms = **24 hours**. This means a stolen token is valid for 24 hours instead of 15 minutes — severely increasing the attack window for token theft.

#### P0-6: RESTAURANT_OWNER Can Modify Any Restaurant via `PUT /restaurants/{id}`
**File:** `RestaurantController.java` line 54-60, `RestaurantService.java` line 72-92  
```java
@PutMapping("/{id}")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'RESTAURANT_OWNER')")
public ResponseEntity<ApiResponse<Restaurant>> update(@PathVariable Long id, ...)
```
`update` calls `findById(id)` which calls `assertRestaurantAccess(id)`. The access check IS in place. However, the SecurityConfig rule `requestMatchers(prependContextPath("/restaurants/**")).hasAnyRole(...)` allows the URL through before the method-level check. The path-level check is role-based, the service-level check is ownership-based. **This chain appears correct** — but must be verified with runtime tests. **Flagging for Phase 1 verification.**

---

### 🟠 P1 — HIGH

#### P1-1: Test Suite Is Broken (`AuthServiceTest` Doesn't Compile)
**File:** `AuthServiceTest.java` lines 56-61, 73-78  
Tests still use the old `RegisterRequest` DTO (package-private class in `AuthDtos.java`) after the refactoring to `UserRegistrationDto`. Tests cannot compile, so the test suite provides **zero regression protection** for the auth layer.

#### P1-2: No Payment Webhook Endpoint
**PRD §7** specifies Razorpay + PayPal integration. No `/webhooks/**` endpoint exists. The subscription activate endpoint is directly callable without payment proof. PRD requirement is unmet.

#### P1-3: `ImageUploadController.uploadLogo` / `uploadBanner` Has No Ownership Check
**File:** `ImageUploadController.java` lines 41-52, 55-67  
```java
restaurantRepository.findById(restaurantId).ifPresent(r -> {
    r.setLogoUrl(url);
    restaurantRepository.save(r);
});
```
Any RESTAURANT_OWNER (from a different restaurant) can call `POST /upload/restaurants/{anyId}/logo` and overwrite another restaurant's logo with an image. The `restaurantId` from the URL is trusted without verifying the caller owns it.

#### P1-4: `SuperAdminController` Duplicates `RestaurantController` Functionality
`SuperAdminController` has its own `/super-admin/restaurants` listing endpoint (`RestaurantRepository.findAllActive`) duplicating `RestaurantController`'s `/restaurants` endpoint. Not a security issue but violates DRY and could cause divergence.

#### P1-5: `BranchController.findById` Does Not Verify Branch Belongs to Restaurant
**File:** `BranchController.java` line 29-34  
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<Branch>> getById(@PathVariable Long restaurantId, @PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(branchService.findById(id)));
}
```
`BranchService.findById` only checks `!b.getIsDeleted()` — it does NOT assert the branch belongs to `restaurantId`. An owner of Restaurant A can read Restaurant B's branch data by using their own valid JWT and a guessed branch ID.

#### P1-6: `CategoryController.getById` Does Not Verify Category Belongs to Restaurant
Same pattern as P1-5 — `categoryService.findById(id)` does not validate `restaurantId` ownership.

#### P1-7: Entities Returned Directly as API Responses (No DTOs)
**Rules.md §6:** _"DTOs for all API input/output — never expose JPA entities directly in responses."_  
Most controllers return JPA entities (`Restaurant`, `Branch`, `Category`, `MenuItem`, `Offer`, `QrCode`, `User`) directly. While `@JsonIgnore` annotations prevent some leaks, this pattern is fragile — adding a new field to an entity can accidentally expose it.

#### P1-8: `System.out.println` Used in Production Code
**File:** `SecurityConfig.java` lines 63, 76-78, 116-117 (within `loggingFilter`)  
**Rules.md §3:** _"No `System.out.println` for logging in backend — use SLF4J."_  
7 `System.out.println` calls found in `SecurityConfig`.

#### P1-9: `TestComponent` in Production Source Tree
**File:** `controller/TestComponent.java`  
A `@Component` with no purpose other than logging its initialization lives in the production `controller/` package. Should not be in production code.

---

### 🟡 P2 — MEDIUM

#### P2-1: Access Token Lifetime 24h vs Architecture Spec of 15 min
(Also flagged as P0-5 — double-listed here for scheduling clarity)

#### P2-2: `JwtUserDetails` — `isAccountNonExpired`, `isAccountNonLocked`, `isCredentialsNonExpired` Always Return `true`
**File:** `JwtUserDetails.java` lines 38-40  
If a user's account is SUSPENDED or credentials are compromised, these hardcoded `true` returns mean Spring Security will not block login at the `UserDetails` level. Only `isEnabled()` (which checks `Status.ACTIVE`) is properly implemented.

#### P2-3: Password Reset Token Not Indexed
**File:** `V1__init.sql` line 144  
`reset_token` column exists but has no database index. `findByResetTokenAndIsDeletedFalse` will perform a full table scan on the users table.

#### P2-4: `ScanEvent` Does Not Extend `BaseEntity`
**File:** `ScanEvent.java`  
Does not use the common `BaseEntity`, so lacks `updatedAt` and soft-delete support. While analytics events generally shouldn't be deleted, it deviates from the project's established pattern.

#### P2-5: `ScheduledTasks.java` — Not Reviewed
A `ScheduledTasks.java` exists in config but was not read during this audit. Its behavior is unknown and must be verified.

#### P2-6: `ddl-auto: update` in Application Config
**File:** `application.yml` line 25  
`ddl-auto: update` allows Hibernate to silently alter the database schema in development. In production this can cause data loss. Should be `validate` in prod.

#### P2-7: Category Image Upload Has No File Type Validation
**File:** `CategoryController.java` line 82-92, `CloudinaryUploadService`  
No MIME type or file extension validation before uploading to Cloudinary. An attacker could upload arbitrary content.

#### P2-8: `PublicMenuController` Returns Raw JPA Entities with Full Data
**File:** `PublicMenuController.java`  
The `MenuPayload` DTO includes direct JPA entity references (`Restaurant`, `Category`, `MenuItem`, `Offer`, `QrCode`). While `@JsonIgnore` guards exist, the public menu endpoint should use read-only public DTOs to prevent accidental data leakage as the entities evolve.

#### P2-9: `loggingFilter` in SecurityConfig Is Debug-Level and Logs All Paths
**File:** `SecurityConfig.java` line 108-120  
Every request URI is printed to stdout via `System.out.println` in the `loggingFilter`. This is a debug artifact that must not ship to production.

#### P2-10: No `@EnableJpaAuditing` Detected
The `BaseEntity` uses `@CreatedDate`/`@LastModifiedDate` which requires `@EnableJpaAuditing` on the application class. If missing, `createdAt` and `updatedAt` will be null.

---

### 🔵 P3 — LOW

#### P3-1: Architecture.md Package Structure Differs from Actual Code
**Architecture.md** shows: `com.restroqr.platform` (wrong prefix) with flat top-level packages (`auth/`, `users/`, `restaurants/`).  
**Actual code:** `com.restaurantqr.platform` with `modules/` sub-grouping.  
**Action:** Architecture.md needs update (done below in §8).

#### P3-2: `Phases.md` Still Shows Phase 0 Bootstrap — Needs Update to Reflect Current State
Several phase descriptions reference incomplete status.

#### P3-3: Duplicate `UserRegistrationDto` Reference in Memory.md
Memory.md references a duplicate DTO in `com.restaurantqr.platform.auth.dto` — this package does not exist in the current codebase. Memory.md is stale here.

#### P3-4: `auth` Module Still Under `modules.auth` But Users Under `users` (Inconsistent)
Users (`User` entity, `UserRepository`, `UserManagementService`) are in top-level `users` package, while auth is in `modules.auth`. This is a structural inconsistency from the migration described in Memory.md.

#### P3-5: No Multi-Language Support
**PRD §6.1** requires multi-language support. Not implemented.

#### P3-6: No Audit Log Table
**Architecture.md §8** states: _"audit log table records who changed what and when."_ No `audit_log` table exists in `V1__init.sql` and no audit log service is implemented.

#### P3-7: No Bulk Upload Endpoint for Menu Items
**PRD §6.2** specifies bulk upload (CSV or batch JSON) for menu items. Not implemented.

#### P3-8: No `subscriptions` Table Referenced in Migration
The `V1__init.sql` Flyway migration does not include a `subscriptions` table. The `Subscription` entity exists in JPA but relies on `ddl-auto: update` to create the table, not Flyway. This is a rules violation (`Rules.md §2`: use Flyway for migrations).

---

## 7. PRD Comparison Matrix

| PRD Requirement | Implemented | Notes |
|----------------|-------------|-------|
| JWT auth, forgot/reset password | ✅ Yes | Access token 24h (should be 15min) |
| Restaurant profile management | ✅ Yes | — |
| Branch management | ✅ Yes | — |
| Category CRUD + drag-and-drop order | ✅ Yes | — |
| Menu items CRUD + image upload | ✅ Yes | — |
| Bulk upload for menu items | ❌ No | P3 |
| Offers with date ranges | ✅ Yes | — |
| QR code generation (ZXing) | ✅ Yes | — |
| Analytics (scan events, dashboard) | ✅ Partial | 1 endpoint; limited aggregation |
| Role-based access (OWNER/MANAGER/STAFF) | ✅ Yes | — |
| SUPER_ADMIN management | ✅ Yes | — |
| Subscription plans (Basic/Pro/Enterprise) | ✅ Partial | No payment verification |
| Razorpay/PayPal integration | ❌ No | No webhook handlers |
| Multi-language support | ❌ No | — |
| Soft deletes + audit trail | ✅ Partial | Soft-delete yes; no audit log table |
| Public menu (no login) | ✅ Yes | — |
| Search + filter menu | ✅ Yes | — |
| Dark/light mode, mobile-responsive | N/A | Frontend only |
| Image storage in Cloudinary (not MySQL) | ✅ Yes | — |

---

## 8. Architecture.md Corrections

The following discrepancies between Architecture.md and actual code were identified:

| Architecture.md States | Actual Code |
|------------------------|-------------|
| Package root: `com.restroqr.platform` | `com.restaurantqr.platform` |
| Flat: `auth/`, `users/`, `restaurants/` | Mixed: `modules/{auth,restaurant,branch,...}` and top-level `users/`, `analytics/` |
| `PlatformApplication.java` | `RestaurantQrApplication.java` |
| Auth in `auth/security/` | JWT classes in top-level `security/` package |
| Refresh token "rotated on use" | Refresh tokens NOT rotated — same token reusable until expiry |

> **Architecture.md will be updated separately to correct the package structure.**

---

## 9. Baseline Build / Test Result

| Step | Result |
|------|--------|
| `mvn clean compile` | ✅ SUCCESS — 76 files |
| `mvn clean test` | ❌ FAIL — test compile error (2 errors in AuthServiceTest) |
| Test error | `RegisterRequest` cannot be converted to `UserRegistrationDto` |
| Root cause | AuthService was refactored; test was not updated |

---

## 10. Summary by Priority

| Priority | Count | Items |
|----------|-------|-------|
| P0 Critical | 5 | P0-1 thru P0-6 (P0-6 needs runtime verification) |
| P1 High | 9 | P1-1 thru P1-9 |
| P2 Medium | 10 | P2-1 thru P2-10 |
| P3 Low | 8 | P3-1 thru P3-8 |
| **Total** | **32** | — |

---

## 11. Files Created/Modified (Phase 0)

| File | Action |
|------|--------|
| `docs/audits/API_INVENTORY.md` | CREATED |
| `docs/audits/INITIAL_AUDIT.md` | CREATED |
| `Memory.md` | UPDATE PENDING |
| `Phases.md` | UPDATE PENDING |
| `Architecture.md` | UPDATE PENDING (package names corrected) |
