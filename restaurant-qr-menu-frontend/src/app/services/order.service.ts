import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, catchError } from 'rxjs';
import { environment } from '../environments/environment';

export interface OrderItem {
  name: string;
  qty: number;
  note?: string;
  notes?: string;
}

export interface Order {
  id: string;
  table: string;
  tableNumber: string | number;
  placedAt: string | Date;
  status: 'pending' | 'preparing' | 'done';
  items: OrderItem[];
  specialRequest?: string;
  waiterName?: string;
}

@Injectable({
  providedIn: 'root'
})
export class OrderService {
  private http = inject(HttpClient);
  private ordersList = signal<Order[]>([]);

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

    // Default demo seed order if empty
    const seedOrders: Order[] = [
      {
        id: 'ORD-101',
        table: 'Table 05',
        tableNumber: '05',
        placedAt: new Date(Date.now() - 300000).toISOString(),
        status: 'pending',
        items: [
          { name: 'Truffle Mushroom Burger', qty: 2, note: 'Medium Spice' },
          { name: 'Artisanal French Fries', qty: 1 }
        ],
        specialRequest: 'No cutlery required, extra napkins please.',
        waiterName: 'Self-Order QR'
      }
    ];
    this.ordersList.set(seedOrders);
    this.saveOrders(seedOrders);
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

  fetchOrders(): Observable<Order[]> {
    return of(this.getOrders());
  }

  createOrder(orderData: Partial<Order>): Order {
    const newId = orderData.id || ('ORD-' + Math.random().toString(36).substring(2, 8).toUpperCase());
    const newOrder: Order = {
      id: newId,
      table: orderData.table || 'Table 01',
      tableNumber: orderData.tableNumber || '01',
      placedAt: new Date().toISOString(),
      status: 'pending',
      items: orderData.items || [],
      specialRequest: orderData.specialRequest || 'Self-Order QR',
      waiterName: orderData.waiterName || 'Self-Order QR'
    };

    this.ordersList.update(list => [newOrder, ...list]);
    this.saveOrders(this.ordersList());
    return newOrder;
  }

  updateOrderStatus(orderId: string, status: 'pending' | 'preparing' | 'done'): void {
    this.ordersList.update(list =>
      list.map(o => o.id === orderId ? { ...o, status } : o)
    );
    this.saveOrders(this.ordersList());
  }
}
