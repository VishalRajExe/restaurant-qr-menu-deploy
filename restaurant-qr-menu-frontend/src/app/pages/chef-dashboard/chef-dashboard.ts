import { Component, inject, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../services/toast.service';
import { ModalService } from '../../services/modal.service';
import { OrderService, Order } from '../../services/order.service';
import { BackButton } from '../../components/back-button/back-button';

@Component({
  selector: 'app-chef-dashboard',
  imports: [CommonModule, RouterLink, BackButton],
  templateUrl: './chef-dashboard.html',
})
export class ChefDashboard implements OnInit, OnDestroy {
  authService  = inject(AuthService);
  toastService = inject(ToastService);
  modalService = inject(ModalService);
  orderService = inject(OrderService);
  http         = inject(HttpClient);
  router       = inject(Router);

  currentTime  = signal<number>(Date.now());
  activeFilter = signal<'all' | 'pending' | 'preparing' | 'done'>('all');
  filterTabs: Array<'all' | 'pending' | 'preparing' | 'done'> = ['all', 'pending', 'preparing', 'done'];

  orders = computed(() => this.orderService.ordersSignal()());

  filteredOrders = computed(() => {
    const f = this.activeFilter();
    const list = this.orders();
    if (f === 'all') return list;
    return list.filter(o => o.status === f);
  });

  pendingCount   = computed(() => this.orders().filter(o => o.status === 'pending').length);
  preparingCount = computed(() => this.orders().filter(o => o.status === 'preparing').length);
  doneCount      = computed(() => this.orders().filter(o => o.status === 'done').length);

  private clockTicker: any;
  Math = Math;

  ngOnInit() {
    this.clockTicker = setInterval(() => {
      this.currentTime.set(Date.now());
    }, 1000);
  }

  ngOnDestroy() {
    if (this.clockTicker) {
      clearInterval(this.clockTicker);
    }
  }

  advanceOrder(orderId: string) {
    const target = this.orders().find(o => o.id === orderId);
    if (!target) return;
    const nextStatus = target.status === 'pending' ? 'preparing' : 'done';
    this.orderService.updateOrderStatus(orderId, nextStatus);
    this.toastService.success('Order Status Updated', `Order ${orderId} moved to ${nextStatus.toUpperCase()}`);
  }

  parseDate(date: string | Date): Date {
    return typeof date === 'string' ? new Date(date) : date;
  }

  timeAgo(date: string | Date): string {
    const d = this.parseDate(date);
    const diffSec = Math.floor((this.currentTime() - d.getTime()) / 1000);
    if (diffSec < 10) return 'just now';
    if (diffSec < 60) return `${diffSec}s ago`;
    const mins = Math.floor(diffSec / 60);
    if (mins < 60) return `${mins}m ago`;
    return `${Math.floor(mins / 60)}h ${mins % 60}m ago`;
  }

  estimatedMins(order: Order): number {
    const count = order.items.reduce((s, i) => s + i.qty, 0);
    if (count <= 2) return 15;
    if (count <= 5) return 25;
    return 35;
  }

  elapsedSeconds(date: string | Date): number {
    const d = this.parseDate(date);
    return Math.max(0, Math.floor((this.currentTime() - d.getTime()) / 1000));
  }

  remainingSeconds(order: Order): number {
    const targetSec = this.estimatedMins(order) * 60;
    return targetSec - this.elapsedSeconds(order.placedAt);
  }

  formatCountdown(order: Order): { text: string; isOverdue: boolean } {
    const rem = this.remainingSeconds(order);
    if (rem >= 0) {
      const m = Math.floor(rem / 60);
      const s = rem % 60;
      return { text: `${m}m ${s < 10 ? '0' : ''}${s}s left`, isOverdue: false };
    } else {
      const over = Math.abs(rem);
      const m = Math.floor(over / 60);
      const s = over % 60;
      return { text: `${m}m ${s < 10 ? '0' : ''}${s}s overdue`, isOverdue: true };
    }
  }

  progressPercent(order: Order): number {
    const totalSec = this.estimatedMins(order) * 60;
    const elapsedSec = this.elapsedSeconds(order.placedAt);
    return Math.min(100, Math.max(0, Math.floor((elapsedSec / totalSec) * 100)));
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
    this.modalService.confirm({
      title: 'Sign Out Kitchen Session',
      message: 'Are you sure you want to log out of the Chef Kitchen panel?',
      type: 'warning',
      confirmText: 'Sign Out',
      onConfirm: () => {
        this.authService.logout();
        this.toastService.info('Signed Out', 'Chef session ended.');
        this.router.navigate(['/login']);
      }
    });
  }
}
