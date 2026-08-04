import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface BranchData {
  id: string;
  name: string;
  address?: string;
  city?: string;
  phone?: string;
  isMain?: boolean;
  isActive?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class BranchService {
  private http = inject(HttpClient);

  branchesByRestaurant = signal<Record<string, BranchData[]>>({});
  defaultBranchId      = signal<string>('1');

  fetchBranches(restaurantId: string): Observable<BranchData[]> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;

    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/restaurants/${numericId}/branches`).pipe(
      map((res: ApiResponse<any[]>) => {
        if (res && res.success && Array.isArray(res.data) && res.data.length > 0) {
          const mapped: BranchData[] = res.data
            .filter((b: any) => !b.isDeleted)
            .map((b: any) => ({
              id: String(b.id),
              name: b.name || 'Main Branch',
              address: b.address || '',
              city: b.city || '',
              phone: b.phone || '',
              isMain: b.isMain || b.isDefault || false,
              isActive: b.isActive !== false
            }));

          this.branchesByRestaurant.update((prevMap: Record<string, BranchData[]>) => ({ ...prevMap, [restaurantId]: mapped }));

          // Set default branch to the first active branch
          const mainBranch = mapped.find((b: BranchData) => b.isMain) || mapped.find((b: BranchData) => b.isActive) || mapped[0];
          if (mainBranch) {
            this.defaultBranchId.set(mainBranch.id);
          }

          return mapped;
        }
        return this.branchesByRestaurant()[restaurantId] || [];
      }),
      catchError((err: { message: string }) => {
        console.warn('Fetch branches failed, will use branchId=1:', err.message);
        // Keep default branchId=1 as fallback
        this.defaultBranchId.set('1');
        return of(this.branchesByRestaurant()[restaurantId] || []);
      })
    );
  }

  getBranchesForRestaurant(restaurantId: string): BranchData[] {
    return this.branchesByRestaurant()[restaurantId] || [];
  }

  getDefaultBranchId(): string {
    return this.defaultBranchId();
  }

  createBranch(restaurantId: string, name: string, address: string = ''): Observable<BranchData | null> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    const body = { name, address };

    return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${numericId}/branches`, body).pipe(
      map((res: ApiResponse<any>) => {
        if (res && res.success && res.data) {
          const branch: BranchData = {
            id: String(res.data.id),
            name: res.data.name,
            address: res.data.address || address,
            isMain: false,
            isActive: true
          };
          this.branchesByRestaurant.update((prevMap: Record<string, BranchData[]>) => {
            const existing = prevMap[restaurantId] || [];
            return { ...prevMap, [restaurantId]: [...existing, branch] };
          });
          return branch;
        }
        return null;
      }),
      catchError((err: { message: string }) => {
        console.warn('Create branch failed:', err.message);
        return of(null);
      })
    );
  }
}
