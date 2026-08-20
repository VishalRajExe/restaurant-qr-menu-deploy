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
  readonly menuItems   = this.menuItemsList.asReadonly();
  private localAddedItems: MenuItem[] = [];

  constructor() {
    this.loadLocalItems();
  }

  private loadLocalItems() {
    try {
      const stored = localStorage.getItem('aura_added_dishes');
      if (stored) {
        this.localAddedItems = JSON.parse(stored);
        if (Array.isArray(this.localAddedItems) && this.localAddedItems.length > 0) {
          this.menuItemsList.set(this.localAddedItems);
        }
      }
    } catch (e) {
      console.warn('Could not read local added items from storage', e);
    }
  }

  private saveLocalItems() {
    try {
      localStorage.setItem('aura_added_dishes', JSON.stringify(this.localAddedItems));
    } catch (e) {
      console.warn('Could not save local added items to storage', e);
    }
  }

  getMenuItemsForCategories(categoryIds?: string[]): MenuItem[] {
    return this.menuItemsList();
  }

  getMenuItemsForCategory(categoryId: string): MenuItem[] {
    const cleanId = String(categoryId).toLowerCase().replace(/^c/, '');
    return this.menuItemsList().filter(item => {
      const itemCatId = String(item.categoryId).toLowerCase().replace(/^c/, '');
      return itemCatId === cleanId || item.categoryId === categoryId;
    });
  }

  setMenuItems(items: MenuItem[]): void {
    this.menuItemsList.set(items);
  }

  fetchMenuItems(restaurantId: string | number): Observable<MenuItem[]> {
    const numericId = parseInt(String(restaurantId), 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/public/restaurants/${numericId}/menu`).pipe(
      map(res => {
        if (res && res.success && Array.isArray(res.data)) {
          const apiMapped: MenuItem[] = res.data.map(item => ({
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

          // Merge API items with locally persisted items to ensure NO dish is lost on refresh
          const combined = [...apiMapped];
          for (const localItem of this.localAddedItems) {
            if (!combined.some(i => String(i.id) === String(localItem.id) || i.name.toLowerCase() === localItem.name.toLowerCase())) {
              combined.push(localItem);
            }
          }

          this.menuItemsList.set(combined);
          return combined;
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
    const newItemId = 'm_' + Date.now() + '_' + Math.floor(Math.random() * 1000);
    const newItem: MenuItem = {
      ...item,
      id: newItemId
    };

    // Store in local array, localStorage, and signal
    this.localAddedItems.push(newItem);
    this.saveLocalItems();
    this.menuItemsList.update(list => [...list, newItem]);

    const restId = 1;
    let parsedCatId = parseInt(String(item.categoryId).replace(/^c_?/, ''), 10);
    const numericCatId = (!isNaN(parsedCatId) && parsedCatId <= 2147483647) ? parsedCatId : 1;
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
      .subscribe((res: ApiResponse<any> | null) => {
        if (res && res.success && res.data && res.data.id) {
          const serverId = String(res.data.id);
          newItem.id = serverId;
          this.menuItemsList.update(list =>
            list.map(i => i.id === newItemId ? { ...i, id: serverId } : i)
          );
          this.saveLocalItems();
        }
      });

    return newItem;
  }

  updateMenuItem(id: string, updated: Partial<MenuItem>) {
    this.menuItemsList.update(list =>
      list.map(item => item.id === id ? { ...item, ...updated } : item)
    );

    this.localAddedItems = this.localAddedItems.map(item =>
      item.id === id ? { ...item, ...updated } : item
    );
    this.saveLocalItems();

    const numericId = parseInt(id.replace(/^m_?/, ''), 10);
    if (!isNaN(numericId)) {
      const restId = 1;
      const target = this.menuItemsList().find(i => i.id === id);
      if (target) {
        const body = {
          categoryId: parseInt(String(target.categoryId).replace(/^c/, ''), 10) || 1,
          name: target.name,
          description: target.description,
          price: target.price,
          vegNonveg: target.isVeg ? 'VEG' : 'NON_VEG',
          isAvailable: target.isAvailable,
          isPopular: target.isPopular
        };
        this.http.put<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/menu-items/${numericId}`, body)
          .pipe(catchError(() => of(null)))
          .subscribe();
      }
    }
  }

  deleteMenuItem(id: string) {
    this.menuItemsList.update(list => list.filter(item => item.id !== id));
    this.localAddedItems = this.localAddedItems.filter(item => item.id !== id);
    this.saveLocalItems();

    const numericId = parseInt(id.replace(/^m_?/, ''), 10);
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
          const numericId = parseInt(id.replace(/^m_?/, ''), 10);
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

  restoreMenuItem(id: string, itemData?: MenuItem): Observable<any> {
    if (itemData) {
      this.menuItemsList.update(list => [...list.filter(i => i.id !== id), itemData]);
      this.localAddedItems.push(itemData);
      this.saveLocalItems();
    }

    const numericId = parseInt(id.replace(/^m_?/, ''), 10);
    if (!isNaN(numericId)) {
      return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/restaurants/1/menu-items/${numericId}/restore`, {}).pipe(
        catchError(() => of(null))
      );
    }
    return of(null);
  }
}
