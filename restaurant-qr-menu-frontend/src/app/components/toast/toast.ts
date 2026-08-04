import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastMessage } from '../../services/toast.service';

@Component({
  selector: 'app-toast-container',
  imports: [CommonModule],
  template: `
    <div class="fixed bottom-6 right-6 z-50 flex flex-col space-y-3 pointer-events-none max-w-sm w-full px-4 sm:px-0">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="pointer-events-auto flex items-start space-x-3 p-4 rounded-2xl border shadow-2xl backdrop-blur-xl transition-all duration-300 transform translate-y-0 animate-bounce-short"
             [ngClass]="{
               'bg-emerald-950/90 border-emerald-500/30 text-emerald-100 shadow-emerald-950/40': toast.type === 'success',
               'bg-rose-950/90 border-rose-500/30 text-rose-100 shadow-rose-950/40': toast.type === 'error',
               'bg-amber-950/90 border-amber-500/30 text-amber-100 shadow-amber-950/40': toast.type === 'warning',
               'bg-zinc-900/90 border-white/10 text-gray-100 shadow-zinc-950/50': toast.type === 'info'
             }">
          
          <div class="shrink-0 mt-0.5">
            @if (toast.type === 'success') {
              <div class="w-6 h-6 rounded-full bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold text-xs">✓</div>
            } @else if (toast.type === 'error') {
              <div class="w-6 h-6 rounded-full bg-rose-500/20 text-rose-400 flex items-center justify-center font-bold text-xs">✕</div>
            } @else if (toast.type === 'warning') {
              <div class="w-6 h-6 rounded-full bg-amber-500/20 text-amber-400 flex items-center justify-center font-bold text-xs">!</div>
            } @else {
              <div class="w-6 h-6 rounded-full bg-blue-500/20 text-blue-400 flex items-center justify-center font-bold text-xs">i</div>
            }
          </div>

          <div class="flex-1 min-w-0">
            <h4 class="font-display font-bold text-xs tracking-wide uppercase">{{ toast.title }}</h4>
            @if (toast.message) {
              <p class="text-xs text-gray-300 mt-0.5 leading-relaxed font-sans">{{ toast.message }}</p>
            }
          </div>

          <button (click)="toastService.remove(toast.id)" type="button"
                  class="shrink-0 text-gray-400 hover:text-white text-xs p-1">
            ✕
          </button>
        </div>
      }
    </div>
  `
})
export class ToastContainer {
  toastService = inject(ToastService);
}
