import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map, tap } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface TicketMessageData {
  id: string | number;
  senderName?: string;
  senderRole?: string;
  message: string;
  attachments?: string;
  isInternalNote?: boolean;
  createdAt: string | Date;
}

export interface SupportTicketData {
  id: string;
  ticketNumber?: string;
  subject: string;
  description?: string;
  message?: string;
  category: 'BILLING' | 'SUBSCRIPTION' | 'TECHNICAL_ISSUE' | 'QR_PROBLEM' | 'MENU_ISSUE' | 'ORDER_ISSUE' | 'FOOD_QUALITY' | 'SERVICE_FEEDBACK' | 'KITCHEN_EQUIPMENT' | 'FEATURE_REQUEST' | 'BUG_REPORT' | 'GENERAL' | 'OTHER' | string;
  priority: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'open' | 'resolved' | 'closed' | string;
  createdAt?: string | Date;
  updatedAt?: string | Date;
  restaurantId?: number;
  restaurantName?: string;
  customerName?: string;
  customerMobile?: string;
  customerEmail?: string;
  ownerName?: string;
  ownerEmail?: string;
  createdByName?: string;
  senderRole?: string;
  tags?: string;
  escalationLevel?: string;
  isChefIssue?: boolean;
  isEscalated?: boolean;
  messages?: TicketMessageData[];
}

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private http = inject(HttpClient);

  ticketsList = signal<SupportTicketData[]>([]);
  private unseenTickets = signal<number>(0);

  unseenTicketsCount = this.unseenTickets.asReadonly();

  markTicketsAsSeen(): void {
    localStorage.setItem('last_seen_tickets_time', String(Date.now()));
    this.unseenTickets.set(0);
  }

  createTicket(payload: {
    restaurantId: string | number;
    category?: string;
    priority?: string;
    subject: string;
    description: string;
    attachments?: string;
  }): Observable<SupportTicketData> {
    const numericId = parseInt(String(payload.restaurantId).replace(/\D/g, ''), 10) || 1;
    const body = {
      category: payload.category || 'TECHNICAL_ISSUE',
      priority: (payload.priority || 'MEDIUM').toUpperCase(),
      subject: payload.subject,
      description: payload.description,
      attachments: payload.attachments || ''
    };

    return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/tickets/restaurants/${numericId}`, body).pipe(
      map((res: ApiResponse<any>) => {
        const t = res?.data;
        const item: SupportTicketData = {
          id: t?.id ? String(t.id) : 'tk_' + Date.now(),
          ticketNumber: t?.ticketNumber || 'TICK-' + Date.now().toString().slice(-6),
          subject: t?.subject || payload.subject,
          description: payload.description,
          category: t?.category || payload.category || 'GENERAL',
          status: (t?.status || 'OPEN').toUpperCase() as any,
          priority: (t?.priority || payload.priority || 'MEDIUM').toUpperCase() as any,
          restaurantName: t?.restaurant?.name || 'Restaurant',
          ownerName: t?.createdByUser?.name || 'Staff',
          createdAt: t?.createdAt ? new Date(t.createdAt) : new Date()
        };
        this.ticketsList.update(list => [item, ...list]);
        return item;
      }),
      catchError(() => {
        const item: SupportTicketData = {
          id: 'tk_' + Date.now(),
          ticketNumber: 'TICK-' + Date.now().toString().slice(-6),
          subject: payload.subject,
          description: payload.description,
          category: payload.category || 'GENERAL',
          status: 'OPEN',
          priority: (payload.priority || 'MEDIUM').toUpperCase() as any,
          createdAt: new Date()
        };
        this.ticketsList.update(list => [item, ...list]);
        return of(item);
      })
    );
  }

  createCustomerTicket(payload: {
    restaurantId: string | number;
    customerName?: string;
    customerMobile: string;
    customerEmail?: string;
    category?: string;
    subject: string;
    description: string;
    attachments?: string;
  }): Observable<SupportTicketData> {
    const numericId = parseInt(String(payload.restaurantId).replace(/\D/g, ''), 10) || 1;
    const body = {
      restaurantId: numericId,
      customerName: payload.customerName || 'Dining Customer',
      customerMobile: payload.customerMobile,
      customerEmail: payload.customerEmail || '',
      category: payload.category || 'SERVICE_FEEDBACK',
      subject: payload.subject,
      description: payload.description,
      attachments: payload.attachments || ''
    };

    return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/tickets/public/restaurants/${numericId}`, body).pipe(
      map(res => {
        const t = res?.data;
        const item: SupportTicketData = {
          id: t?.id ? String(t.id) : 'cust_tk_' + Date.now(),
          ticketNumber: t?.ticketNumber || 'CUST-' + Date.now().toString().slice(-6),
          subject: t?.subject || payload.subject,
          description: payload.description,
          category: t?.category || payload.category || 'SERVICE_FEEDBACK',
          status: 'OPEN',
          priority: 'MEDIUM',
          customerName: payload.customerName,
          customerMobile: payload.customerMobile,
          createdAt: t?.createdAt ? new Date(t.createdAt) : new Date()
        };
        return item;
      }),
      catchError(() => of({
        id: 'cust_tk_' + Date.now(),
        ticketNumber: 'CUST-' + Date.now().toString().slice(-6),
        subject: payload.subject,
        description: payload.description,
        category: payload.category || 'SERVICE_FEEDBACK',
        status: 'OPEN',
        priority: 'MEDIUM',
        customerName: payload.customerName,
        customerMobile: payload.customerMobile,
        createdAt: new Date()
      }))
    );
  }

  fetchCustomerTickets(restaurantId: string | number, mobile: string): Observable<SupportTicketData[]> {
    const numericId = parseInt(String(restaurantId).replace(/\D/g, ''), 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/tickets/public/track?restaurantId=${numericId}&mobile=${encodeURIComponent(mobile)}`).pipe(
      map(res => {
        if (res && res.success && Array.isArray(res.data)) {
          return res.data.map(t => this.mapTicket(t));
        }
        return [];
      }),
      catchError(() => of([]))
    );
  }

  fetchOwnerTickets(restaurantId: string | number): Observable<SupportTicketData[]> {
    const numericId = parseInt(String(restaurantId).replace(/\D/g, ''), 10) || 1;
    return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/tickets/restaurants/${numericId}`).pipe(
      map(res => {
        const dataArray = res?.data?.content || res?.data;
        if (Array.isArray(dataArray)) {
          const mapped = dataArray.map(t => this.mapTicket(t));
          this.ticketsList.set(mapped);

          const lastSeen = parseInt(localStorage.getItem('last_seen_tickets_time') || '0', 10);
          const unseen = mapped.filter(t => new Date(t.createdAt || '').getTime() > lastSeen).length;
          this.unseenTickets.set(unseen);

          return mapped;
        }
        return [];
      }),
      catchError(() => of(this.ticketsList()))
    );
  }

  fetchAdminTickets(): Observable<SupportTicketData[]> {
    return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/tickets/admin/all`).pipe(
      map(res => {
        const dataArray = res?.data?.content || res?.data;
        if (Array.isArray(dataArray)) {
          const mapped = dataArray.map(t => this.mapTicket(t));
          this.ticketsList.set(mapped);

          const lastSeen = parseInt(localStorage.getItem('last_seen_tickets_time') || '0', 10);
          const unseen = mapped.filter(t => new Date(t.createdAt || '').getTime() > lastSeen && (t.status === 'OPEN' || t.status === 'IN_PROGRESS')).length;
          this.unseenTickets.set(unseen);

          return mapped;
        }
        return this.ticketsList();
      }),
      catchError(() => of(this.ticketsList()))
    );
  }

  getTicketDetails(ticketId: string | number, isPublic: boolean = false): Observable<{ ticket: SupportTicketData; messages: TicketMessageData[] } | null> {
    const url = isPublic
      ? `${environment.apiUrl}/tickets/public/${ticketId}`
      : `${environment.apiUrl}/tickets/${ticketId}`;

    return this.http.get<ApiResponse<any>>(url).pipe(
      map(res => {
        if (res && res.success && res.data) {
          const rawTicket = res.data.ticket;
          const rawMessages = res.data.messages || [];
          return {
            ticket: this.mapTicket(rawTicket),
            messages: rawMessages.map((m: any) => ({
              id: m.id,
              senderName: m.senderUser?.name || m.senderName || 'User',
              senderRole: m.senderRole || m.senderUser?.role || 'CUSTOMER',
              message: m.message,
              attachments: m.attachments,
              isInternalNote: m.isInternalNote,
              createdAt: m.createdAt ? new Date(m.createdAt) : new Date()
            }))
          };
        }
        return null;
      }),
      catchError(() => of(null))
    );
  }

  addMessage(ticketId: string | number, message: string, senderName?: string, isPublic: boolean = false): Observable<TicketMessageData | null> {
    const url = isPublic
      ? `${environment.apiUrl}/tickets/public/${ticketId}/messages`
      : `${environment.apiUrl}/tickets/${ticketId}/messages`;

    const body = isPublic
      ? { senderName: senderName || 'Customer', message }
      : { message, isInternalNote: false };

    return this.http.post<ApiResponse<any>>(url, body).pipe(
      map(res => {
        if (res && res.success && res.data) {
          const m = res.data;
          return {
            id: m.id,
            senderName: m.senderUser?.name || m.senderName || senderName || 'User',
            senderRole: m.senderRole || 'OWNER',
            message: m.message,
            attachments: m.attachments,
            isInternalNote: m.isInternalNote,
            createdAt: m.createdAt ? new Date(m.createdAt) : new Date()
          };
        }
        return null;
      }),
      catchError(() => of(null))
    );
  }

  updateTicketStatus(ticketId: string | number, status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'): Observable<boolean> {
    const statusUpper = status.toUpperCase();
    this.ticketsList.update(list =>
      list.map(t => String(t.id) === String(ticketId) ? { ...t, status: statusUpper as any } : t)
    );

    return this.http.patch<ApiResponse<any>>(`${environment.apiUrl}/tickets/${ticketId}/status?status=${statusUpper}`, {}).pipe(
      map(res => res && res.success),
      catchError(() => of(true))
    );
  }

  resolveTicket(id: string): Observable<boolean> {
    return this.updateTicketStatus(id, 'RESOLVED');
  }

  reopenTicket(id: string): Observable<boolean> {
    return this.updateTicketStatus(id, 'OPEN');
  }

  escalateTicket(ticketId: string | number, reason?: string): Observable<boolean> {
    this.ticketsList.update(list =>
      list.map(t => String(t.id) === String(ticketId) ? { ...t, isEscalated: true, status: 'IN_PROGRESS' } : t)
    );

    return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/tickets/${ticketId}/escalate`, { reason }).pipe(
      map(res => res && res.success),
      catchError(() => of(true))
    );
  }

  private mapTicket(t: any): SupportTicketData {
    const rawTags = t?.tags || '';
    const creatorRole = t?.createdByUser?.role || '';
    const isChef = rawTags.includes('CHEF_ISSUE') || creatorRole === 'CHEF' || t?.category === 'KITCHEN_EQUIPMENT';
    const isEscalated = rawTags.includes('ESCALATED_TO_ADMIN') || t?.escalationLevel === 'LEVEL_2';

    return {
      id: String(t?.id),
      ticketNumber: t?.ticketNumber || 'TICK-' + String(t?.id),
      subject: t?.subject || 'Support Ticket',
      description: t?.description || t?.feedback || '',
      category: t?.category || 'GENERAL',
      status: (t?.status || 'OPEN').toUpperCase() as any,
      priority: (t?.priority || (isChef ? 'HIGH' : 'MEDIUM')).toUpperCase() as any,
      restaurantId: t?.restaurant?.id,
      restaurantName: t?.restaurant?.name || 'Restaurant',
      customerName: t?.customerName || t?.createdByUser?.name || 'Guest',
      customerMobile: t?.customerMobile || '',
      customerEmail: t?.customerEmail || '',
      ownerName: t?.createdByUser?.name || 'Owner',
      createdByName: t?.createdByUser?.name || t?.customerName || 'Staff',
      senderRole: creatorRole || (t?.customerMobile ? 'CUSTOMER' : 'STAFF'),
      tags: rawTags,
      escalationLevel: t?.escalationLevel || 'LEVEL_1',
      isChefIssue: isChef,
      isEscalated: isEscalated,
      createdAt: t?.createdAt ? new Date(t.createdAt) : new Date(),
      updatedAt: t?.updatedAt ? new Date(t.updatedAt) : new Date()
    };
  }
}
