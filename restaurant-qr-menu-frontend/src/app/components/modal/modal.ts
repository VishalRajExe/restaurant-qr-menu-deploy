import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalService } from '../../services/modal.service';

@Component({
  selector: 'app-confirm-modal',
  imports: [CommonModule],
  template: `
    @if (modalService.activeModal(); as config) {
      <div class="lez-overlay" (click)="onOverlayClick($event)">
        <div class="lez-modal" role="dialog" aria-modal="true">
          
          <div style="display:flex;align-items:flex-start;gap:16px;margin-bottom:20px">
            <div style="width:44px;height:44px;border-radius:12px;display:flex;align-items:center;justify-content:center;flex-shrink:0"
                 [style.background]="config.type === 'danger' ? '#fee2e2' : config.type === 'warning' ? '#fef3c7' : '#ffdbd0'"
                 [style.color]="config.type === 'danger' ? '#b91c1c' : config.type === 'warning' ? '#b45309' : '#ab3500'">
              @if (config.type === 'danger') {
                <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                </svg>
              } @else if (config.type === 'warning') {
                <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/><path stroke-linecap="round" d="M12 8v4m0 4h.01"/>
                </svg>
              } @else {
                <svg width="22" height="22" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/><path stroke-linecap="round" d="M12 16v-4m0-4h.01"/>
                </svg>
              }
            </div>

            <div>
              <h3 style="font-size:18px;font-weight:700;color:#0b1c30;margin:0 0 6px 0;">{{ config.title }}</h3>
              <p style="font-size:13.5px;color:#594139;line-height:1.5;margin:0;">{{ config.message }}</p>
            </div>
          </div>

          <div style="display:flex;justify-content:flex-end;gap:10px;padding-top:16px;border-top:1px solid #e8ecf4">
            <button class="lez-btn lez-btn-secondary" type="button" (click)="modalService.close()">
              {{ config.cancelText || 'Cancel' }}
            </button>
            <button class="lez-btn" type="button" (click)="modalService.proceed()"
                    [style.background]="config.type === 'danger' ? '#b91c1c' : '#ff6b35'"
                    style="color:#ffffff">
              {{ config.confirmText || 'Confirm' }}
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ConfirmModal {
  modalService = inject(ModalService);

  onOverlayClick(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('lez-overlay')) {
      this.modalService.close();
    }
  }
}
