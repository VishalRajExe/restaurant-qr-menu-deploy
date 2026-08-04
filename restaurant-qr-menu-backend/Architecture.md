# Architecture.md — System Architecture

### 1. High-Level Flow
```
Customer (mobile browser)
   ↓ scans QR
Angular Public Menu App
   ↓ REST/JSON
Spring Boot API Gateway/Service Layer
   ↓
MySQL (tenant-scoped data)   +   Cloud Storage (S3/Cloudinary for images)

Restaurant Admin (browser)
   ↓
Angular Admin Panel
   ↓ REST/JSON (JWT auth)
Spring Boot API
   ↓
MySQL

Super Admin (browser)
   ↓
Angular Super Admin Panel (same shell, role-gated routes)
   ↓ REST/JSON (JWT auth, SUPER_ADMIN role)
Spring Boot API
   ↓
MySQL
```

### 2. Tech Stack
- **Frontend:** Angular (standalone components, Angular Router, Angular Signals or NgRx for state), Tailwind CSS or Angular Material, Chart.js/ApexCharts for analytics.
- **Backend:** Spring Boot 3.x, Spring Web, Spring Security, Spring Data JPA/Hibernate.
- **Database:** MySQL 8.
- **Auth:** JWT (access + refresh tokens), BCrypt password hashing.
- **Storage:** Amazon S3 (preferred for scale) or Cloudinary (cheaper/simpler initially).
- **QR Generation:** ZXing (Java library).
- **Payments:** Razorpay (primary, India) + PayPal (international).
- **Caching (optional, later):** Redis for public menu read-through caching.
- **Deployment:** Docker containers; Nginx reverse proxy; CI/CD via GitHub Actions.

### 3. Multi-Tenancy Model
- Shared database, shared schema.
- Every tenant-owned table carries `restaurant_id` (directly or via `branch_id` → `restaurant_id`).
- All queries must be scoped by `restaurant_id` derived from the authenticated user's JWT claims — never trust a client-supplied `restaurant_id` for write operations.
- Public menu endpoints are scoped by restaurant `slug`/QR token, not by authenticated identity.

### 4. Backend Package Structure (Spring Boot)
> ⚠️ **CORRECTED in Phase 0 audit** — original doc had wrong package root and flat structure.

```
com.restaurantqr.platform            ← actual root (not com.restroqr.platform)
├── RestaurantQrApplication.java     ← main class (not PlatformApplication)
├── modules/
│   ├── auth/                        # JWT issuance, login, refresh, password reset
│   │   ├── AuthController.java
│   │   ├── AuthService.java
│   │   ├── AuthDtos.java
│   │   ├── CustomUserDetailsService.java
│   │   └── dto/UserRegistrationDto.java
│   ├── restaurant/                  # Restaurant profile, super-admin mgmt
│   ├── branch/
│   ├── category/
│   ├── menuitem/
│   ├── offer/
│   ├── qr/                          # QR generation (ZXing), QR metadata
│   └── subscription/                # Plans, billing
├── users/                           # User entity, repository, management service
├── analytics/                       # Scan events, aggregation
├── security/                        # JwtAuthenticationFilter, JwtTokenProvider, JwtUserDetails
├── config/                          # SecurityConfig, CloudinaryConfig, RateLimitFilter, EmailService
├── common/                          # BaseEntity, GlobalExceptionHandler, exceptions, ApiResponse
└── controller/                      # TestComponent (dev artifact — should be removed)
```

### 5. Frontend Folder Structure (Angular)
```
src/app
├── auth/                 # login, forgot/reset password
├── dashboard/             # admin dashboard widgets
├── restaurants/           # profile mgmt (admin) + super-admin restaurant list
├── branches/
├── categories/
├── menu/                  # menu items CRUD (admin) + public menu view (customer)
│   ├── admin/
│   └── public/
├── offers/
├── analytics/
├── settings/
├── shared/                # shared components, pipes, interceptors, guards
│   ├── interceptors/      # JWT attach, error handling, refresh-token
│   ├── guards/            # role guard, auth guard
│   └── components/
└── core/                  # services, models, api base config
```

### 6. Public Customer Menu URL Scheme
- `https://menu.yourdomain.com/menu/{restaurant-slug}` — restaurant landing/menu page.
- QR codes encode this URL (optionally with `?table={table_number}` for per-table analytics).

### 7. Request Flow — Customer Scan
```
QR scanned → GET /menu/{slug}
  → Angular loads public menu shell
  → API: GET /api/public/restaurants/{slug}
  → API: GET /api/public/restaurants/{slug}/menu (categories + items)
  → Render menu; log scan event async (POST /api/public/analytics/scan)
```

### 8. Security Architecture
- Stateless JWT auth; access token short-lived (~15 min), refresh token longer-lived (~7 days), rotated on use.
- Spring Security filter chain validates JWT on every protected request; role checks via `@PreAuthorize`.
- Passwords hashed with BCrypt (cost factor ≥ 12).
- Rate limiting on auth endpoints (e.g., Bucket4j) to prevent brute force.
- Input validation via Bean Validation (`@Valid`, DTO constraints) on every controller.
- CORS restricted to known frontend origins per environment.
- Soft deletes (`deleted_at`) instead of hard deletes on tenant data; audit log table records who changed what and when.

### 9. Deployment Topology
```
Internet → Nginx (TLS termination, static Angular build) → Spring Boot API (containerized)
                                                          → MySQL (managed instance)
                                                          → S3/Cloudinary (images)
```
- Separate environments: dev, staging, production.
- Database migrations managed via Flyway or Liquibase (never manual schema edits in prod).
