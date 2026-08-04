import { Component, inject, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Navbar } from '../../components/navbar/navbar';
import { Hero } from '../../components/hero/hero';
import { Features } from '../../components/features/features';
import { Footer } from '../../components/footer/footer';
import { PRICING_PLANS, TESTIMONIALS, STATS_DATA, CLIENT_LOGOS, PORTFOLIO_HOWITWORKS } from '../../mock-data/landing.data';

@Component({
  selector: 'app-landing',
  imports: [CommonModule, RouterLink, Navbar, Hero, Features, Footer],
  templateUrl: './landing.html',
  styleUrls: ['./landing.css']
})
export class Landing implements AfterViewInit {
  @ViewChild(Navbar) navbarComponent!: Navbar;

  stats = STATS_DATA;
  logos = CLIENT_LOGOS;
  howItWorks = PORTFOLIO_HOWITWORKS;
  testimonials = TESTIMONIALS;
  pricingPlans = PRICING_PLANS;

  ngAfterViewInit() {
    // Initial sync of theme or state wrapper if needed
  }

  // Get active theme state directly from the nested navbar
  get isDarkMode(): boolean {
    return this.navbarComponent ? this.navbarComponent.isDarkMode() : true;
  }
}
