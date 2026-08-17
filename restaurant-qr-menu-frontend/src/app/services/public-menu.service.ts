import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { Restaurant } from '../models/restaurant.model';
import { Category } from '../models/category.model';
import { MenuItem } from '../models/menu-item.model';
import { Offer } from '../models/offer.model';
import { ApiResponse } from '../models/api-response.model';
import { RestaurantService } from './restaurant.service';
import { CategoryService } from './category.service';
import { MenuService } from './menu.service';
import { OfferService } from './offer.service';

export interface PublicMenuPayload {
  restaurant: Restaurant;
  qrCode?: any;
  categories: Category[];
  menuItems: MenuItem[];
  activeOffers: Offer[];
}

@Injectable({
  providedIn: 'root'
})
export class PublicMenuService {
  private http              = inject(HttpClient);
  private restaurantService = inject(RestaurantService);
  private categoryService   = inject(CategoryService);
  private menuService       = inject(MenuService);
  private offerService      = inject(OfferService);

  /**
   * Unified public menu endpoint.
   * GET /public/menu/{tokenOrSlug}
   * Resolves QR token OR restaurant slug in one shot.
   * Populates all individual service signals for reactive computed use.
   */
  fetchPublicMenu(tokenOrSlug: string): Observable<PublicMenuPayload | null> {
    const url = `${environment.apiUrl}/public/menu/${encodeURIComponent(tokenOrSlug)}`;

    return this.http.get<ApiResponse<any>>(url).pipe(
      map(res => {
        if (!res || !res.success || !res.data) return null;
        const d = res.data;

        // ── Map Restaurant ────────────────────────────────────────────────────
        const rd = d.restaurant || {};
        const restaurant: Restaurant = {
          id:          String(rd.id),
          name:        rd.name || 'Restaurant',
          slug:        rd.slug || tokenOrSlug,
          tagline:     rd.description || rd.tagline || '',
          logo:        rd.logoUrl || rd.logo || '',
          banner:      rd.bannerUrl || rd.banner || '',
          address:     rd.address || '',
          phone:       rd.phone || '',
          rating:      rd.rating ?? 4.8,
          reviewCount: rd.reviewCount ?? 0,
          currency:    rd.currency || '₹',
          tableCount:  rd.tableCount || 20,
          isPublished: rd.status === 'ACTIVE' || rd.isPublished === true
        };

        // ── Map Categories ────────────────────────────────────────────────────
        const categories: Category[] = (d.categories || []).map((c: any) => ({
          id:           String(c.id),
          restaurantId: String(rd.id),
          name:         c.name,
          icon:         c.icon || 'Utensils',
          sortOrder:    c.displayOrder || c.sortOrder || 1
        }));

        // ── Map Menu Items ────────────────────────────────────────────────────
        const menuItems: MenuItem[] = (d.menuItems || []).map((item: any) => ({
          id:          String(item.id),
          categoryId:  String(item.categoryId || item.category?.id || ''),
          name:        item.name,
          price:       Number(item.price),
          description: item.description || '',
          image:       item.imageUrl || item.image || '',
          isAvailable: item.isAvailable ?? true,
          isVeg:       item.vegNonveg === 'VEG' || item.isVeg === true,
          isPopular:   item.isPopular || item.isChefSpecial || false,
          spicyLevel:  item.spiceLevel || 0,
          calories:    item.calories || undefined
        }));

        // ── Map Offers ────────────────────────────────────────────────────────
        const activeOffers: Offer[] = (d.activeOffers || []).map((o: any) => ({
          id:                  String(o.id),
          restaurantId:        String(rd.id),
          title:               o.title,
          discountPercentage:  o.discountPercentage || 10,
          badge:               o.code || 'OFFER',
          description:         o.description || `${o.discountPercentage || 10}% off`,
          code:                o.code || '',
          validUntil:          o.endDate || '',
          isActive:            o.isActive !== false
        }));

        const payload: PublicMenuPayload = { restaurant, qrCode: d.qrCode, categories, menuItems, activeOffers };

        // ── Cache into individual services via public methods ─────────────────
        this.restaurantService.setRestaurant(restaurant);
        this.categoryService.setCategories(categories);
        this.menuService.setMenuItems(menuItems);
        this.offerService.setOffers(activeOffers);

        return payload;
      }),
      catchError(err => {
        console.warn('Public menu fetch failed (will try fallback):', err.message);
        return of(null);
      })
    );
  }
}
