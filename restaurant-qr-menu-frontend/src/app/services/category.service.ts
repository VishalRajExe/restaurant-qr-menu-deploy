import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { Category } from '../models/category.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private http = inject(HttpClient);
  private categoriesList = signal<Category[]>([]);
  readonly categories   = this.categoriesList.asReadonly();
  private localAddedCategories: Category[] = [];

  constructor() {
    this.loadLocalCategories();
  }

  private loadLocalCategories() {
    try {
      const stored = localStorage.getItem('aura_added_categories');
      if (stored) {
        this.localAddedCategories = JSON.parse(stored);
        if (Array.isArray(this.localAddedCategories) && this.localAddedCategories.length > 0) {
          this.categoriesList.set(this.localAddedCategories);
        }
      }
    } catch (e) {
      console.warn('Could not read local added categories from storage', e);
    }
  }

  private saveLocalCategories() {
    try {
      localStorage.setItem('aura_added_categories', JSON.stringify(this.localAddedCategories));
    } catch (e) {
      console.warn('Could not save local added categories to storage', e);
    }
  }

  getCategoriesForRestaurant(restaurantId?: string | number): Category[] {
    return this.categoriesList().sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  }

  setCategories(categories: Category[]): void {
    this.categoriesList.set(categories);
  }

  fetchCategories(restaurantId: string | number): Observable<Category[]> {
    const numericId = parseInt(String(restaurantId), 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/restaurants/${numericId}/categories`).pipe(
      map(res => {
        let apiMapped: Category[] = [];
        if (res && res.success && Array.isArray(res.data)) {
          apiMapped = res.data.map(item => ({
            id: String(item.id),
            restaurantId: String(restaurantId),
            name: item.name,
            icon: item.icon || 'Utensils',
            displayOrder: item.displayOrder,
            sortOrder: item.displayOrder
          }));

          // Merge API categories with locally persisted categories
          const combined = [...apiMapped];
          for (const localCat of this.localAddedCategories) {
            if (!combined.some(c => String(c.id) === String(localCat.id) || c.name.toLowerCase() === localCat.name.toLowerCase())) {
              combined.push(localCat);
            }
          }

          this.categoriesList.set(combined);
          return combined;
        }

        return this.getCategoriesForRestaurant(restaurantId);
      }),
      catchError(err => {
        console.warn('API fetch categories notice:', err.message);
        return of(this.getCategoriesForRestaurant(restaurantId));
      })
    );
  }

  addCategory(category: Omit<Category, 'id'>): Category {
    const tempId = 'c_' + Date.now();
    const newCategory: Category = {
      ...category,
      id: tempId,
      sortOrder: this.categoriesList().length + 1
    };

    this.localAddedCategories.push(newCategory);
    this.saveLocalCategories();
    this.categoriesList.update(list => [...list, newCategory]);

    const numericRestId = parseInt(category.restaurantId, 10) || 1;
    const body = {
      name: category.name,
      displayOrder: newCategory.sortOrder
    };

    this.http.post<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${numericRestId}/categories`, body)
      .pipe(catchError(() => of(null)))
      .subscribe(res => {
        if (res && res.success && res.data && res.data.id) {
          const serverId = String(res.data.id);
          newCategory.id = serverId;
          this.categoriesList.update(list =>
            list.map(c => c.id === tempId ? { ...c, id: serverId } : c)
          );
          this.saveLocalCategories();
        }
      });

    return newCategory;
  }

  updateCategory(id: string, name: string, icon?: string) {
    this.categoriesList.update(list =>
      list.map(c => c.id === id ? { ...c, name, icon: icon || c.icon } : c)
    );

    this.localAddedCategories = this.localAddedCategories.map(c =>
      c.id === id ? { ...c, name, icon: icon || c.icon } : c
    );
    this.saveLocalCategories();

    const numericId = parseInt(id.replace(/^c_?/, ''), 10);
    if (!isNaN(numericId)) {
      const restId = 1;
      this.http.put<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/categories/${numericId}`, { name, displayOrder: 1 })
        .pipe(catchError(() => of(null)))
        .subscribe();
    }
  }

  deleteCategory(id: string) {
    this.categoriesList.update(list => list.filter(c => c.id !== id));
    this.localAddedCategories = this.localAddedCategories.filter(c => c.id !== id);
    this.saveLocalCategories();

    const numericId = parseInt(id.replace(/^c_?/, ''), 10);
    if (!isNaN(numericId)) {
      const restId = 1;
      this.http.delete<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/categories/${numericId}`)
        .pipe(catchError(() => of(null)))
        .subscribe();
    }
  }

  reorderCategories(reordered: Category[]) {
    const updated = reordered.map((c, index) => ({ ...c, sortOrder: index + 1 }));
    this.categoriesList.update(list => {
      const rest = list.filter(c => c.restaurantId !== reordered[0]?.restaurantId);
      return [...rest, ...updated];
    });

    const restId = parseInt(reordered[0]?.restaurantId || '1', 10) || 1;
    const itemsPayload = reordered.map((c, idx) => ({
      id: parseInt(c.id.replace(/^c_?/, ''), 10) || (idx + 1),
      displayOrder: idx + 1
    }));

    this.http.put<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/categories/reorder`, itemsPayload)
      .pipe(catchError(() => of(null)))
      .subscribe();
  }

  restoreCategory(id: string, categoryData?: Category): Observable<any> {
    if (categoryData) {
      this.categoriesList.update(list => [...list.filter(c => c.id !== id), categoryData]);
      this.localAddedCategories.push(categoryData);
      this.saveLocalCategories();
    }

    const numericId = parseInt(id.replace(/^c_?/, ''), 10);
    if (!isNaN(numericId)) {
      return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/restaurants/1/categories/${numericId}/restore`, {}).pipe(
        catchError(() => of(null))
      );
    }
    return of(null);
  }
}
