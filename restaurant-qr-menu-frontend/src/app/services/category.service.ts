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

  getCategoriesForRestaurant(restaurantId: string): Category[] {
    return this.categoriesList()
      .filter(c => c.restaurantId === restaurantId || !c.restaurantId)
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  }

  /** Called by PublicMenuService to set categories from unified endpoint */
  setCategories(categories: Category[]): void {
    this.categoriesList.set(categories);
  }

  fetchCategories(restaurantId: string): Observable<Category[]> {
    const numericId = parseInt(restaurantId, 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/restaurants/${numericId}/categories`).pipe(
      map(res => {
        if (res && res.success && Array.isArray(res.data)) {
          const mapped: Category[] = res.data.map(item => ({
            id: String(item.id),
            restaurantId: String(restaurantId),
            name: item.name,
            icon: item.icon || 'Utensils',
            sortOrder: item.displayOrder || item.sortOrder || 1
          }));
          this.categoriesList.set(mapped);
          return mapped;
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
    const newCategory: Category = {
      ...category,
      id: 'c' + (this.categoriesList().length + 1),
      sortOrder: this.categoriesList().length + 1
    };
    this.categoriesList.update(list => [...list, newCategory]);

    const numericRestId = parseInt(category.restaurantId, 10) || 1;
    const body = {
      name: category.name,
      displayOrder: newCategory.sortOrder
    };

    this.http.post<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${numericRestId}/categories`, body)
      .pipe(catchError(() => of(null)))
      .subscribe();

    return newCategory;
  }

  updateCategory(id: string, name: string, icon?: string) {
    this.categoriesList.update(list =>
      list.map(c => c.id === id ? { ...c, name, icon } : c)
    );

    const numericId = parseInt(id, 10);
    if (!isNaN(numericId)) {
      const restId = 1;
      this.http.put<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/categories/${numericId}`, { name, displayOrder: 1 })
        .pipe(catchError(() => of(null)))
        .subscribe();
    }
  }

  deleteCategory(id: string) {
    this.categoriesList.update(list => list.filter(c => c.id !== id));

    const numericId = parseInt(id, 10);
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
      id: parseInt(c.id, 10) || (idx + 1),
      displayOrder: idx + 1
    }));

    this.http.put<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${restId}/categories/reorder`, itemsPayload)
      .pipe(catchError(() => of(null)))
      .subscribe();
  }
}
