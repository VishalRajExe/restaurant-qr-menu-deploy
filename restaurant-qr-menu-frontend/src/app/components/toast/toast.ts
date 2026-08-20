import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastMessage } from '../../services/toast.service';
import { UndoService } from '../../services/undo.service';

@Component({
  selector: 'app-toast-container',
  imports: [CommonModule],
  template: `
    <!-- Top-Right Standard Toasts -->
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

    <!-- ── Global Bottom Undo Snackbar ── -->
    @if (undoService.activeUndo(); as undo) {
      <div class="lez-undo-snackbar-container">
        <div class="lez-undo-snackbar">
          
          <!-- Animated Progress Shrinking Line -->
          <div class="lez-undo-progress-bar" [style.width.%]="undo.progress"></div>

          <!-- Icon / Indicator -->
          <div style="display:flex;align-items:center;justify-content:center;width:28px;height:28px;background:rgba(234,88,12,0.2);border-radius:50%;color:#fb923c;flex-shrink:0">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
              <path d="M3 3v5h5"/>
            </svg>
          </div>

          <!-- Text message & countdown -->
          <div style="flex:1;min-width:0;display:flex;flex-direction:column">
            <div style="font-size:13.5px;font-weight:700;color:#f8fafc;display:flex;align-items:center;gap:6px">
              <span>{{ undo.message }}</span>
              <span style="font-size:11px;font-weight:600;color:#94a3b8">({{ undo.remainingSeconds }}s)</span>
            </div>
            @if (undo.subMessage) {
              <div style="font-size:11.5px;color:#94a3b8;margin-top:1px">{{ undo.subMessage }}</div>
            }
          </div>

          <!-- Undo action button -->
          <button type="button" class="lez-undo-btn" (click)="undoService.executeUndo()" [disabled]="undoService.isExecutingUndo()">
            @if (undoService.isExecutingUndo()) {
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="animation:spin 1s linear infinite">
                <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
              </svg>
              <span>Restoring...</span>
            } @else {
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
                <path d="M3 3v5h5"/>
              </svg>
              <span>UNDO</span>
              <span class="lez-undo-shortcut">Ctrl+Z</span>
            }
          </button>

          <!-- Dismiss button -->
          <button type="button" (click)="undoService.dismiss()"
                  style="border:none;background:transparent;cursor:pointer;color:#64748b;padding:4px;display:flex;align-items:center;justify-content:center"
                  title="Dismiss">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>

        </div>
      </div>
    }
  `
})
export class ToastContainer {
  toastService = inject(ToastService);
  undoService  = inject(UndoService);
}
