import { Component, inject, signal, computed, HostListener, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, AppNotification } from '../../services/notification.service';

@Component({
  selector: 'app-notification-center',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-center.html',
  styleUrls: ['./notification-center.css']
})
export class NotificationCenter {
  notificationService = inject(NotificationService);
  private elementRef = inject(ElementRef);

  activeFilter = signal<'all' | 'unread' | 'orders'>('all');

  unreadCount = computed(() => this.notificationService.unreadCount());
  isOpen = computed(() => this.notificationService.isDropdownOpen());

  filteredNotifications = computed(() => {
    const list = this.notificationService.notificationsList();
    const filter = this.activeFilter();

    if (filter === 'unread') {
      return list.filter(n => !n.isRead);
    } else if (filter === 'orders') {
      return list.filter(n => n.eventType.includes('ORDER'));
    }
    return list;
  });

  toggle() {
    this.notificationService.toggleDropdown();
    if (this.isOpen()) {
      this.notificationService.fetchNotifications().subscribe();
    }
  }

  markAsRead(event: Event, notif: AppNotification) {
    event.stopPropagation();
    if (!notif.isRead) {
      this.notificationService.markAsRead(notif.id).subscribe();
    }
  }

  markAllAsRead() {
    this.notificationService.markAllAsRead().subscribe();
  }

  deleteNotification(event: Event, notif: AppNotification) {
    event.stopPropagation();
    this.notificationService.deleteNotification(notif.id).subscribe();
  }

  getEventIcon(type: string): string {
    switch (type) {
      case 'NEW_ORDER':
      case 'ORDER_STATUS_CHANGED':
      case 'ORDER_READY':
        return 'receipt_long';
      case 'PAYMENT_RECEIVED':
        return 'payments';
      case 'QR_GENERATED':
        return 'qr_code_2';
      case 'SUPPORT_TICKET_UPDATE':
        return 'support_agent';
      case 'SECURITY_ALERT':
        return 'security';
      default:
        return 'notifications';
    }
  }

  getEventColor(type: string): string {
    switch (type) {
      case 'NEW_ORDER':
      case 'ORDER_READY':
        return '#ff6b35';
      case 'PAYMENT_RECEIVED':
        return '#10b981';
      case 'QR_GENERATED':
        return '#3b82f6';
      case 'SUPPORT_TICKET_UPDATE':
        return '#8b5cf6';
      case 'SECURITY_ALERT':
        return '#ef4444';
      default:
        return '#64748b';
    }
  }

  formatTime(isoString?: string): string {
    if (!isoString) return 'Just now';
    try {
      const diff = Math.floor((Date.now() - new Date(isoString).getTime()) / 1000);
      if (diff < 60) return 'Just now';
      if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
      if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
      return `${Math.floor(diff / 86400)}d ago`;
    } catch {
      return 'Recent';
    }
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: Event) {
    if (this.isOpen() && !this.elementRef.nativeElement.contains(event.target)) {
      this.notificationService.closeDropdown();
    }
  }
}
