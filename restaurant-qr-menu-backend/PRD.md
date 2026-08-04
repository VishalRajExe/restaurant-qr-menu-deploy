# PRD.md — Product Requirements Document
## Restaurant QR Menu SaaS Platform

### 1. Overview
A multi-tenant SaaS platform that lets restaurants publish a digital menu accessed via QR code at each table. Customers scan a QR code, view the live menu (no app install), and restaurants manage everything through an admin panel. A super admin layer manages the platform, restaurants, and subscriptions.

### 2. Problem Statement
Restaurants need a low-cost, easy way to replace printed menus with a fast, mobile-friendly, always-up-to-date digital menu, without hiring developers per restaurant. A single SaaS platform lets many restaurants onboard themselves, manage their own menu, and pay a subscription.

### 3. Target Users
- **Customers** — diners who scan a QR code at a table and browse the menu.
- **Restaurant Owner / Manager / Staff** — manage restaurant profile, branches, categories, items, offers, and QR codes.
- **Super Admin (Platform Owner)** — manages tenants (restaurants), subscription plans, billing, support, and platform-wide analytics.

### 4. Goals
- Zero-friction menu viewing for customers (no login, no app).
- Self-service onboarding for restaurants.
- Multi-branch, multi-tenant support from day one.
- Subscription-gated feature access (Basic / Professional / Enterprise).
- Reliable, secure, auditable admin operations.

### 5. Non-Goals (v1)
- Online food ordering / cart / checkout (out of scope for MVP; may be Phase N+1).
- Table reservation system.
- POS integration.
- Native mobile apps (mobile-responsive web only).

### 6. Functional Requirements

#### 6.1 Customer-Facing
- Scan QR → open digital menu instantly.
- Browse categories (Starters, Main Course, Desserts, Drinks, etc.).
- View item details: name, price, description, image, veg/non-veg badge.
- Search menu items instantly.
- Filter by veg/non-veg and price range.
- View active offers/discounts.
- Multi-language support.
- Dark/Light mode toggle.
- Fully mobile-responsive.

#### 6.2 Restaurant Admin
- Secure login (JWT-based), forgot/reset password.
- Dashboard: total categories, total items, today's scans, popular items, charts.
- Manage restaurant profile (name, logo, phone, email, address).
- Manage branches (add/edit/deactivate).
- Manage categories (CRUD, drag-and-drop ordering, display order).
- Manage menu items (CRUD, image upload, bulk upload, availability toggle).
- Manage offers (title, description, discount %, start/end date).
- Generate, download, and print QR codes per table/branch.
- View analytics (menu views, popular items, peak hours, device type).
- Role-based access: RESTAURANT_OWNER, MANAGER, STAFF.

#### 6.3 Super Admin
- Manage all restaurants (create, suspend, delete).
- Subscription plan management (Basic, Professional, Enterprise).
- Revenue tracking and billing history.
- Platform-wide user management.
- Support ticket management.
- Platform-wide analytics dashboard.

### 7. Subscription Plans
| Plan | Branches | Menu Items | Notes |
|---|---|---|---|
| Basic | 1 | 100 | Entry tier |
| Professional | 5 | Unlimited | Growth tier |
| Enterprise | Unlimited | Unlimited | Full access |

Payment integration: Razorpay and PayPal for recurring billing.

### 8. Success Metrics
- Time from signup to first published menu < 15 minutes.
- QR scan-to-menu-load time < 2 seconds on 4G.
- < 1% failed QR scans due to app errors.
- Admin panel task completion rate (create item, generate QR) > 95% without support tickets.

### 9. Assumptions & Constraints
- Single database, multi-tenant via `restaurant_id` scoping (not separate DBs per tenant, for MVP).
- Images stored in cloud object storage (S3/Cloudinary), never in MySQL.
- Public menu pages must be crawlable/shareable via clean URLs (`/menu/{slug}` or `/r/{slug}`).

### 10. Out-of-Scope Risks to Flag Later
- GDPR/data residency if expanding internationally.
- Multi-currency support.
- White-labeling per restaurant domain.
