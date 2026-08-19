import { Injectable, signal, inject, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map, tap } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { ToastService } from './toast.service';

export interface AppNotification {
  id: string | number;
  userId?: number;
  restaurantId?: number;
  eventType: string;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  channel?: string;
}

export interface NotificationInboxPayload {
  unreadCount: number;
  notifications: AppNotification[];
  totalElements: number;
  totalPages: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private http = inject(HttpClient);
  private toastService = inject(ToastService);

  private notifications = signal<AppNotification[]>([]);
  private unread = signal<number>(0);
  private isOpen = signal<boolean>(false);

  // Readonly Public Signals
  notificationsList = this.notifications.asReadonly();
  unreadCount = this.unread.asReadonly();
  isDropdownOpen = this.isOpen.asReadonly();

  constructor() {
    this.initDefaultNotifications();
  }

  toggleDropdown() {
    this.isOpen.update(v => !v);
  }

  closeDropdown() {
    this.isOpen.set(false);
  }

  openDropdown() {
    this.isOpen.set(true);
  }

  /**
   * Fetch live user notifications from backend API
   */
  fetchNotifications(): Observable<AppNotification[]> {
    return this.http.get<ApiResponse<NotificationInboxPayload>>(`${environment.apiUrl}/notifications`).pipe(
      map(res => {
        if (res && res.success && res.data) {
          const apiList = res.data.notifications || [];
          this.notifications.set(apiList);
          this.unread.set(res.data.unreadCount || 0);
          return apiList;
        }
        return this.notifications();
      }),
      catchError(err => {
        console.warn('API fetch notifications fallback:', err.message);
        return of(this.notifications());
      })
    );
  }

  /**
   * Mark a single notification as read
   */
  markAsRead(id: string | number): Observable<any> {
    // Optimistically update signal
    this.notifications.update(list =>
      list.map(n => n.id === id ? { ...n, isRead: true } : n)
    );
    this.unread.update(count => Math.max(0, count - 1));

    return this.http.patch<ApiResponse<any>>(`${environment.apiUrl}/notifications/${id}/read`, {}).pipe(
      catchError(err => {
        console.warn('Mark as read API notice:', err.message);
        return of(null);
      })
    );
  }

  /**
   * Mark all notifications as read
   */
  markAllAsRead(): Observable<any> {
    this.notifications.update(list => list.map(n => ({ ...n, isRead: true })));
    this.unread.set(0);
    this.toastService.success('All Caught Up', 'All notifications marked as read.');

    return this.http.patch<ApiResponse<any>>(`${environment.apiUrl}/notifications/read-all`, {}).pipe(
      catchError(err => {
        console.warn('Mark all read API notice:', err.message);
        return of(null);
      })
    );
  }

  /**
   * Delete a single notification
   */
  deleteNotification(id: string | number): Observable<any> {
    const wasUnread = this.notifications().find(n => n.id === id)?.isRead === false;
    this.notifications.update(list => list.filter(n => n.id !== id));
    if (wasUnread) {
      this.unread.update(count => Math.max(0, count - 1));
    }

    return this.http.delete<ApiResponse<any>>(`${environment.apiUrl}/notifications/${id}`).pipe(
      catchError(err => {
        console.warn('Delete notification API notice:', err.message);
        return of(null);
      })
    );
  }

  /**
   * Push an instant real-time notification (e.g., when an order is placed in the current session)
   */
  pushNotification(notif: { eventType: string; title: string; message: string; id?: string | number; isRead?: boolean; restaurantId?: number; userId?: number; channel?: string }): void {
    const newNotif: AppNotification = {
      id: notif.id || 'notif_' + Date.now(),
      createdAt: new Date().toISOString(),
      isRead: notif.isRead ?? false,
      ...notif
    };

    this.notifications.update(list => [newNotif, ...list]);
    if (!newNotif.isRead) {
      this.unread.update(c => c + 1);
      this.toastService.info(newNotif.title, newNotif.message);
    }
  }

  private initDefaultNotifications() {
    const defaults: AppNotification[] = [
      {
        id: '1',
        eventType: 'NEW_ORDER',
        title: 'New Order #ORD-8821',
        message: 'Table 01 placed an order for 2x Truffle Mushroom Burger + Chocolate Fondant ($46.00).',
        isRead: false,
        createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString()
      },
      {
        id: '2',
        eventType: 'PAYMENT_RECEIVED',
        title: 'Settlement Received',
        message: 'Daily dine-in revenue settlement of $951.52 processed successfully.',
        isRead: false,
        createdAt: new Date(Date.now() - 35 * 60 * 1000).toISOString()
      },
      {
        id: '3',
        eventType: 'QR_GENERATED',
        title: 'High QR Traffic Alert',
        message: 'Table 01 QR code has reached 42 active guest scans today.',
        isRead: true,
        createdAt: new Date(Date.now() - 120 * 60 * 1000).toISOString()
      }
    ];

    this.notifications.set(defaults);
    this.unread.set(defaults.filter(d => !d.isRead).length);
  }
}
