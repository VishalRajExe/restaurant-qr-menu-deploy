import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastMessage } from '../../services/toast.service';

@Component({
  selector: 'app-toast-container',
  imports: [CommonModule],
  template: `
    <div class="lez-toast-container">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="lez-toast"
             [class.success]="toast.type === 'success'"
             [class.error]="toast.type === 'error'"
             [class.warning]="toast.type === 'warning'"
             [class.info]="toast.type === 'info'">
          
          <!-- Icon -->
          <div style="flex-shrink:0;display:flex;align-items:center;justify-content:center;width:24px;height:24px">
            @if (toast.type === 'success') {
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" style="color:#10B981">
                <circle cx="10" cy="10" r="9" stroke="currentColor" stroke-width="1.8"/>
                <path d="M6 10l3 3 5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            } @else if (toast.type === 'error') {
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" style="color:#EF4444">
                <circle cx="10" cy="10" r="9" stroke="currentColor" stroke-width="1.8"/>
                <path d="M7 7l6 6M13 7l-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            } @else if (toast.type === 'warning') {
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" style="color:#F59E0B">
                <path d="M10 2L18 17H2L10 2z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                <path d="M10 8v4M10 14v.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            } @else {
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" style="color:#3B82F6">
                <circle cx="10" cy="10" r="9" stroke="currentColor" stroke-width="1.8"/>
                <path d="M10 9v5M10 6.5v.5" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            }
          </div>

          <!-- Message Body -->
          <div style="flex:1;min-width:0">
            @if (toast.title) {
              <div style="font-size:13px;font-weight:700;color:#0b1c30;line-height:1.3">{{ toast.title }}</div>
            }
            @if (toast.message) {
              <div style="font-size:12px;color:#594139;margin-top:2px">{{ toast.message }}</div>
            }
          </div>

          <!-- Close button -->
          <button (click)="toastService.remove(toast.id)" type="button"
                  style="border:none;background:transparent;cursor:pointer;color:#8c9ba5;padding:4px">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M1 1l10 10M11 1L1 11" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      }
    </div>
  `
})
export class ToastContainer {
  toastService = inject(ToastService);
}
