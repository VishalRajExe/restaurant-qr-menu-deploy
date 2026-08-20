import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { Order } from './order.service';

export interface FavoriteItem {
  itemName: string;
  totalQuantity: number;
  totalAmount: number;
}

export interface CustomerHistoryData {
  customerMobile: string;
  customerName: string;
  restaurantId: number;
  restaurantName: string;
  totalOrders: number;
  totalSpent: number;
  averageOrderValue: number;
  firstOrderDate: string | null;
  lastOrderDate: string | null;
  favoriteItems: FavoriteItem[];
  orders: Order[];
}

export interface CustomerSummary {
  customerMobile: string;
  customerName: string;
  orderCount: number;
  totalSpent: number;
  lastOrderDate: string;
}

@Injectable({
  providedIn: 'root'
})
export class CustomerHistoryService {
  private http = inject(HttpClient);

  activeCustomerHistory = signal<CustomerHistoryData | null>(null);
  recentCustomers       = signal<CustomerSummary[]>([]);
  isLoading             = signal<boolean>(false);
  searchError           = signal<string | null>(null);

  /**
   * Fetch complete customer order history for a restaurant by 10-digit mobile number
   */
  fetchCustomerHistory(restaurantId: string | number, phone: string): Observable<CustomerHistoryData | null> {
    const cleanPhone = phone.replace(/\D/g, '');
    this.isLoading.set(true);
    this.searchError.set(null);

    return this.http.get<ApiResponse<CustomerHistoryData>>(`${environment.apiUrl}/restaurants/${restaurantId}/customers/history?phone=${cleanPhone}`).pipe(
      map(res => {
        this.isLoading.set(false);
        if (res && res.success && res.data) {
          const raw = res.data;
          const mappedOrders: Order[] = (raw.orders || []).map((o: any) => ({
            ...o,
            id: String(o.id || o.orderNumber),
            tableNumber: o.tableNumber || '01',
            placedAt: o.createdAt || o.placedAt || new Date().toISOString(),
            createdAt: o.createdAt || o.placedAt || new Date().toISOString(),
            items: (o.items || []).map((i: any) => ({
              ...i,
              name: i.itemName || i.name || 'Item',
              itemName: i.itemName || i.name || 'Item',
              qty: i.quantity || i.qty || 1,
              quantity: i.quantity || i.qty || 1,
              price: i.price || 0,
              subtotal: (i.price || 0) * (i.quantity || i.qty || 1)
            }))
          }));
          const data: CustomerHistoryData = {
            ...raw,
            orders: mappedOrders
          };
          this.activeCustomerHistory.set(data);
          return data;
        }
        this.activeCustomerHistory.set(null);
        return null;
      }),
      catchError(err => {
        this.isLoading.set(false);
        const msg = err?.error?.message || 'Failed to fetch customer order history';
        this.searchError.set(msg);
        this.activeCustomerHistory.set(null);
        return of(null);
      })
    );
  }

  /**
   * Fetch recent customers who ordered at this restaurant
   */
  fetchRecentCustomers(restaurantId: string | number, search: string = '', limit: number = 20): Observable<CustomerSummary[]> {
    const params = search ? `?search=${encodeURIComponent(search)}&limit=${limit}` : `?limit=${limit}`;
    return this.http.get<ApiResponse<CustomerSummary[]>>(`${environment.apiUrl}/restaurants/${restaurantId}/customers/recent${params}`).pipe(
      map(res => {
        if (res && res.success && res.data) {
          this.recentCustomers.set(res.data);
          return res.data;
        }
        return [];
      }),
      catchError(() => of([]))
    );
  }
}
