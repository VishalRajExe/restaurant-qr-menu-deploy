import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModalService } from '../../services/modal.service';

@Component({
  selector: 'app-confirm-modal',
  imports: [CommonModule],
  template: `
    @if (modalService.activeModal(); as config) {
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fade-in font-sans">
        <div class="max-w-md w-full p-6 sm:p-8 rounded-3xl bg-[#0f1117] border border-white/10 shadow-2xl space-y-6 relative overflow-hidden">
          
          <!-- Background Glow Accent -->
          <div class="absolute -top-24 -right-24 w-48 h-48 rounded-full pointer-events-none blur-3xl"
               [ngClass]="{
                 'bg-rose-500/10': config.type === 'danger',
                 'bg-amber-500/10': config.type === 'warning',
                 'bg-orange-500/10': !config.type || config.type === 'info'
               }">
          </div>

          <div class="space-y-2">
            <h3 class="font-display font-black text-xl text-white tracking-tight">
              {{ config.title }}
            </h3>
            <p class="text-xs text-gray-400 leading-relaxed">
              {{ config.message }}
            </p>
          </div>

          <div class="flex items-center justify-end space-x-3 pt-4 border-t border-white/5">
            <button (click)="modalService.close()" type="button"
                    class="px-5 py-2.5 rounded-xl border border-white/10 text-xs font-semibold text-gray-300 hover:bg-white/5 transition-all cursor-pointer">
              {{ config.cancelText || 'Cancel' }}
            </button>
            <button (click)="modalService.proceed()" type="button"
                    class="px-6 py-2.5 rounded-xl text-xs font-bold text-white shadow-lg transition-all cursor-pointer"
                    [ngClass]="{
                      'bg-rose-500 hover:bg-rose-600 shadow-rose-500/20': config.type === 'danger',
                      'bg-amber-500 hover:bg-amber-600 shadow-amber-500/20': config.type === 'warning',
                      'bg-orange-500 hover:bg-orange-600 shadow-orange-500/20': !config.type || config.type === 'info'
                    }">
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
}
