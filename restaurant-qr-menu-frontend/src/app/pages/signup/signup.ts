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

  // Step 1: Chef Info
  chefName = signal<string>('');
  chefEmail = signal<string>('');
  chefPassword = signal<string>('');
  restaurantCode = signal<string>('');

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
        if (!this.chefName() || !this.chefEmail() || !this.chefPassword()) {
          this.errorMessage.set('Please fill out all your details.');
          return;
        }
      } else {
        if (!this.ownerName() || !this.ownerEmail() || !this.ownerPassword()) {
          this.errorMessage.set('Please fill out all owner details before proceeding.');
          return;
        }
      }
    } else if (this.currentStep() === 2) {
      if (!this.restaurantName() || !this.venueAddress()) {
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
        name: this.ownerName(),
        email: this.ownerEmail(),
        password: this.ownerPassword(),
        restaurantName: this.restaurantName(),
        planId: this.selectedPlanId(),
        photo: this.restaurantPhoto()
      });
    } else if (this.activePortal() === 'chef') {
      action$ = this.authService.signupChef({
        name: this.chefName(),
        email: this.chefEmail(),
        restaurantName: this.restaurantCode(),
        planId: 'chef-free'
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
        this.currentStep.set(4);
      },
      error: (err: any) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.message || 'Registration failed.');
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
