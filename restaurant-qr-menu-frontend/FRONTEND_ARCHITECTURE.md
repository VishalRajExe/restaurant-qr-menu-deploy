# AuraMenu — Frontend Architecture
> Angular 19 · Standalone Components · Tailwind CSS · Signals

---

## Actual Folder Structure

```
src/app/
├── pages/
│   ├── landing/                  # Public homepage
│   ├── login/                    # Login (owner / chef / super-admin tabs)
│   ├── signup/                   # Signup (multi-step for owner, single for chef/admin)
│   ├── dashboard/                # Owner dashboard (categories, dishes, QR studio)
│   ├── chef-dashboard/           # Chef order queue (pending → preparing → done)
│   ├── admin-dashboard/          # Super admin (all restaurants table)
│   └── customer-menu/            # Public guest menu (no login required)
│
├── services/
│   ├── auth.service.ts           # Login, signup, JWT storage, currentUser signal
│   ├── restaurant.service.ts     # Restaurant profile data
│   ├── category.service.ts       # Category CRUD
│   ├── menu.service.ts           # Menu items CRUD
│   ├── order.service.ts          # Place order, get orders, update status  ← TO BUILD
│   └── theme.ts                  # Dark/light theme toggle
│
├── models/
│   ├── restaurant.model.ts
│   ├── category.model.ts
│   ├── menu-item.model.ts
│   ├── pricing-plan.model.ts
│   ├── testimonial.model.ts
│   └── offer.model.ts
│
├── mock-data/                    # Temporary mock data (replaced by API calls in Phase 2+)
│   ├── landing.data.ts
│   ├── dashboard.data.ts
│   ├── restaurant.data.ts
│   └── orders.data.ts
│
├── guards/                       # Route protection           ← TO CREATE
│   ├── auth.guard.ts             # Redirect to /login if not logged in
│   └── role.guard.ts             # Redirect to correct dashboard by role
│
├── interceptors/                 # HTTP pipeline             ← TO CREATE on Phase 1
│   ├── jwt.interceptor.ts        # Attach Bearer token to every API request
│   └── error.interceptor.ts     # Handle 401 → redirect to login, 500 → show toast
│
├── app.routes.ts                 # All routes with role guards
├── app.config.ts                 # Angular providers (router, http)
└── app.ts                        # Root component
```

---

## Routes Map

```
/                        → Landing (public)
/login                   → Login page (3 role tabs)
/signup                  → Signup page (3 role tabs)
/dashboard/owner         → Owner Dashboard    [requires JWT + role: owner]
/dashboard/chef          → Chef Dashboard     [requires JWT + role: chef]
/dashboard/admin         → Super Admin        [requires JWT + role: super-admin]
/menu/:restaurantSlug    → Guest Menu         [public, no auth]
/menu/:restaurantSlug?table=4  → Guest Menu with table context
```

---

## State Management

No NgRx. Using Angular Signals throughout:

```typescript
// Example pattern used everywhere
currentUser = signal<UserSession | null>(null);
isLoggedIn  = signal<boolean>(false);

categories  = signal<Category[]>([]);
menuItems   = signal<MenuItem[]>([]);
orders      = signal<Order[]>([]);
```

Computed values derived automatically:
```typescript
pendingOrders  = computed(() => this.orders().filter(o => o.status === 'pending'));
totalItems     = computed(() => this.menuItems().length);
```

---

## API Integration Plan (Frontend side)

### Phase 1 — Auth (blocks everything else)

```typescript
// auth.service.ts changes:

// BEFORE (mock):
login(email: string, role: string): boolean {
  this.currentUser.set({ ...mockUser });
  return true;
}

// AFTER (real):
login(email: string, password: string): Observable<void> {
  return this.http.post<{ token: string; user: UserSession }>('/api/auth/login', { email, password })
    .pipe(tap(res => {
      localStorage.setItem('jwt', res.token);
      this.currentUser.set(res.user);
      this.isLoggedIn.set(true);
    }));
}
```

JWT interceptor attaches token automatically:
```typescript
// interceptors/jwt.interceptor.ts
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('jwt');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
```

### Phase 2 — Owner Dashboard Data

