# API_INVENTORY.md — Complete Endpoint Inventory
**Generated:** Phase 0 Audit — 2026-07-20  
**Context path:** `/api/v1`  
**Total endpoints discovered:** 68

---

## 1. Authentication (`AuthController`) — `/auth/**` — PUBLIC

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| POST | `/auth/login` | None | — | Returns JWT access + refresh token |
| POST | `/auth/register` | None | — | Public self-registration → hardcoded STAFF role |
| POST | `/auth/refresh` | None | — | Refresh access token via refresh token |
| POST | `/auth/forgot-password` | None | — | Sends reset email (always 200) |
| POST | `/auth/reset-password` | None | — | Reset password via token |
| POST | `/auth/change-password` | JWT | Any | Requires current password |

---

## 2. Users (`UserController`) — `/restaurants/{restaurantId}/users`

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/restaurants/{restaurantId}/users` | JWT | OWNER, SUPER_ADMIN | Paginated list |
| GET | `/restaurants/{restaurantId}/users/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Get by ID |
| POST | `/restaurants/{restaurantId}/users` | JWT | OWNER, SUPER_ADMIN | Create staff user |
| PUT | `/restaurants/{restaurantId}/users/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Update profile |
| PATCH | `/restaurants/{restaurantId}/users/{id}/toggle-status` | JWT | OWNER, SUPER_ADMIN | Toggle active/inactive |
| DELETE | `/restaurants/{restaurantId}/users/{id}` | JWT | OWNER, SUPER_ADMIN | Soft delete |

---

## 3. Restaurants (`RestaurantController`) — `/restaurants`

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/restaurants/slug/{slug}` | None | — | Public — customer menu page |
| GET | `/restaurants/{id}` | None* | — | Public GET but assertRestaurantAccess blocks cross-tenant |
| GET | `/restaurants` | JWT | SUPER_ADMIN | Paginated list |
| POST | `/restaurants` | JWT | SUPER_ADMIN | Create restaurant |
| PUT | `/restaurants/{id}` | JWT | SUPER_ADMIN, OWNER | Update |
| DELETE | `/restaurants/{id}` | JWT | SUPER_ADMIN | Soft delete |

---

## 4. Branches (`BranchController`) — `/restaurants/{restaurantId}/branches`

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/restaurants/{restaurantId}/branches` | JWT | OWNER, MANAGER, STAFF, SUPER_ADMIN | List branches |
| GET | `/restaurants/{restaurantId}/branches/{id}` | JWT | OWNER, MANAGER, STAFF, SUPER_ADMIN | Get branch |
| POST | `/restaurants/{restaurantId}/branches` | JWT | OWNER, SUPER_ADMIN | Create (limit checked) |
| PUT | `/restaurants/{restaurantId}/branches/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Update |
| DELETE | `/restaurants/{restaurantId}/branches/{id}` | JWT | OWNER, SUPER_ADMIN | Soft delete |

---

## 5. Categories (`CategoryController`) — `/restaurants/{restaurantId}/categories`

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/restaurants/{restaurantId}/categories/active` | None | — | Public — customer menu |
| GET | `/restaurants/{restaurantId}/categories` | JWT | OWNER, MANAGER, STAFF, SUPER_ADMIN | All categories |
| GET | `/restaurants/{restaurantId}/categories/{id}` | JWT | OWNER, MANAGER, STAFF, SUPER_ADMIN | Get by ID |
| POST | `/restaurants/{restaurantId}/categories` | JWT | OWNER, MANAGER, SUPER_ADMIN | Create |
| PUT | `/restaurants/{restaurantId}/categories/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Update |
| PUT | `/restaurants/{restaurantId}/categories/reorder` | JWT | OWNER, MANAGER, SUPER_ADMIN | Drag-and-drop reorder |
| PATCH | `/restaurants/{restaurantId}/categories/{id}/toggle-status` | JWT | OWNER, MANAGER, SUPER_ADMIN | Toggle active |
| POST | `/restaurants/{restaurantId}/categories/{id}/image` | JWT | OWNER, MANAGER, SUPER_ADMIN | Upload image |
| DELETE | `/restaurants/{restaurantId}/categories/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Soft delete |

---

## 6. Menu Items (`MenuItemController`) — Mixed base paths

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/public/restaurants/{restaurantId}/menu` | None | — | Public menu |
| GET | `/public/restaurants/{restaurantId}/menu/search` | None | — | Public search |
| GET | `/public/restaurants/{restaurantId}/menu/featured` | None | — | Public featured items |
| GET | `/restaurants/{restaurantId}/menu-items/category/{categoryId}` | JWT | OWNER, MANAGER, STAFF, SUPER_ADMIN | By category |
| POST | `/restaurants/{restaurantId}/menu-items` | JWT | OWNER, MANAGER, SUPER_ADMIN | Create (limit checked) |
| PUT | `/restaurants/{restaurantId}/menu-items/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Update |
| PATCH | `/restaurants/{restaurantId}/menu-items/{id}/availability` | JWT | OWNER, MANAGER, STAFF, SUPER_ADMIN | Toggle availability |
| DELETE | `/restaurants/{restaurantId}/menu-items/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Delete |

