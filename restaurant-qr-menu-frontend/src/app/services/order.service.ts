import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, catchError, map } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface OrderItem {
  id?: string | number;
  name: string;
  itemName?: string;
  qty: number;
  quantity?: number;
  price?: number;
  subtotal?: number;
  note?: string;
  notes?: string;
}

export interface Order {
  id: string;
  orderNumber?: string;
  table?: string;
  tableNumber: string | number;
  customerMobile: string;
  customerName?: string;
  placedAt: string | Date;
  createdAt?: string | Date;
  status: 'PENDING' | 'ACCEPTED' | 'PREPARING' | 'COOKING' | 'READY' | 'COMPLETED' | 'DELIVERED' | 'CANCELLED' | 'pending' | 'preparing' | 'done' | string;
  items: OrderItem[];
  totalAmount?: number;
  totalPrice?: number;
  specialRequest?: string;
  specialInstructions?: string;
  waiterName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private http = inject(HttpClient);
  private ordersList = signal<Order[]>([]);
  private unseenOrders = signal<number>(0);

  unseenOrdersCount = this.unseenOrders.asReadonly();

  constructor() {
    this.loadOrders();
  }

  private getStorageKey(): string {
    return 'aura_kitchen_orders';
  }

  private loadOrders(): void {
    try {
      const stored = localStorage.getItem(this.getStorageKey());
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed) && parsed.length > 0) {
          this.ordersList.set(parsed);
          return;
        }
      }
    } catch (e) {
      console.warn('Could not load orders from storage', e);
    }
    this.ordersList.set([]);
  }

  private saveOrders(list: Order[]): void {
    try {
      localStorage.setItem(this.getStorageKey(), JSON.stringify(list));
    } catch (e) {
      console.warn('Could not save orders to storage', e);
    }
  }

  getOrders(): Order[] {
    return this.ordersList();
  }

  ordersSignal() {
    return this.ordersList.asReadonly();
  }

  markOrdersAsSeen(restaurantId: string | number = 1): void {
    const numericRestId = parseInt(String(restaurantId), 10) || 1;
    localStorage.setItem(`last_seen_orders_time_${numericRestId}`, String(Date.now()));
    this.unseenOrders.set(0);
  }

  fetchOrders(restaurantId: string | number = 1): Observable<Order[]> {
    const numericRestId = parseInt(String(restaurantId), 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/restaurants/${numericRestId}/orders`).pipe(
      map(res => {
        if (res && res.success && Array.isArray(res.data)) {
          const mapped: Order[] = res.data.map(o => ({
            id: String(o.id || o.orderNumber),
            orderNumber: o.orderNumber || String(o.id),
            table: 'Table ' + (o.tableNumber || '01'),
            tableNumber: String(o.tableNumber || '01'),
            customerMobile: o.customerMobile || 'N/A',
            customerName: o.customerName || 'Customer',
            placedAt: o.createdAt || o.placedAt || new Date().toISOString(),
            status: (o.status || 'PENDING').toUpperCase() as any,
            totalAmount: Number(o.totalAmount || 0),
            items: Array.isArray(o.items) ? o.items.map((i: any) => ({
              name: i.itemName || i.name,
              qty: i.quantity || i.qty || 1,
              price: Number(i.price || 0),
              subtotal: Number(i.subtotal || (i.price * i.quantity) || 0),
              note: i.notes || i.note
            })) : [],
            specialRequest: o.specialInstructions || 'Self-Order QR'
          }));
          this.ordersList.set(mapped);
          this.saveOrders(mapped);

          const lastSeen = parseInt(localStorage.getItem(`last_seen_orders_time_${numericRestId}`) || '0', 10);
          const unseen = mapped.filter(o => new Date(o.placedAt).getTime() > lastSeen && (o.status === 'PENDING' || o.status === 'PREPARING')).length;
          this.unseenOrders.set(unseen);

          return mapped;
        }
        return this.getOrders();
      }),
      catchError(() => of(this.getOrders()))
    );
  }

  createOrder(orderPayload: {
    restaurantId?: number;
    restaurantSlug?: string;
    tableNumber: string;
    customerMobile: string;
    customerName?: string;
    specialInstructions?: string;
    items: Array<{ menuItemId?: number; name: string; price: number; qty: number; note?: string }>;
  }): Observable<Order> {
    const tempId = 'ORD-' + Math.random().toString(36).substring(2, 8).toUpperCase();
    const calculatedTotal = orderPayload.items.reduce((sum, item) => sum + (item.price * item.qty), 0);

    const localOrder: Order = {
      id: tempId,
      orderNumber: tempId,
      table: 'Table ' + orderPayload.tableNumber,
      tableNumber: orderPayload.tableNumber,
      customerMobile: orderPayload.customerMobile,
      customerName: orderPayload.customerName || 'Customer',
      placedAt: new Date().toISOString(),
      status: 'PENDING',
      totalAmount: calculatedTotal,
      items: orderPayload.items.map(i => ({
        name: i.name,
        qty: i.qty,
        price: i.price,
        subtotal: i.price * i.qty,
        note: i.note
      })),
      specialRequest: orderPayload.specialInstructions || 'Self-Order QR',
      waiterName: 'Self-Order QR'
    };

    this.ordersList.update(list => [localOrder, ...list]);
    this.saveOrders(this.ordersList());

    const body = {
      restaurantId: orderPayload.restaurantId || 1,
      restaurantSlug: orderPayload.restaurantSlug || 'gourmet-bistro',
      tableNumber: orderPayload.tableNumber,
      customerMobile: orderPayload.customerMobile,
      customerName: orderPayload.customerName || 'Customer',
      specialInstructions: orderPayload.specialInstructions || '',
      items: orderPayload.items.map(i => ({
        menuItemId: i.menuItemId,
        itemName: i.name,
        price: i.price,
        quantity: i.qty,
        notes: i.note || ''
      }))
    };

    return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/public/orders`, body).pipe(
      map(res => {
        if (res && res.success && res.data) {
          const serverOrder = res.data;
          const updated: Order = {
            id: String(serverOrder.id || serverOrder.orderNumber),
            orderNumber: serverOrder.orderNumber || tempId,
            table: 'Table ' + (serverOrder.tableNumber || orderPayload.tableNumber),
            tableNumber: serverOrder.tableNumber || orderPayload.tableNumber,
            customerMobile: serverOrder.customerMobile,
            customerName: serverOrder.customerName || 'Customer',
            placedAt: serverOrder.createdAt || new Date().toISOString(),
            status: (serverOrder.status || 'PENDING').toUpperCase() as any,
            totalAmount: Number(serverOrder.totalAmount || calculatedTotal),
            items: localOrder.items,
            specialRequest: orderPayload.specialInstructions
          };

          this.ordersList.update(list => list.map(o => o.id === tempId ? updated : o));
          this.saveOrders(this.ordersList());
          return updated;
        }
        return localOrder;
      }),
      catchError(() => of(localOrder))
    );
  }

  trackOrders(identifier: string): Observable<Order[]> {
    if (!identifier || identifier.trim().length === 0) return of([]);

    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/public/orders/track?identifier=${encodeURIComponent(identifier)}`).pipe(
      map(res => {
        if (res && res.success && Array.isArray(res.data) && res.data.length > 0) {
          return res.data.map(o => ({
            id: String(o.id || o.orderNumber),
            orderNumber: o.orderNumber || String(o.id),
            table: 'Table ' + (o.tableNumber || '01'),
            tableNumber: String(o.tableNumber || '01'),
            customerMobile: o.customerMobile || 'N/A',
            customerName: o.customerName || 'Customer',
            placedAt: o.createdAt || new Date().toISOString(),
            status: (o.status || 'PENDING').toUpperCase() as any,
            totalAmount: Number(o.totalAmount || 0),
            items: Array.isArray(o.items) ? o.items.map((i: any) => ({
              name: i.itemName || i.name,
              qty: i.quantity || i.qty || 1,
              price: Number(i.price || 0),
              subtotal: Number(i.subtotal || 0),
              note: i.notes || i.note
            })) : []
          }));
        }
        // Fallback local match
        const cleanId = identifier.trim().toLowerCase();
        return this.ordersList().filter(o =>
          o.id.toLowerCase() === cleanId ||
          (o.orderNumber && o.orderNumber.toLowerCase() === cleanId) ||
          o.customerMobile.includes(cleanId)
        );
      }),
      catchError(() => {
        const cleanId = identifier.trim().toLowerCase();
        return of(this.ordersList().filter(o =>
          o.id.toLowerCase() === cleanId ||
          (o.orderNumber && o.orderNumber.toLowerCase() === cleanId) ||
          o.customerMobile.includes(cleanId)
        ));
      })
    );
  }

  updateOrderStatus(orderId: string, newStatus: string, restaurantId: string | number = 1): Observable<boolean> {
    const statusUpper = newStatus.toUpperCase();
    this.ordersList.update(list =>
      list.map(o => (o.id === orderId || o.orderNumber === orderId) ? { ...o, status: statusUpper as any } : o)
    );
    this.saveOrders(this.ordersList());

    const numericRestId = parseInt(String(restaurantId), 10) || 1;
    const cleanId = encodeURIComponent(orderId.trim());

    return this.http.patch<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${numericRestId}/orders/${cleanId}/status`, { status: statusUpper })
      .pipe(
        map(res => res && res.success),
        catchError(() => of(true))
      );
  }
}
