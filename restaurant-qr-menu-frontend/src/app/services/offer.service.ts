import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { Offer } from '../models/offer.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class OfferService {
  private http = inject(HttpClient);
  private offersList = signal<Offer[]>([]);

  getOffersForRestaurant(restaurantId: string): Offer[] {
    return this.offersList().filter(o => o.restaurantId === restaurantId || !o.restaurantId);
  }

  /** Called by PublicMenuService to set offers from unified endpoint */
  setOffers(offers: Offer[]): void {
    this.offersList.set(offers);
  }

  fetchActiveOffers(restaurantId: string): Observable<Offer[]> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/public/restaurants/${numericId}/offers`).pipe(
      map(res => {
        if (res && res.success && Array.isArray(res.data)) {
          const mapped: Offer[] = res.data.map(o => ({
            id: String(o.id),
            restaurantId: String(restaurantId),
            title: o.title,
            discountPercentage: o.discountPercentage || 20,
            badge: o.code || 'SPECIAL OFFER',
            description: o.description || `${o.discountPercentage || 20}% OFF on orders`,
            code: o.code || 'AURA20',
            validUntil: o.endDate || '2026-12-31',
            isActive: o.isActive !== false
          }));
          this.offersList.set(mapped);
          return mapped;
        }
        return this.getOffersForRestaurant(restaurantId);
      }),
      catchError(err => {
        console.warn('Fetch active offers notice:', err.message);
        return of(this.getOffersForRestaurant(restaurantId));
      })
    );
  }

  addOffer(offer: Omit<Offer, 'id'>): Offer {
    const newOffer: Offer = {
      ...offer,
      id: 'off_' + Date.now()
    };
    this.offersList.update(list => [...list, newOffer]);

    const restId = parseInt(offer.restaurantId.replace('r', ''), 10) || 1;
    const body = {
      title: offer.title,
      discountType: 'PERCENTAGE',
      discountPercentage: offer.discountPercentage,
      startDate: new Date().toISOString().split('T')[0],
      endDate: offer.validUntil || '2026-12-31'
    };

    this.http.post<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/offers`, body)
      .pipe(catchError(() => of(null)))
      .subscribe();

    return newOffer;
  }
}
