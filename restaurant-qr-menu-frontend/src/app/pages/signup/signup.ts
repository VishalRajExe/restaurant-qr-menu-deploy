import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { PRICING_PLANS } from '../../mock-data/landing.data';
import { PricingPlan } from '../../models/pricing-plan.model';

@Component({
  selector: 'app-signup',
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './signup.html',
  styleUrls: ['./signup.css']
})
export class Signup {
  authService = inject(AuthService);
  router = inject(Router);

  // Type Selector: 'owner' | 'chef' | 'super-admin'
  activePortal = signal<'owner' | 'chef' | 'super-admin'>('owner');

  // Multi-step index for Owner: 1, 2, 3, 4
  currentStep = signal<number>(1);

  // Step 1: Owner Info
  ownerName = signal<string>('');
  ownerEmail = signal<string>('');
  ownerPassword = signal<string>('');
  showOwnerPassword = signal<boolean>(false);
  ownerPhone = signal<string>('');

  // Step 1: Chef Info
  chefName = signal<string>('');
  chefEmail = signal<string>('');
  chefPassword = signal<string>('');
  showChefPassword = signal<boolean>(false);
  chefPhone = signal<string>('');
  chefInviteCode = signal<string>('');

  // Step 2: Restaurant Info
  restaurantName = signal<string>('');
  cuisineType = signal<string>('Modern French Fine Dining');
  venueAddress = signal<string>('');
  venuePhone = signal<string>('');
  gstin = signal<string>('27AAPFU0939F1ZV');
  restaurantPhoto = signal<string>('');

  // Step 3: Plan Selection
  plans: PricingPlan[] = PRICING_PLANS;
  selectedPlanId = signal<string>('p2');

  // Form states
  isLoading = signal<boolean>(false);
  errorMessage = signal<string>('');

  setPortal(portal: 'owner' | 'chef' | 'super-admin') {
    this.activePortal.set(portal);
    this.currentStep.set(1);
    this.errorMessage.set('');
  }

  nextStep() {
    this.errorMessage.set('');

    if (this.currentStep() === 1) {
      if (this.activePortal() === 'chef') {
        if (!this.chefName().trim() || !this.chefEmail().trim() || !this.chefPassword().trim()) {
          this.errorMessage.set('Please fill out all chef fields.');
          return;
        }
        if (!this.chefInviteCode().trim()) {
          this.errorMessage.set('Please enter your Restaurant Chef Registration Code (e.g. CHEF-REST01).');
          return;
        }
        const phone = this.chefPhone().trim().replace(/\D/g, '');
        if (phone && phone.length !== 10) {
          this.errorMessage.set('Phone number must be exactly 10 digits.');
          return;
        }
      } else {
        if (!this.ownerName().trim() || !this.ownerEmail().trim() || !this.ownerPassword().trim()) {
          this.errorMessage.set('Please fill out all owner details before proceeding.');
          return;
        }
        const cleanPhone = this.ownerPhone().trim().replace(/\D/g, '');
        if (!cleanPhone || cleanPhone.length !== 10) {
          this.errorMessage.set('Owner phone number is required and must be exactly 10 digits.');
          return;
        }
      }
    } else if (this.currentStep() === 2) {
      if (!this.restaurantName().trim() || !this.venueAddress().trim()) {
        this.errorMessage.set('Please input restaurant name and operational address.');
        return;
      }
    }

    this.currentStep.update(n => n + 1);
  }

  prevStep() {
    this.currentStep.update(n => n - 1);
    this.errorMessage.set('');
  }

  selectPlan(planId: string) {
    this.selectedPlanId.set(planId);
  }

  handleRegister() {
    this.onRegister();
  }

  handleChefRegister() {
    if (!this.chefName().trim() || !this.chefEmail().trim() || !this.chefPassword().trim()) {
      this.errorMessage.set('Please fill out all chef fields.');
      return;
    }
    if (!this.chefInviteCode().trim()) {
      this.errorMessage.set('Please enter your Restaurant Chef Registration Code (e.g. CHEF-REST01).');
      return;
    }
    this.onRegister();
  }

  goToOwnerDashboard() {
    this.enterDashboard();
  }

  onRegister() {
    this.isLoading.set(true);
    this.errorMessage.set('');

    let action$: any;

    if (this.activePortal() === 'owner') {
      action$ = this.authService.signupOwner({
        name: this.ownerName().trim(),
        email: this.ownerEmail().trim(),
        password: this.ownerPassword(),
        phone: this.ownerPhone().trim().replace(/\D/g, ''),
        restaurantName: this.restaurantName().trim(),
        restaurantAddress: this.venueAddress().trim(),
        planId: this.selectedPlanId(),
        photo: this.restaurantPhoto()
      });
    } else if (this.activePortal() === 'chef') {
      action$ = this.authService.signupChef({
        name: this.chefName().trim(),
        email: this.chefEmail().trim(),
        password: this.chefPassword(),
        phone: this.chefPhone().trim().replace(/\D/g, '') || '9876543210',
        chefInviteCode: this.chefInviteCode().trim().toUpperCase()
      });
    } else {
      action$ = this.authService.signupSuperAdmin({
        name: this.ownerName() || 'Admin Operator',
        email: this.ownerEmail() || 'admin@restaurantqr.com'
      });
    }

    action$.subscribe({
      next: () => {
        this.isLoading.set(false);
        if (this.activePortal() === 'chef') {
          this.router.navigate(['/dashboard/chef']);
        } else {
          this.currentStep.set(4);
        }
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.message || 'Registration failed. Please verify credentials.');
      }
    });
  }

  enterDashboard() {
    const role = this.authService.currentUser()?.role;
    if (role === 'owner') {
      this.router.navigate(['/dashboard/owner']);
    } else if (role === 'chef') {
      this.router.navigate(['/dashboard/chef']);
    } else {
      this.router.navigate(['/dashboard/admin']);
    }
  }

  handleRestaurantPhotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      this.errorMessage.set('Please select a valid image file.');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage.set('Image size should not exceed 5MB.');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.restaurantPhoto.set(reader.result as string);
    };
    reader.readAsDataURL(file);
  }

  clearRestaurantPhoto() {
    this.restaurantPhoto.set('');
  }
}
