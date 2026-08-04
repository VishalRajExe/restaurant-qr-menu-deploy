import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './services/theme';
import { ToastContainer } from './components/toast/toast';
import { ConfirmModal } from './components/modal/modal';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastContainer, ConfirmModal],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('AuraMenu');

  constructor(private themeService: ThemeService) {
    this.themeService.initTheme();
  }
}
