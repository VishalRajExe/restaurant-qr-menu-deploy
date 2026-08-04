import { Component, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-hero',
  imports: [CommonModule, RouterLink],
  templateUrl: './hero.html',
  styleUrls: ['./hero.css']
})
export class Hero {
  @Input() isDarkMode = true;

  // Track Demo video overlay dialog toggle
  showDemoVideo = signal<boolean>(false);

  toggleDemo(state: boolean) {
    this.showDemoVideo.set(state);
  }
}
