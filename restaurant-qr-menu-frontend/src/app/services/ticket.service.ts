import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface SupportTicketData {
  id: string;
  subject: string;
  description: string;
  message?: string;
  status: 'open' | 'resolved';
  priority: 'low' | 'medium' | 'high' | 'CRITICAL';
  createdAt?: string | Date;
  restaurantName?: string;
  ownerName?: string;
  ownerEmail?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private http = inject(HttpClient);

  // Initialize with empty array — populated from live API
  ticketsList = signal<SupportTicketData[]>([]);

  createTicket(restaurantId: string, subject: string, description: string, priority: string = 'medium'): Observable<SupportTicketData> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    const body = {
      category: 'QR_PROBLEM',
      priority: priority.toUpperCase(),
      subject,
      description
    };

    return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/tickets/restaurants/${numericId}`, body).pipe(
      map((res: ApiResponse<any>) => {
        const item: SupportTicketData = {
          id: res?.data?.id ? String(res.data.id) : 'tk_' + Date.now(),
          subject: res?.data?.subject || subject,
          description: res?.data?.description || description,
          message: res?.data?.description || description,
          status: 'open',
          priority: (res?.data?.priority || priority).toLowerCase() as any,
          restaurantName: res?.data?.restaurantName || 'My Restaurant',
          ownerName: res?.data?.createdByName || 'Owner',
          ownerEmail: res?.data?.createdByEmail || '',
          createdAt: res?.data?.createdAt ? new Date(res.data.createdAt) : new Date()
        };
        this.ticketsList.update((list: SupportTicketData[]) => [item, ...list]);
        return item;
      }),
      catchError((err: { message: string }) => {
        console.warn('Create ticket failed:', err.message);
        const item: SupportTicketData = {
          id: 'tk_' + Date.now(),
          subject,
          description,
          message: description,
          status: 'open',
          priority: priority as any,
          createdAt: new Date()
        };
        this.ticketsList.update((list: SupportTicketData[]) => [item, ...list]);
        return of(item);
      })
    );
  }

  fetchAdminTickets(): Observable<SupportTicketData[]> {
    return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/admin/tickets`).pipe(
      map((res: ApiResponse<any>) => {
        const dataArray = res?.data?.content || res?.data;
        if (Array.isArray(dataArray)) {
          const mapped: SupportTicketData[] = dataArray.map((t: any) => ({
            id: String(t.id),
            subject: t.subject || 'Support Ticket',
            description: t.description || '',
            message: t.description || '',
            status: (t.status === 'RESOLVED' || t.status === 'CLOSED') ? 'resolved' : 'open',
            priority: (t.priority || 'MEDIUM').toLowerCase() as any,
            restaurantName: t.restaurantName || t.restaurant?.name || 'Restaurant Partner',
            ownerName: t.createdByName || t.ownerName || 'Partner Owner',
            ownerEmail: t.createdByEmail || t.ownerEmail || '',
            createdAt: t.createdAt ? new Date(t.createdAt) : new Date()
          }));
          this.ticketsList.set(mapped);
          return mapped;
        }
        return this.ticketsList();
      }),
      catchError((err: { message: string }) => {
        console.warn('Fetch admin tickets failed:', err.message);
        return of(this.ticketsList());
      })
    );
  }

  fetchOwnerTickets(restaurantId: string): Observable<SupportTicketData[]> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    return this.http.get<ApiResponse<any>>(`${environment.apiUrl}/tickets/restaurants/${numericId}`).pipe(
      map((res: ApiResponse<any>) => {
        const dataArray = res?.data?.content || res?.data;
        if (Array.isArray(dataArray)) {
          const mapped: SupportTicketData[] = dataArray.map((t: any) => ({
            id: String(t.id),
            subject: t.subject || 'Support Ticket',
            description: t.description || '',
            message: t.description || '',
            status: (t.status === 'RESOLVED' || t.status === 'CLOSED') ? 'resolved' : 'open',
            priority: (t.priority || 'MEDIUM').toLowerCase() as any,
            createdAt: t.createdAt ? new Date(t.createdAt) : new Date()
          }));
          return mapped;
        }
        return [];
      }),
      catchError(() => of([]))
    );
  }

  resolveTicket(id: string): Observable<boolean> {
    this.ticketsList.update((list: SupportTicketData[]) =>
      list.map((t: SupportTicketData) => t.id === id ? { ...t, status: 'resolved' as const } : t)
    );
    const numericId = parseInt(id, 10);
    if (!isNaN(numericId)) {
      return this.http.patch<ApiResponse<any>>(`${environment.apiUrl}/admin/tickets/${numericId}/resolve`, {}).pipe(
        map(() => true),
        catchError(() => of(true))
      );
    }
    return of(true);
  }

  reopenTicket(id: string): Observable<boolean> {
    this.ticketsList.update((list: SupportTicketData[]) =>
      list.map((t: SupportTicketData) => t.id === id ? { ...t, status: 'open' as const } : t)
    );
    const numericId = parseInt(id, 10);
    if (!isNaN(numericId)) {
      // Correct endpoint: PATCH /tickets/{ticketId}/reopen
      return this.http.patch<ApiResponse<any>>(`${environment.apiUrl}/tickets/${numericId}/reopen`, {}).pipe(
        map(() => true),
        catchError(() => of(true))
      );
    }
    return of(true);
  }
}
