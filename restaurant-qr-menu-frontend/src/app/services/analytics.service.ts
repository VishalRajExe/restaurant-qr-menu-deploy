import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface DashboardKpiData {
  totalScans: number;
  totalCategories: number;
  totalMenuItems: number;
  popularItemName: string;
  peakHour: string;
  todayScans?: number;
  weeklyScans?: number;
  avgRating?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private http = inject(HttpClient);

  // Initialize with zeros — all values come from live API
  dashboardKpi = signal<DashboardKpiData>({
    totalScans: 0,
    totalCategories: 0,
    totalMenuItems: 0,
    popularItemName: '—',
    peakHour: '—',
    todayScans: 0,
    weeklyScans: 0,
    avgRating: 0
  });

  fetchDashboardKpi(restaurantId: string): Observable<DashboardKpiData> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/analytics/restaurants/${numericId}/dashboard`).pipe(
      map((res: ApiResponse<any>) => {
        if (res && res.success && res.data) {
          const d = res.data;
          const kpis: DashboardKpiData = {
            totalScans: d.totalScans ?? d.scansCount ?? 0,
            totalCategories: d.totalCategories ?? 0,
            totalMenuItems: d.totalMenuItems ?? 0,
            popularItemName: d.popularItemName || d.topItem?.name || '—',
            peakHour: d.peakHour || '—',
            todayScans: d.todayScans ?? 0,
            weeklyScans: d.weeklyScans ?? 0,
            avgRating: d.avgRating ?? 0
          };
          this.dashboardKpi.set(kpis);
          return kpis;
        }
        return this.dashboardKpi();
      }),
      catchError((err: { message: string }) => {
        console.warn('Fetch analytics dashboard failed:', err.message);
        return of(this.dashboardKpi());
      })
    );
  }

  fetchPopularItems(restaurantId: string): Observable<any[]> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/analytics/restaurants/${numericId}/popular-items`).pipe(
      map((res: ApiResponse<any[]>) => (res && res.success && Array.isArray(res.data)) ? res.data : []),
      catchError(() => of([]))
    );
  }

  fetchScanTrend(restaurantId: string, days: number = 7): Observable<any[]> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/analytics/restaurants/${numericId}/scan-trend?days=${days}`).pipe(
      map((res: ApiResponse<any[]>) => (res && res.success && Array.isArray(res.data)) ? res.data : []),
      catchError(() => of([]))
    );
  }
}