---

## 7. Offers (`OfferController`) — Mixed base paths

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/public/restaurants/{restaurantId}/offers` | None | — | Active offers only |
| GET | `/restaurants/{restaurantId}/offers` | JWT | OWNER, MANAGER, STAFF, SUPER_ADMIN | All offers |
| POST | `/restaurants/{restaurantId}/offers` | JWT | OWNER, MANAGER, SUPER_ADMIN | Create |
| PUT | `/restaurants/{restaurantId}/offers/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Update |
| POST | `/restaurants/{restaurantId}/offers/{id}/banner` | JWT | OWNER, MANAGER, SUPER_ADMIN | Upload banner |
| DELETE | `/restaurants/{restaurantId}/offers/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Delete |

---

## 8. QR Codes (`QrCodeController`) — Mixed base paths

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/public/qr/{token}` | None | — | Public QR resolve |
| POST | `/restaurants/{restaurantId}/qr-codes` | JWT | OWNER, MANAGER, SUPER_ADMIN | Generate QR |
| GET | `/restaurants/{restaurantId}/qr-codes` | JWT | OWNER, MANAGER, STAFF, SUPER_ADMIN | List |
| PATCH | `/restaurants/{restaurantId}/qr-codes/{id}/deactivate` | JWT | OWNER, MANAGER, SUPER_ADMIN | Deactivate |
| DELETE | `/restaurants/{restaurantId}/qr-codes/{id}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Delete |

---

## 9. Public Menu (`PublicMenuController`) — `/public/menu`

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/public/menu/{token}` | None | — | Full menu payload by QR token |
| GET | `/public/menu/restaurant/{slug}` | None | — | Full menu payload by slug |

---

## 10. Image Upload (`ImageUploadController`) — `/upload`

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| POST | `/upload/menu-items/{restaurantId}/{itemId}` | JWT | OWNER, MANAGER, SUPER_ADMIN | Upload menu item image |
| POST | `/upload/restaurants/{restaurantId}/logo` | JWT | OWNER, SUPER_ADMIN | Upload restaurant logo |
| POST | `/upload/restaurants/{restaurantId}/banner` | JWT | OWNER, SUPER_ADMIN | Upload restaurant banner |

---

## 11. Analytics (`AnalyticsController`) — `/analytics`

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/analytics/restaurants/{restaurantId}/dashboard` | JWT | OWNER, MANAGER, SUPER_ADMIN | Dashboard stats |

---

## 12. Subscriptions (`SubscriptionController`) — `/subscriptions`

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/subscriptions/plans` | None | — | Public plan details |
| GET | `/subscriptions/restaurants/{restaurantId}/active` | JWT | OWNER, SUPER_ADMIN | Active subscription |
| GET | `/subscriptions/restaurants/{restaurantId}/history` | JWT | OWNER, SUPER_ADMIN | Subscription history |
| POST | `/subscriptions/restaurants/{restaurantId}/activate` | JWT | OWNER, SUPER_ADMIN | Activate subscription |
| POST | `/subscriptions/restaurants/{restaurantId}/cancel` | JWT | OWNER, SUPER_ADMIN | Cancel subscription |

---

## 13. Super Admin (`SuperAdminController`) — `/super-admin`

| Method | Path | Auth | Roles | Notes |
|--------|------|------|-------|-------|
| GET | `/super-admin/stats` | JWT | SUPER_ADMIN | Platform dashboard stats |
| GET | `/super-admin/restaurants` | JWT | SUPER_ADMIN | All restaurants |
| PATCH | `/super-admin/restaurants/{id}/status` | JWT | SUPER_ADMIN | Suspend/activate restaurant |
| GET | `/super-admin/users` | JWT | SUPER_ADMIN | All platform users |
| POST | `/super-admin/restaurants/{restaurantId}/owner` | JWT | SUPER_ADMIN | Create restaurant owner account |
| GET | `/super-admin/subscriptions/expiring-soon` | JWT | SUPER_ADMIN | Expiring subscriptions |

---

## Summary

| Module | Total | Public | Auth-required |
|--------|-------|--------|---------------|
| Auth | 6 | 5 | 1 |
| Users | 6 | 0 | 6 |
| Restaurants | 6 | 2 | 4 |
| Branches | 5 | 0 | 5 |
| Categories | 9 | 1 | 8 |
| Menu Items | 8 | 3 | 5 |
| Offers | 6 | 1 | 5 |
| QR Codes | 5 | 1 | 4 |
| Public Menu | 2 | 2 | 0 |
| Image Upload | 3 | 0 | 3 |
| Analytics | 1 | 0 | 1 |
| Subscriptions | 5 | 1 | 4 |
| Super Admin | 6 | 0 | 6 |
| **TOTAL** | **68** | **16** | **52** |

> Note: Prepend `/api/v1` to all paths.
