import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../environments/environment';

export interface OrderItem {
  name: string;
  qty: number;
  note?: string;
  notes?: string;
}

export interface Order {
  id: string;
  table: string;
  tableNumber?: number | string;
  placedAt: Date;
  status: 'pending' | 'preparing' | 'done';
  items: OrderItem[];
  specialRequest?: string;
  waiterName?: string;
}

@Component({
  selector: 'app-chef-dashboard',
  imports: [CommonModule, RouterLink],
  templateUrl: './chef-dashboard.html',
})
export class ChefDashboard implements OnInit, OnDestroy {
  authService = inject(AuthService);
  http        = inject(HttpClient);
  router      = inject(Router);

  orders = signal<Order[]>([]);

  activeFilter = signal<'all' | 'pending' | 'preparing' | 'done'>('all');

  filterTabs: Array<'all' | 'pending' | 'preparing' | 'done'> = ['all', 'pending', 'preparing', 'done'];

  filteredOrders = computed(() => {
    const f = this.activeFilter();
    if (f === 'all') return this.orders();
    return this.orders().filter(o => o.status === f);
  });

  pendingCount   = computed(() => this.orders().filter(o => o.status === 'pending').length);
  preparingCount = computed(() => this.orders().filter(o => o.status === 'preparing').length);
  doneCount      = computed(() => this.orders().filter(o => o.status === 'done').length);

  private timerInterval: any;
  Math = Math;

  ngOnInit() {
    this.fetchOrders();
    this.timerInterval = setInterval(() => {
      this.orders.update(o => [...o]);
    }, 10000);
  }

  fetchOrders() {
    this.http.get<any>(`${environment.apiUrl}/public/menu/gourmet-bistro`).subscribe({
      next: (res) => {
        if (res && res.data && res.data.menuItems) {
          if (this.orders().length === 0) {
            const items = res.data.menuItems.slice(0, 3).map((item: any) => ({
              name: item.name,
              qty: 2,
              note: item.isVeg ? 'Vegetarian' : 'Medium Spice',
              notes: item.isVeg ? 'Vegetarian' : 'Medium Spice'
            }));
            this.orders.set([
              {
                id: 'ORD-101',
                table: 'Table 05',
                tableNumber: '05',
                placedAt: new Date(Date.now() - 300000),
                status: 'pending',
                items: items,
                specialRequest: 'No cutlery required, extra napkins please.',
                waiterName: 'Self-Order QR'
              }
            ]);
          }
        }
      },
      error: () => {
        if (this.orders().length === 0) {
          this.orders.set([
            {
              id: 'ORD-101',
              table: 'Table 05',
              tableNumber: '05',
              placedAt: new Date(Date.now() - 300000),
              status: 'pending',
              items: [
                { name: 'Truffle Mushroom Burger', qty: 2, note: 'Extra Swiss Cheese', notes: 'Extra Swiss Cheese' },
                { name: 'Artisanal French Fries', qty: 1 }
              ],
              specialRequest: 'No cutlery required, extra napkins please.',
              waiterName: 'Self-Order QR'
            }
          ]);
        }
      }
    });
  }

  ngOnDestroy() {
    clearInterval(this.timerInterval);
  }

  advanceOrder(orderId: string) {
    this.orders.update(list =>
      list.map(o => {
        if (o.id !== orderId) return o;
        const next = o.status === 'pending' ? 'preparing' : 'done';
        return { ...o, status: next };
      })
    );
  }

  timeAgo(date: Date): string {
    const mins = Math.floor((Date.now() - date.getTime()) / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    return `${Math.floor(mins / 60)}h ${mins % 60}m ago`;
  }

  estimatedMins(order: Order): number {
    const count = order.items.reduce((s, i) => s + i.qty, 0);
    if (count <= 2) return 15;
    if (count <= 5) return 25;
    return 35;
  }

  elapsedMins(date: Date): number {
    return Math.floor((Date.now() - date.getTime()) / 60000);
  }

  remainingMins(order: Order): number {
    return this.estimatedMins(order) - this.elapsedMins(order.placedAt);
  }

  statusColor(status: Order['status']): string {
    return {
      pending:   'bg-amber-500/15 text-amber-400 border-amber-500/20',
      preparing: 'bg-blue-500/15  text-blue-400  border-blue-500/20',
      done:      'bg-green-500/15 text-green-400 border-green-500/20',
    }[status];
  }

  advanceLabel(status: Order['status']): string {
    return status === 'pending' ? 'Start Preparing' : 'Mark as Done';
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
