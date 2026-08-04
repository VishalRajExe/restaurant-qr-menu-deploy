import { Component, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  imports: [CommonModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.css']
})
export class Navbar {
  authService = inject(AuthService);
  router = inject(Router);

  isMobileMenuOpen = signal<boolean>(false);
  
  // Theme management signal - defaults to dark mode
  isDarkMode = signal<boolean>(true);

  constructor() {
    // Sync theme class wrapper with document element
    effect(() => {
      const dark = this.isDarkMode();
      const body = document.body;
      if (dark) {
        body.classList.add('dark');
        body.classList.remove('light');
        body.style.backgroundColor = '#0b0c0e';
        body.style.color = '#f3f4f6';
      } else {
        body.classList.add('light');
        body.classList.remove('dark');
        body.style.backgroundColor = '#fafafa';
        body.style.color = '#1f2937';
      }
    });
  }

  toggleMobileMenu() {
    this.isMobileMenuOpen.update(val => !val);
  }

  toggleTheme() {
    this.isDarkMode.update(val => !val);
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
