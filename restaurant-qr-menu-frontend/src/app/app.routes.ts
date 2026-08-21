import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './services/auth.service';

import { Landing }        from './pages/landing/landing';
import { Login }          from './pages/login/login';
import { Signup }         from './pages/signup/signup';
import { OwnerDashboard } from './pages/owner-dashboard/owner-dashboard';
import { ChefDashboard }  from './pages/chef-dashboard/chef-dashboard';
import { AdminDashboard } from './pages/admin-dashboard/admin-dashboard';
import { CustomerMenu }   from './pages/customer-menu/customer-menu';
import { NotFound }       from './pages/not-found/not-found';
import { ServerError }    from './pages/server-error/server-error';

// ── Guards ─────────────────────────────────────────────────────────────────

const authGuard = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  if (auth.isLoggedIn()) return true;
  return router.createUrlTree(['/login']);
};

const ownerGuard = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) return router.createUrlTree(['/login']);
  if (auth.currentUser()?.role === 'owner') return true;
  return router.createUrlTree([getDashboardRoute(auth.currentUser()?.role)]);
};

const chefGuard = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) return router.createUrlTree(['/login']);
  if (auth.currentUser()?.role === 'chef') return true;
  return router.createUrlTree([getDashboardRoute(auth.currentUser()?.role)]);
};

const adminGuard = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) return router.createUrlTree(['/login']);
  if (auth.currentUser()?.role === 'super-admin') return true;
  return router.createUrlTree([getDashboardRoute(auth.currentUser()?.role)]);
};

function getDashboardRoute(role: string | undefined): string {
  if (role === 'owner')       return '/dashboard/owner';
  if (role === 'chef')        return '/dashboard/chef';
  if (role === 'super-admin') return '/dashboard/admin';
  return '/login';
}

// ── Smart /dashboard redirect component ────────────────────────────────────
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-dashboard-redirect',
  standalone: true,
  template: '',
})
class DashboardRedirect implements OnInit {
  auth   = inject(AuthService);
  router = inject(Router);

  ngOnInit() {
    this.router.navigate([getDashboardRoute(this.auth.currentUser()?.role)]);
  }
}

// ── Routes ─────────────────────────────────────────────────────────────────
export const routes: Routes = [
  // Public
  { path: '',       component: Landing },
  { path: 'login',  component: Login },
  { path: 'signup', component: Signup },

  // Guest Digital Menu — no login needed (supports ID, QR token, slug, table, and direct /menu)
  { path: 'menu', component: CustomerMenu },
  { path: 'menu/:restaurantId', component: CustomerMenu },
  { path: 'restaurant/:restaurantId', component: CustomerMenu },
  { path: 'r/:restaurantId', component: CustomerMenu },
  { path: 'table/:tableNumber', component: CustomerMenu },
  { path: 't/:tableNumber', component: CustomerMenu },
  { path: 'scan/:restaurantId', component: CustomerMenu },
  { path: 'scan', component: CustomerMenu },

  // /dashboard → redirects to correct role sub-route
  {
    path: 'dashboard',
    component: DashboardRedirect,
    canActivate: [authGuard],
  },

  // Role-specific dashboards
  {
    path: 'dashboard/owner',
    component: OwnerDashboard,
    canActivate: [ownerGuard],
  },
  {
    path: 'dashboard/chef',
    component: ChefDashboard,
    canActivate: [chefGuard],
  },
  {
    path: 'dashboard/admin',
    component: AdminDashboard,
    canActivate: [adminGuard],
  },

  // Error pages
  { path: '500', component: ServerError },
  { path: '404', component: NotFound },

  // Catch-all 404
  { path: '**', component: NotFound },
];