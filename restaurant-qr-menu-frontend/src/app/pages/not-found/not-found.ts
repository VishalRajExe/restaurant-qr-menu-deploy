import { Component, inject } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [CommonModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center p-6 bg-[#08090b] text-gray-100 font-sans relative overflow-hidden">
      <!-- Background Ambient Glows -->
      <div class="absolute -top-40 -left-40 w-96 h-96 rounded-full bg-orange-500/5 blur-3xl pointer-events-none"></div>
      <div class="absolute -bottom-40 -right-40 w-96 h-96 rounded-full bg-amber-500/5 blur-3xl pointer-events-none"></div>

      <div class="max-w-md w-full p-8 sm:p-12 rounded-3xl bg-[#0f1117] border border-white/5 shadow-2xl text-center space-y-6 relative z-10">
        
        <!-- 404 Illustration Badge -->
        <div class="w-20 h-20 rounded-3xl bg-orange-500/10 border border-orange-500/20 text-orange-500 flex items-center justify-center mx-auto text-3xl font-black font-display shadow-inner">
          404
        </div>

        <div class="space-y-2">
          <h2 class="font-display font-black text-2xl sm:text-3xl text-white tracking-tight">
            Page Not Found
          </h2>
          <p class="text-xs text-gray-400 leading-relaxed font-sans">
            The page or digital menu table link you requested could not be located. It may have been moved or deleted.
          </p>
        </div>

        <div class="flex flex-col sm:flex-row items-center justify-center gap-3 pt-4 border-t border-white/5">
          <button (click)="goBack()" type="button"
                  class="w-full sm:w-auto px-5 py-3 rounded-xl border border-white/10 text-xs font-semibold text-gray-300 hover:bg-white/5 transition-all cursor-pointer flex items-center justify-center space-x-2">
            <span>← Go Back</span>
          </button>
          <a routerLink="/"
             class="w-full sm:w-auto px-6 py-3 rounded-xl bg-orange-500 hover:bg-orange-600 text-xs font-bold text-white shadow-lg shadow-orange-500/10 transition-all cursor-pointer">
            Return to Home
          </a>
        </div>

      </div>
    </div>
  `
})
export class NotFound {
  private location = inject(Location);

  goBack() {
    this.location.back();
  }
}
