import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface AdminRestaurantData {
  id: string;
  name: string;
  owner: string;
  ownerEmail?: string;
  ownerPhone?: string;
  location: string;
  address?: string;
  photo?: string;
  description?: string;
  cuisine?: string;
  gstin?: string;
  totalScans: number;
  totalItems?: number;
  joinedDate?: string;
  plan: 'Basic' | 'Pro' | 'Enterprise';
  status: 'active' | 'inactive';
  verificationStatus?: 'PENDING_VERIFICATION' | 'VERIFIED' | 'REJECTED';
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);

  // Initialize empty — populated from live API
  restaurantsList = signal<AdminRestaurantData[]>([]);

  fetchRestaurants(): Observable<AdminRestaurantData[]> {
    return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/super-admin/restaurants`).pipe(
      map((res: ApiResponse<any>) => {
        const dataArray = res?.data?.content || res?.data;
        if (Array.isArray(dataArray) && dataArray.length > 0) {
          const mapped: AdminRestaurantData[] = dataArray.map((r: any) => ({
            id: String(r.id),
            name: r.name || 'Unnamed Restaurant',
            owner: r.ownerName || r.ownerEmail || 'Partner Owner',
            ownerEmail: r.ownerEmail || r.email || '',
            ownerPhone: r.phone || r.ownerPhone || '',
            location: r.city || r.address || 'Location not set',
            address: r.address || '',
            photo: r.bannerUrl || r.logoUrl || r.photo || 'https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=600&q=80',
            description: r.description || '',
            cuisine: r.cuisineType || r.cuisine || 'Multi-Cuisine',
            gstin: r.gstin || '',
            totalScans: r.totalScans || r.scansCount || 0,
            totalItems: r.totalMenuItems || r.totalItems || 0,
            joinedDate: r.createdAt ? new Date(r.createdAt).toISOString().split('T')[0] : '',
            plan: r.subscriptionPlan || r.plan || 'Basic',
            status: (r.status === 'ACTIVE') ? 'active' : 'inactive',
            verificationStatus: r.verificationStatus || 'PENDING_VERIFICATION'
          }));
          this.restaurantsList.set(mapped);
          return mapped;
        }
        return this.restaurantsList();
      }),
      catchError((err: { message: string }) => {
        console.warn('Fetch super admin restaurants failed:', err.message);
        return of(this.restaurantsList());
      })
    );
  }

  toggleRestaurantStatus(id: string): Observable<boolean> {
    const current = this.restaurantsList().find((r: AdminRestaurantData) => r.id === id);
    const nextStatus = current?.status === 'active' ? 'SUSPENDED' : 'ACTIVE';

    // Optimistic update
    this.restaurantsList.update((list: AdminRestaurantData[]) =>
      list.map((r: AdminRestaurantData) => r.id === id ? { ...r, status: nextStatus === 'ACTIVE' ? 'active' as const : 'inactive' as const } : r)
    );

    const numericId = parseInt(id, 10);
    if (!isNaN(numericId)) {
      return this.http.patch<ApiResponse<any>>(
        `${environment.apiUrl}/super-admin/restaurants/${numericId}/status?status=${nextStatus}`, {}
      ).pipe(
        map(() => true),
        catchError(() => of(true))
      );
    }
    return of(true);
  }

  updateVerificationStatus(id: string, verificationStatus: 'VERIFIED' | 'REJECTED'): Observable<boolean> {
    this.restaurantsList.update((list: AdminRestaurantData[]) =>
      list.map((r: AdminRestaurantData) => r.id === id ? { ...r, verificationStatus } : r)
    );

    const numericId = parseInt(id, 10);
    if (!isNaN(numericId)) {
      return this.http.patch<ApiResponse<any>>(
        `${environment.apiUrl}/restaurants/${numericId}/verification?status=${verificationStatus}`, {}
      ).pipe(
        map(() => true),
        catchError(() => of(true))
      );
    }
    return of(true);
  }

  fetchPlatformAnalytics(): Observable<any> {
    return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/super-admin/analytics/overview`).pipe(
      map((res: ApiResponse<any>) => res?.data || null),
      catchError(() => of(null))
    );
  }
}
