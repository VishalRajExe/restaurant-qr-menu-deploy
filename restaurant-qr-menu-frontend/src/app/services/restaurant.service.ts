import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map, switchMap } from 'rxjs';
import { environment } from '../environments/environment';
import { Restaurant } from '../models/restaurant.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class RestaurantService {
  private http = inject(HttpClient);
  private restaurantsList = signal<Restaurant[]>([]);
  activeRestaurantId = signal<string>('1');

  getRestaurants() {
    return this.restaurantsList.asReadonly();
  }

  getRestaurantById(id: string): Restaurant | undefined {
    return this.restaurantsList().find((r: Restaurant) => r.id === id || r.slug === id);
  }

  getActiveRestaurant(): Restaurant | undefined {
    return this.restaurantsList().find((r: Restaurant) => r.id === this.activeRestaurantId()) || this.restaurantsList()[0];
  }

  /** Called by PublicMenuService to cache restaurant from unified endpoint */
  setRestaurant(restaurant: Restaurant): void {
    this.restaurantsList.update((list: Restaurant[]) => {
      const exists = list.some((r: Restaurant) => r.id === restaurant.id);
      return exists ? list.map((r: Restaurant) => r.id === restaurant.id ? restaurant : r) : [...list, restaurant];
    });
    this.activeRestaurantId.set(restaurant.id);
  }

  /**
   * Fetch restaurant profile by ID, slug, or QR token.
   * - Numeric string → GET /restaurants/{id}
   * - 32+ char hex string (QR token) → GET /public/qr/{token} then extract restaurant
   * - Otherwise → GET /restaurants/slug/{slug}
   */
  fetchRestaurantProfile(idOrSlug: string): Observable<Restaurant | undefined> {
    const numericId = parseInt(idOrSlug, 10);

    if (!isNaN(numericId)) {
      // Numeric ID → direct restaurant lookup (owner dashboard use case)
      return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${numericId}`).pipe(
        map((res: ApiResponse<any>) => this.mapAndCache(res)),
        catchError(() => of(this.getRestaurantById(idOrSlug)))
      );
    }

    // Check if this looks like a QR token (32-char hex, no dashes)
    const isQrToken = /^[a-f0-9]{32}$/i.test(idOrSlug) || /^[a-f0-9-]{36}$/.test(idOrSlug);

    if (isQrToken) {
      // Try to resolve via public QR endpoint → then fetch restaurant by ID
      return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/public/qr/${idOrSlug}`).pipe(
        switchMap((qrRes: ApiResponse<any>) => {
          const restaurantId = qrRes?.data?.restaurant?.id || qrRes?.data?.restaurantId;
          if (restaurantId) {
            return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/restaurants/slug/${idOrSlug}`).pipe(
              map((res: ApiResponse<any>) => this.mapAndCache(res)),
              catchError(() => {
                // Build from QR response data
                if (qrRes?.data?.restaurant) {
                  return of(this.mapFromData(qrRes.data.restaurant));
                }
                return of(this.getRestaurantById(idOrSlug));
              })
            );
          }
          return of(this.getRestaurantById(idOrSlug));
        }),
        catchError(() => {
          // Fallback: treat as slug
          return this.fetchBySlug(idOrSlug);
        })
      );
    }

    // Slug-based lookup (most common: customer menu via human-readable URL)
    return this.fetchBySlug(idOrSlug);
  }

  private fetchBySlug(slug: string): Observable<Restaurant | undefined> {
    return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/restaurants/slug/${slug}`).pipe(
      map((res: ApiResponse<any>) => this.mapAndCache(res)),
      catchError((err: { message: string }) => {
        console.warn('Fetch restaurant by slug failed:', err.message);
        return of(this.getRestaurantById(slug));
      })
    );
  }

  private mapAndCache(res: ApiResponse<any>): Restaurant | undefined {
    if (res && res.success && res.data) {
      const mapped = this.mapFromData(res.data);
      this.restaurantsList.update((list: Restaurant[]) => {
        const exists = list.some((r: Restaurant) => r.id === mapped.id);
        return exists ? list.map((r: Restaurant) => r.id === mapped.id ? mapped : r) : [...list, mapped];
      });
      return mapped;
    }
    return undefined;
  }

  private mapFromData(d: any): Restaurant {
    const id = String(d.id);
    const storedVerification = localStorage.getItem('restaurant_verification_' + id) || localStorage.getItem('restaurant_verification_1');
    return {
      id: String(d.id),
      name: d.name || 'Restaurant',
      slug: d.slug || String(d.id),
      tagline: d.description || d.tagline || 'Gourmet Dining & Digital QR Menu',
      logo: d.logoUrl || d.logo || '',
      banner: d.bannerUrl || d.banner || '',
      address: d.address || '',
      phone: d.phone || '',
      rating: d.rating || 4.8,
      reviewCount: d.reviewCount || 0,
      currency: d.currency || '₹',
      tableCount: d.tableCount || 20,
      isPublished: d.status === 'ACTIVE' || d.isPublished === true,
      verificationStatus: (storedVerification as any) || d.verificationStatus || 'PENDING_VERIFICATION'
    };
  }

  setVerificationStatus(id: string, status: 'VERIFIED' | 'REJECTED' | 'PENDING_VERIFICATION') {
    localStorage.setItem('restaurant_verification_' + id, status);
    localStorage.setItem('restaurant_verification_1', status);
    this.restaurantsList.update((list: Restaurant[]) =>
      list.map((r: Restaurant) => (r.id === id || id === '1' || r.id === '1') ? { ...r, verificationStatus: status } : r)
    );
  }

  updateProfile(id: string, updated: Partial<Restaurant>) {
    this.updateRestaurant(id, updated);
  }

  updateRestaurant(id: string, updated: Partial<Restaurant>) {
    this.restaurantsList.update((list: Restaurant[]) =>
      list.map((r: Restaurant) => r.id === id ? { ...r, ...updated } : r)
    );

    const numericId = parseInt(id.replace('r', ''), 10) || 1;
    const body = {
      name: updated.name,
      slug: updated.slug,
      description: updated.tagline,
      address: updated.address,
      phone: updated.phone
    };

    this.http.put<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${numericId}`, body)
      .pipe(catchError(() => of(null)))
      .subscribe();
  }

  addRestaurant(restaurant: Omit<Restaurant, 'id'>): Restaurant {
    const newRestaurant: Restaurant = {
      ...restaurant,
      id: 'r' + (this.restaurantsList().length + 1)
    };
    this.restaurantsList.update((list: Restaurant[]) => [...list, newRestaurant]);
    return newRestaurant;
  }
}
