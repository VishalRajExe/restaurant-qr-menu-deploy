import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { MenuItem } from '../models/menu-item.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class MenuService {
  private http = inject(HttpClient);
  private menuItemsList = signal<MenuItem[]>([]);

  getMenuItemsForCategories(categoryIds: string[]): MenuItem[] {
    return this.menuItemsList().filter(item => categoryIds.includes(item.categoryId));
  }

  getMenuItemsForCategory(categoryId: string): MenuItem[] {
    return this.menuItemsList().filter(item => item.categoryId === categoryId);
  }

  /** Called by PublicMenuService to set menu items from unified endpoint */
  setMenuItems(items: MenuItem[]): void {
    this.menuItemsList.set(items);
  }

  fetchMenuItems(restaurantId: string): Observable<MenuItem[]> {
    const numericId = parseInt(restaurantId, 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/public/restaurants/${numericId}/menu`).pipe(
      map(res => {
        if (res && res.success && Array.isArray(res.data)) {
          const mapped: MenuItem[] = res.data.map(item => ({
            id: String(item.id),
            categoryId: String(item.categoryId || (item.category ? item.category.id : 'c1')),
            name: item.name,
            price: Number(item.price),
            description: item.description || '',
            image: item.imageUrl || item.image || 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80',
            isAvailable: item.isAvailable ?? true,
            isVeg: item.vegNonveg === 'VEG' || item.isVeg === true,
            isPopular: item.isPopular || item.isChefSpecial || false,
            spicyLevel: item.spiceLevel || 0,
            calories: item.calories || undefined
          }));
          this.menuItemsList.set(mapped);
          return mapped;
        }
        return this.menuItemsList();
      }),
      catchError(err => {
        console.warn('API fetch menu items notice:', err.message);
        return of(this.menuItemsList());
      })
    );
  }

  addMenuItem(item: Omit<MenuItem, 'id'>): MenuItem {
    const newItem: MenuItem = {
      ...item,
      id: 'm' + (this.menuItemsList().length + 1)
    };
    this.menuItemsList.update(list => [...list, newItem]);

    const restId = 1;
    const numericCatId = parseInt(item.categoryId.replace('c', ''), 10) || 1;
    const body = {
      categoryId: numericCatId,
      name: item.name,
      description: item.description,
      price: item.price,
      vegNonveg: item.isVeg ? 'VEG' : 'NON_VEG',
      isAvailable: item.isAvailable,
      isPopular: item.isPopular,
      spiceLevel: item.spicyLevel
    };

    this.http.post<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/menu-items`, body)
      .pipe(catchError(() => of(null)))
      .subscribe();

    return newItem;
  }

  updateMenuItem(id: string, updated: Partial<MenuItem>) {
    this.menuItemsList.update(list =>
      list.map(item => item.id === id ? { ...item, ...updated } : item)
    );

    const numericId = parseInt(id.replace('m', ''), 10);
    if (!isNaN(numericId)) {
      const restId = 1;
      const target = this.menuItemsList().find(i => i.id === id);
      if (target) {
        const body = {
          categoryId: parseInt(target.categoryId.replace('c', ''), 10) || 1,
          name: target.name,
          description: target.description,
          price: target.price,
          vegNonveg: target.isVeg ? 'VEG' : 'NON_VEG',
          isAvailable: target.isAvailable
        };
        this.http.put<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/menu-items/${numericId}`, body)
          .pipe(catchError(() => of(null)))
          .subscribe();
      }
    }
  }

  deleteMenuItem(id: string) {
    this.menuItemsList.update(list => list.filter(item => item.id !== id));

    const numericId = parseInt(id.replace('m', ''), 10);
    if (!isNaN(numericId)) {
      const restId = 1;
      this.http.delete<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/menu-items/${numericId}`)
        .pipe(catchError(() => of(null)))
        .subscribe();
    }
  }

  toggleAvailability(id: string) {
    this.menuItemsList.update(list =>
      list.map(item => {
        if (item.id === id) {
          const next = !item.isAvailable;
          const numericId = parseInt(id.replace('m', ''), 10);
          if (!isNaN(numericId)) {
            this.http.patch<ApiResponse<any>>(`${environment.apiUrl}/restaurants/1/menu-items/${numericId}/availability`, { available: next })
              .pipe(catchError(() => of(null)))
              .subscribe();
          }
          return { ...item, isAvailable: next };
        }
        return item;
      })
    );
  }
}
