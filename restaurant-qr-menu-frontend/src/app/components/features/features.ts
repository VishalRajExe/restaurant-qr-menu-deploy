import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface FeatureCard {
  title: string;
  icon: string;
  description: string;
  highlight?: string;
}

@Component({
  selector: 'app-features',
  imports: [CommonModule],
  templateUrl: './features.html',
  styleUrls: ['./features.css']
})
export class Features {
  @Input() isDarkMode = true;

  featuresList: FeatureCard[] = [
    {
      title: 'Cinematic Visual Theme',
      description: 'Your guest menus look extraordinarily Appetizing, shifting colors, floating card reveals, and smooth horizontal scrolling.',
      icon: 'sparkles',
      highlight: 'SaaS Exclusive'
    },
    {
      title: 'Custom QR Canva Styles',
      description: 'Modify patterns, adjust solid/gradient foreground shades, and embed your signature logo inside print-ready codes.',
      icon: 'qr'
    },
    {
      title: 'Deep Real-time Analytics',
      description: 'Track daily table scans count, popular meals, conversion metrics, and active assistant request logs.',
      icon: 'chart'
    },
    {
      title: 'Interactive Promotion Banners',
      description: 'Launch mid-week discount popups and late-night drinks trigger banners automatically to pump average order value.',
      icon: 'gift'
    },
    {
      title: 'Dietary & Spicy Tagging',
      description: 'Categorize dishes easily with clear green-circle Veg badges, red-shield spicy limits, and raw calorie listings.',
      icon: 'shield'
    },
    {
      title: 'Seamless Drag Reordering',
      description: 'Rearrange entire menus or food categories using standard drag-and-drop sort orders simulation within the admin panel.',
      icon: 'drag'
    }
  ];
}
