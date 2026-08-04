import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private _isDark = signal<boolean>(true);
  readonly isDark = this._isDark.asReadonly();

  initTheme(): void {
    const stored = localStorage.getItem('aura-theme');
    const dark = stored !== 'light';
    this._isDark.set(dark);
    this._applyTheme(dark);
  }

  toggle(): void {
    const next = !this._isDark();
    this._isDark.set(next);
    this._applyTheme(next);
    localStorage.setItem('aura-theme', next ? 'dark' : 'light');
  }

  private _applyTheme(dark: boolean): void {
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
  }
}
