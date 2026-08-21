<div align="center">

# 🍽️ RestQR — Restaurant QR Menu & Order Platform

**A full-stack, multi-tenant SaaS that turns every table into a digital dining experience.**

Scan a QR → Browse a beautiful live menu → Order from your phone → Kitchen fires the ticket in real time.

<img src="https://img.shields.io/badge/Spring_Boot-3.2.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot"/>
<img src="https://img.shields.io/badge/Angular-21.2-DD0031?style=for-the-badge&logo=angular&logoColor=white" alt="Angular"/>
<img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"/>
<img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17"/>
<img src="https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT"/>
<img src="https://img.shields.io/badge/Tailwind-4.3-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white" alt="Tailwind"/>

[🚀 Quick Start](#-quick-start) · [🔑 Demo Accounts](#-demo-accounts--seeded-data) · [🧩 Features](#-features) · [🔌 API](#-api-overview) · [🗺️ Roadmap](#️-roadmap)

</div>

---

## 📖 About The Project

**RestQR** (frontend app name: *AuraMenu*) is a production-style **multi-tenant Restaurant QR Menu & Order Management System**. A single platform hosts many restaurants: each owner gets a branded admin dashboard, chefs get a live kitchen display, customers get a no-login mobile menu via per-table QR codes, and a Super Admin runs the whole SaaS.

Built to the roadmap in `RESTRAUNT QR MENU APP.pdf` — customer menu, admin panel, QR flow, analytics, subscriptions and support are all implemented.

```
┌─────────────┐     scans      ┌──────────────────┐     https      ┌─────────────────────┐
│   📱 Guest   │ ────────────▶ │  QR Code on Table │ ────────────▶ │  Customer Menu (SPA) │
└─────────────┘                └──────────────────┘                └─────────┬───────────┘
                                                                             │ REST (JSON)
                                              ┌────────────────────────────┼──────────────────────────┐
                                              ▼                            ▼                          ▼
                                     ┌─────────────────┐         ┌──────────────────┐        ┌──────────────────┐
                                     │  Spring Boot API │         │  Owner Dashboard  │        │  Chef Dashboard   │
                                     │  :8080 /api/v1   │         │  :4200 /dashboard │        │  :4200 /dashboard │
                                     └────────┬────────┘         └────────┬─────────┘        └────────┬─────────┘
                                              │                           │                           │
                                     ┌────────▼───────────────────────────▼───────────────────────────▼──────┐
                                     │                    MySQL 8 — restaurant_qr_db                        │
                                     │   restaurants · branches · categories · menu_items · orders · tables   │
                                     │   qr_codes · users · subscriptions · tickets · chat · analytics …     │
                                     └──────────────────────────────┬───────────────────────────────────────┘
                                                                    │ image URLs only
                                                       ┌────────────▼────────────┐
                                                       │  Cloudinary (images/QR)  │
                                                       └─────────────────────────┘
```

---

## ✨ Features

### 🧑‍🤝‍🧑 Customer Portal — *no login, no app install*
| Feature | Details |
|---|---|
| 📷 QR Scan → Menu | Each table QR opens `/menu/{token}?table=N` (also `/menu/{slug}` or `/r/{slug}`) |
| 🔍 Instant Search | Live dish search with category chips |
| 🥬 Veg / Non-Veg Filters | Badge filters + max-price slider |
| 🛒 Cart & Checkout | 5% tax auto-applied, mobile + name + special instructions |
| 📡 Live Order Tracking | Status polled every 2.5 s: `PENDING → ACCEPTED → PREPARING → READY → COMPLETED` |
| 🧾 Bill Printing & History | Printable bill, order history, track-by-identifier |
| 🌙 Dark Mode | One-tap theme toggle |
| 🆘 Report an Issue | In-menu support ticket (food quality / menu issue / general) |

### 🏪 Owner Dashboard — `/dashboard/owner`
- **Overview** — KPI cards, scan-activity feed, dashboard analytics (views, popular items, scan trend)
- **Orders** — status pipeline with cancel / retry / restore, KOT print, tax invoice, live-tracking modal with timeline
- **Menu & Categories** — full CRUD, image upload, availability toggle, popular/chef-special flags, category reorder
- **Tables** — floor plan (Available / Occupied / Reserved / Cleaning), reservations, session close/restore
- **QR Studio** — generate / regenerate / deactivate per-table QRs, download, print, scan counts
- **Customers** — phone-number order-history lookup
- **Support** — ticket threads, escalate to Super Admin, resolve
- **Settings** — restaurant profile, branding color, chef invite code
- **Live side panel** — direct chat with the chef, notification center, smart global search

### 👨‍🍳 Chef Dashboard — `/dashboard/chef`
Live order queue with filters (all / pending / preparing / done), one-tap status updates, owner↔chef direct chat, support tickets and notifications — refreshed on a 2.5 s poll.

### 🛡️ Super Admin Console — `/dashboard/admin`
Platform KPIs (restaurants, scans, revenue, Pro-plan count), weekly/monthly/yearly sales views, restaurant list with activate / suspend / verification, cross-restaurant customer search, all-platform ticket queue with SLA tracking and saved replies.

### 💰 SaaS Layer
| Plan | Branches | Menu Items | Staff | Storage | Extras |
|---|---|---|---|---|---|
| **Starter** | 1 | 100 | 2 | 1 GB | — |
| **Professional** | 5 | Unlimited | 10 | 10 GB | Custom domain |
| **Business** | 15 | Unlimited | 50 | 50 GB | Advanced analytics |
| **Enterprise** | Unlimited | Unlimited | Unlimited | Unlimited | API access |

Subscriptions auto-expire via a daily 01:00 scheduled job; 09:00 job emails expiry reminders. Coupons, usage meters and invoices are modeled in the API.

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17 · Spring Boot 3.2.3 · Spring Security (stateless JWT) · Spring Data JPA · Validation · Mail · Cache · Actuator |
| **Database** | MySQL 8 (`restaurant_qr_db`, Hibernate `ddl-auto: update`) |
| **Security** | JJWT 0.11.5 (15-min access / 7-day refresh tokens) · BCrypt · Bucket4j rate limiting · security-headers filter |
| **Media** | Cloudinary 1.36 (menu images, logos, QR PNGs) with pluggable `StorageProvider` (S3 stub included) |
| **QR** | ZXing 3.5.2 — server-side PNG generation |
| **Frontend** | Angular 21.2 (standalone components, signals, `@if/@for`) · Tailwind CSS 4.3 · lucide-angular · Plus Jakarta Sans |
| **Tooling** | Maven · Lombok · MapStruct · Vitest · SonarQube plugin |

---

## 📂 Repository Layout

```
rest.qr.backend/
├── restaurant-qr-menu-backend/     # 🌱 Spring Boot REST API  → :8080/api/v1
│   └── src/main/java/com/restaurantqr/platform/
│       ├── modules/                # Feature modules (controller/service/entity/repository)
│       │   ├── auth/               #   Login, register, refresh, forgot/reset, invitations
│       │   ├── restaurant/         #   Restaurant CRUD + PublicMenu + SuperAdmin controllers
│       │   ├── menuitem/           #   Menu CRUD, availability, image upload
│       │   ├── category/           #   Categories + reorder
│       │   ├── order/              #   Owner order mgmt + PublicOrderController (guest)
│       │   ├── table/              #   Dining tables, reservations, sessions
│       │   ├── qr/                 #   QR generation (ZXing) + public token lookup
│       │   ├── chat/               #   Owner ↔ chef direct chat
│       │   ├── ticket/             #   Support tickets + admin console + knowledge base
│       │   ├── notification/       #   In-app notifications
│       │   ├── subscription/       #   Plans, coupons, invoices, usage meters
│       │   ├── analytics/          #   Scan events, search logs, dashboards
│       │   ├── media/              #   Cloudinary/S3 storage providers
│       │   ├── branch/ customer/ offer/ report/ enterprise/ users/ admin/
│       ├── security/               #   JwtTokenProvider, JwtAuthenticationFilter
│       ├── config/                 #   SecurityConfig, seeders, mail, rate limits, scheduled jobs
│       └── common/                 #   ApiResponse envelope, exceptions, BaseEntity (soft delete + audit)
├── restaurant-qr-menu-frontend/    # 💠 Angular 21 SPA → :4200
│   └── src/app/
│       ├── pages/                  #   landing, login, signup, customer-menu,
│       │                           #   owner-dashboard, chef-dashboard, admin-dashboard
│       ├── services/               #   23 injectable API/state services (signals)
│       ├── interceptors/           #   JWT attach + 401 refresh-retry
│       └── mock-data/              #   Offline fallback data
├── landing/                        # 🎨 Static Bootstrap template — design/asset source
├── frontendmd/                     # 📐 "Epicure OS" design system (DESIGN.md + HTML prototypes)
├── seed_menu_items.sql             # 🌾 Optional richer demo menu for restaurant #1
└── README.md
```

---

## 🚀 Quick Start

### Prerequisites
| Tool | Version |
|---|---|
| JDK | 17+ |
| Maven | 3.8+ |
| Node.js | 20+ (npm 10) |
| MySQL | 8.0 running locally on `:3306` |

### 1 · Create the database
```sql
CREATE DATABASE restaurant_qr_db;
```
Tables are auto-created by Hibernate on first boot. Optionally load the richer demo menu:
```bash
mysql -u root -p restaurant_qr_db < seed_menu_items.sql
```

### 2 · Configure & run the backend
```bash
cd restaurant-qr-menu-backend
cp .env.example .env        # then edit DB creds / JWT secret / Cloudinary / SMTP
mvn spring-boot:run
```
The API starts at **`http://localhost:8080/api/v1`** and auto-seeds demo data on first boot. All settings are environment-variable driven (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CLOUDINARY_*`, `MAIL_*`, `QR_BASE_URL`, `FRONTEND_URL`) — see `.env.example` for Mailtrap/Gmail/SendGrid/SES/Mailpit options.

### 3 · Run the frontend
```bash
cd restaurant-qr-menu-frontend
npm install
npm run start               # ng serve → http://localhost:4200
```

### 4 · Try the full loop
1. Open **http://localhost:4200** → log in as the owner → *QR Studio* → download/print a table QR.
2. Scan it (or just visit `http://localhost:4200/menu/gourmet-bistro?table=1`).
3. Add dishes to the cart, checkout, and watch the order land in the **Owner** and **Chef** dashboards.
4. Advance the status from the chef queue — the customer's tracker updates live.

---

## 🔑 Demo Accounts & Seeded Data

Seeded automatically on first boot (`DatabaseSeeder` / `DatabaseDataSeeder`):

| Portal | Email | Password | Role |
|---|---|---|---|
| 🏪 Owner | `owner@restaurantqr.com` | `Owner@12345` | `RESTAURANT_OWNER` |
| 👨‍🍳 Chef | `chef@restaurantqr.com` | `Chef@12345` | `STAFF` (chef portal) |
| 🛡️ Super Admin | `admin@restaurantqr.com` | `Admin@12345` | `SUPER_ADMIN` |
| 🍽️ Customer | *no login needed* | — | open `/menu/gourmet-bistro` |

Seeded restaurant: **Gourmet Bistro** (slug `gourmet-bistro`) with a flagship branch, 4 categories (Appetizers, Main Course, Desserts, Beverages), menu items, sample orders and per-table QR codes.

---

## 🔌 API Overview

Base URL: `http://localhost:8080/api/v1` · Envelope: `{ "success", "message", "data" }`

<details>
<summary><b>🔐 Auth</b> — public</summary>

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/auth/login` | Email + password → access & refresh tokens |
| POST | `/auth/register` | Owner signup (creates restaurant + trial) |
| POST | `/auth/refresh` | Rotate access token |
| POST | `/auth/forgot-password` / `/auth/reset-password` | Email reset flow |
| POST | `/auth/change-password` | Authenticated password change |
| GET/POST | `/auth/invitations/{token}` · `/auth/invitations/accept` | Staff invite flow |
</details>

<details>
<summary><b>🌍 Public (guest) endpoints</b> — no auth</summary>

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/public/menu/{token}` | Unified menu by QR token **or** slug |
| GET | `/public/menu/restaurant/{slug}` | Menu by restaurant slug |
| GET | `/public/restaurants/{id}/menu` · `/offers` · `/tables` | Menu, offers, live table status |
| GET | `/public/menu/restaurants/{id}/search` · `/recommended` · `/recently-added` · `/combos` | Discovery |
| POST | `/public/orders` | Guest places an order |
| GET | `/public/orders/track?…` · `/{orderNumber}` | Guest order tracking |
| GET | `/public/qr/{token}` | Resolve QR → restaurant/table |
| POST/GET | `/tickets/public/…` | Guest issue reporting & tracking |
</details>

<details>
<summary><b>🏪 Restaurant management</b> — role: OWNER / MANAGER / STAFF</summary>

| Group | Endpoints |
|---|---|
| Restaurant | `GET/POST /restaurants` · `GET/PUT/DELETE /restaurants/{id}` · `PATCH /{id}/chef-code` · `GET /restaurants/slug/{slug}` |
| Categories | `GET/POST /restaurants/{id}/categories` · `PUT /{id}` · `PUT /reorder` · `PATCH /{id}/toggle-status` · `POST /{id}/image` · `POST /{id}/restore` |
| Menu items | `GET/POST /restaurants/{id}/menu-items` · `PUT/PATCH/DELETE /{id}` · `PATCH /{id}/availability` · `POST /{id}/restore` |
| Orders | `GET /restaurants/{id}/orders` · `PATCH /{orderId}/status` |
| Tables | `GET/POST /restaurants/{id}/tables` · `PATCH /{tableId}/status` · `POST /{tableId}/reserve|close|restore` · `GET /stats` |
| QR codes | `GET/POST /restaurants/{id}/qr-codes` · `POST /{id}/regenerate` · `PATCH /{id}/deactivate` · `DELETE /{id}` |
| Offers | `GET/POST/PUT/DELETE /restaurants/{id}/offers…` · `POST /{id}/banner` |
| Media | `POST /media/restaurants/{id}/upload` · `/upload-multiple` · gallery / crop / CDN URL |
| Chat | `/restaurants/{id}/chat/contacts` · `/threads/{userId}` · `/messages` · `/unread-count` |
| Users | `/restaurants/{id}/users` CRUD · `/invite` · invitations |
| Customers | `/restaurants/{id}/customers/history` · `/recent` |
| Analytics | `GET /analytics/restaurants/{id}/dashboard` · `/summary` · audit timeline |
</details>

<details>
<summary><b>💰 Subscriptions & 🛡️ Super Admin</b></summary>

| Group | Endpoints |
|---|---|
| Subscriptions | `GET /subscriptions/plans` (public) · `/restaurants/{id}/active|history|usage-meter|invoices` · `POST …/activate|cancel|apply-coupon|auto-renew` · `POST /subscriptions/coupons` |
| Super Admin | `GET /super-admin/stats|restaurants|users` · `PATCH /restaurants/{id}/status|verification` · `/subscriptions/expiring-soon` |
| Admin tickets | `/admin/tickets` · dashboard · assign/escalate/resolve · saved-replies · `sla-overdue` |
| SaaS console | `/admin/saas/dashboard` · announcements · system settings · health |
| Enterprise | API keys, webhooks, custom domain + verify, backups, status page |
| Reports | `GET /reports/restaurants/{id}/export` |
</details>

---

## 🗃️ Data Model (core)

```
Restaurant 1─┬─* Branch
             ├─* Category ──* MenuItem          (price, food type, flags, macros, allergens)
             ├─* DiningTable ─1 QrCode          (UUID token, label, scan count)
             ├─* Order ─* OrderItem             (order #, table, guest mobile, status, total)
             ├─* User                          (SUPER_ADMIN | RESTAURANT_OWNER | MANAGER | STAFF)
             ├─* Subscription / Coupon / Invoice
             ├─* Offer · ChatMessage · Notification
             ├─* SupportTicket ─* TicketMessage
             └─* ScanEvent · SearchLog · AuditLog · MediaAsset
```

Every entity extends `BaseEntity` → **soft deletes** (`isDeleted`) + audit timestamps. Order lifecycle: `PENDING → ACCEPTED → PREPARING → READY → COMPLETED | DELIVERED | CANCELLED`.

---

## 🔒 Security

- **Stateless JWT** — 15-minute access token + 7-day refresh; frontend auto-refreshes on 401 and retries.
- **RBAC** — route-level roles (`SUPER_ADMIN`, `RESTAURANT_OWNER`, `MANAGER`, `STAFF`) + fine-grained `Permission` sets per role.
- **Hardening** — BCrypt passwords, Bucket4j rate limiting, security-headers filter, CORS locked to the frontend origin, actuator limited to `health,info,metrics`.
- **Caveats for production** — set a strong `JWT_SECRET`, switch Hibernate to `ddl-auto: validate`, and lock down the currently public GET routes on `/restaurants/**` and `/analytics/**` if needed.

---

## 🏃 Scripts

| Location | Command | Action |
|---|---|---|
| backend | `mvn spring-boot:run` | Start API on `:8080` |
| backend | `mvn clean package` | Build runnable JAR |
| backend | `mvn test` | Run test suite |
| frontend | `npm run start` | Dev server on `:4200` |
| frontend | `npm run build` | Production build |
| frontend | `npm test` | Vitest unit tests |

---

## 🗺️ Roadmap

From the original product roadmap, current status:

| Phase | Area | Status |
|---|---|---|
| 1–3 | Auth, roles, schema, multi-tenant foundation | ✅ Done |
| 4 | Menu / category / QR / order / analytics APIs | ✅ Done |
| 5 | Admin panel (owner + super-admin dashboards) | ✅ Done |
| 6 | Customer menu website (search, filters, dark mode) | ✅ Done |
| 7 | QR generation & scan flow (ZXing + Cloudinary) | ✅ Done |
| 8 | Image storage via Cloudinary (URLs only in DB) | ✅ Done |
| 9 | Security hardening (JWT, rate limits, headers) | ✅ Done |
| 10 | SaaS subscriptions & plans | ✅ API done · ⏳ payment gateway (Razorpay/PayPal) pending |
| — | Multi-language UI | ⏳ Placeholder UI only |
| — | WebSocket push (replace polling) | ⏳ Planned (currently 2.5 s polling) |
| — | Docker Compose deployment | ⏳ Planned |

---

<div align="center">

**Built with 🧡 as a full-stack SaaS reference implementation.**

`Scan → Browse → Order → Cook → Serve` — the entire restaurant journey in one platform.

</div>