```typescript
// category.service.ts
getCategories(): Observable<Category[]> {
  return this.http.get<Category[]>('/api/categories');
}
addCategory(data): Observable<Category> {
  return this.http.post<Category>('/api/categories', data);
}
deleteCategory(id: string): Observable<void> {
  return this.http.delete<void>(`/api/categories/${id}`);
}

// menu.service.ts
getMenuItems(): Observable<MenuItem[]> {
  return this.http.get<MenuItem[]>('/api/menu-items');
}
addMenuItem(data): Observable<MenuItem> {
  return this.http.post<MenuItem>('/api/menu-items', data);
}
toggleAvailability(id: string, isAvailable: boolean): Observable<MenuItem> {
  return this.http.put<MenuItem>(`/api/menu-items/${id}`, { isAvailable });
}
deleteMenuItem(id: string): Observable<void> {
  return this.http.delete<void>(`/api/menu-items/${id}`);
}
```

### Phase 3 — Guest Cart + Order

```typescript
// order.service.ts
placeOrder(order: { restaurantSlug: string; tableNumber: number; items: OrderItem[] }): Observable<Order> {
  return this.http.post<Order>('/api/public/orders', order);
}
```

### Phase 4 — Chef Dashboard Live Orders

```typescript
// order.service.ts (chef side)
getOrders(): Observable<Order[]> {
  return this.http.get<Order[]>('/api/orders');
}
updateOrderStatus(id: string, status: 'preparing' | 'done'): Observable<Order> {
  return this.http.put<Order>(`/api/orders/${id}/status`, { status });
}

// chef-dashboard.ts — poll every 10 seconds
ngOnInit() {
  this.loadOrders();
  setInterval(() => this.loadOrders(), 10000);
}
loadOrders() {
  this.orderService.getOrders().subscribe(orders => this.orders.set(orders));
}
```

### Phase 5 — Super Admin

```typescript
// admin-dashboard.ts
this.http.get<Restaurant[]>('/api/admin/restaurants')
  .subscribe(data => this.restaurants.set(data));
```

---

## APIs We Actually Need (14 of the 35)

| # | Method | Endpoint | Used By |
|---|--------|----------|---------|
| 1 | POST | `/api/auth/register` | Signup |
| 2 | POST | `/api/auth/login` | Login |
| 3 | POST | `/api/auth/refresh` | JWT interceptor |
| 4 | GET  | `/api/restaurants/me` | Owner dashboard header |
| 5 | PUT  | `/api/restaurants/me` | Owner settings |
| 6 | GET  | `/api/categories` | Owner dashboard |
| 7 | POST | `/api/categories` | Add category |
| 8 | DELETE | `/api/categories/{id}` | Delete category |
| 9 | GET  | `/api/menu-items` | Owner dashboard + customer menu |
| 10 | POST | `/api/menu-items` | Add dish |
| 11 | PUT  | `/api/menu-items/{id}` | Edit / toggle availability |
| 12 | DELETE | `/api/menu-items/{id}` | Delete dish |
| 13 | GET  | `/api/public/restaurants/{slug}/menu` | Guest menu page |
| 14 | POST | `/api/public/orders` | Guest places order |
| 15 | GET  | `/api/orders` | Chef dashboard |
| 16 | PUT  | `/api/orders/{id}/status` | Chef marks status |
| 17 | GET  | `/api/admin/restaurants` | Super admin table |
| 18 | GET  | `/api/admin/stats` | Super admin stat cards |

**Skip for MVP:** branches, offers, subscriptions, analytics scan events,
password reset, email verification, QR metadata endpoints.

---

## Image Upload Strategy

```
Owner uploads dish photo
  ↓
Frontend converts to base64 preview (already built)
  ↓
On form submit → POST /api/menu-items with multipart/form-data
  OR
Frontend uploads to Cloudinary directly (simpler)
  → gets back a URL string
  → sends URL to POST /api/menu-items as imageUrl field
```

Cloudinary direct upload is recommended — backend never touches binary files.

---

## What Backend Teammate Needs to Know

1. Frontend uses `restaurantSlug` (string like "le-jardin") not numeric `restaurantId` in public URLs
2. JWT payload must include: `{ id, email, role, name, restaurantId }`
3. All protected endpoints derive `restaurantId` from JWT — frontend never sends it in body
4. Orders need a `tableNumber` field (integer, from URL query param `?table=4`)
5. Public endpoints (`/api/public/*`) must allow CORS from `localhost:4200` in dev
6. Chef role needs `GET /api/orders` scoped to their `restaurantId` from JWT
7. Image storage: prefer Cloudinary direct upload from frontend, backend stores the URL string only
