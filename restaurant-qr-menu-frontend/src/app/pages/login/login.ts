import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

import { BackButton } from '../../components/back-button/back-button';

@Component({
  selector: 'app-login',
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class Login implements OnInit {
  authService = inject(AuthService);
  router      = inject(Router);

  // Switcher role tab
  activeTab = signal<'owner' | 'chef' | 'super-admin'>('owner');

  // Input states
  email      = signal<string>('owner@example.com');
  password   = signal<string>('Password123!');
  rememberMe = signal<boolean>(true);

  isLoading    = signal<boolean>(false);
  errorMessage = signal<string>('');

  private readonly roleRoutes: Record<string, string> = {
    'owner':       '/dashboard/owner',
    'chef':        '/dashboard/chef',
    'super-admin': '/dashboard/admin',
  };

  ngOnInit() {
    // Clear stale session on navigating to login page if user explicitly came to login
    const session = this.authService.currentUser();
    if (session && session.role !== 'owner') {
      // Allow session reset when on login page
    }
  }

  setRoleTab(role: 'owner' | 'chef' | 'super-admin') {
    this.activeTab.set(role);
    this.errorMessage.set('');
    this.authService.logout();

    if (role === 'owner') {
      this.email.set('owner@example.com');
      this.password.set('Password123!');
    } else if (role === 'chef') {
      this.email.set('chef@jardinprovence.fr');
      this.password.set('Password123!');
    } else {
      this.email.set('admin@restaurantqr.com');
      this.password.set('Admin@12345');
    }
  }

  onSubmit() {
    this.isLoading.set(true);
    this.errorMessage.set('');

    const expectedRole = this.activeTab();

    // Clear any previous session before performing new login
    this.authService.logout();

    this.authService.login(this.email(), this.password(), expectedRole).subscribe({
      next: (success: boolean) => {
        this.isLoading.set(false);
        if (success) {
          const role = this.authService.currentUser()?.role ?? expectedRole;
          const targetPath = this.roleRoutes[role] || '/dashboard/owner';
          this.router.navigateByUrl(targetPath);
        } else {
          this.errorMessage.set('Invalid credentials. Please verify email and password.');
        }
      },
      error: (err: { message: string }) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.message || 'Login failed. Please check backend connection.');
      }
    });
  }
}
