import { Component, inject, input } from '@angular/core';
import { CommonModule, Location } from '@angular/common';

@Component({
  selector: 'app-back-button',
  imports: [CommonModule],
  template: `
    <button (click)="goBack()" type="button"
            class="px-3.5 py-2 rounded-xl bg-zinc-900/80 hover:bg-zinc-800 border border-white/10 text-xs font-semibold text-gray-300 hover:text-white transition-all shadow-sm cursor-pointer flex items-center space-x-2">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
      </svg>
      <span>{{ label() || 'Back' }}</span>
    </button>
  `
})
export class BackButton {
  label = input<string>('Back');
  private location = inject(Location);

  goBack() {
    this.location.back();
  }
}
