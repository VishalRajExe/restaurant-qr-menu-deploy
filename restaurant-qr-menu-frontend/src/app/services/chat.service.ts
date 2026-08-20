import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map, catchError, of } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface ChatContact {
  userId: number;
  name: string;
  email: string;
  role: 'OWNER' | 'CHEF' | 'STAFF' | string;
  avatarUrl?: string;
  lastMessage?: string;
  lastMessageTime?: string;
  unreadCount: number;
  isOnline: boolean;
}

export interface ChatMessageItem {
  id: number;
  restaurantId: number;
  senderId: number;
  senderName: string;
  senderRole: string;
  receiverId: number;
  receiverName: string;
  receiverRole: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private http = inject(HttpClient);

  contacts = signal<ChatContact[]>([]);
  activeContact = signal<ChatContact | null>(null);
  activeThread = signal<ChatMessageItem[]>([]);
  unreadTotalCount = signal<number>(0);
  isLoadingThread = signal<boolean>(false);
  isSending = signal<boolean>(false);

  fetchContacts(restaurantId: number | string): Observable<ChatContact[]> {
    return this.http.get<ApiResponse<ChatContact[]>>(`${environment.apiUrl}/restaurants/${restaurantId}/chat/contacts`).pipe(
      map(res => res.data || []),
      tap(contacts => {
        this.contacts.set(contacts);
        const total = contacts.reduce((sum, c) => sum + (c.unreadCount || 0), 0);
        this.unreadTotalCount.set(total);
      }),
      catchError(() => of([]))
    );
  }

  loadThread(restaurantId: number | string, contact: ChatContact): Observable<ChatMessageItem[]> {
    this.activeContact.set(contact);
    this.isLoadingThread.set(true);

    return this.http.get<ApiResponse<ChatMessageItem[]>>(`${environment.apiUrl}/restaurants/${restaurantId}/chat/threads/${contact.userId}`).pipe(
      map(res => res.data || []),
      tap(messages => {
        this.activeThread.set(messages);
        this.isLoadingThread.set(false);

        // Update local contact unread
        this.contacts.update(list => list.map(c => {
          if (c.userId === contact.userId) {
            return { ...c, unreadCount: 0 };
          }
          return c;
        }));
        const total = this.contacts().reduce((sum, c) => sum + (c.unreadCount || 0), 0);
        this.unreadTotalCount.set(total);
      }),
      catchError(() => {
        this.isLoadingThread.set(false);
        return of([]);
      })
    );
  }

  refreshThreadSilently(restaurantId: number | string, contactUserId: number) {
    this.http.get<ApiResponse<ChatMessageItem[]>>(`${environment.apiUrl}/restaurants/${restaurantId}/chat/threads/${contactUserId}`).pipe(
      map(res => res.data || []),
      catchError(() => of([]))
    ).subscribe(messages => {
      this.activeThread.set(messages);
    });
  }

  sendMessage(restaurantId: number | string, receiverId: number, message: string): Observable<ChatMessageItem | null> {
    this.isSending.set(true);

    return this.http.post<ApiResponse<ChatMessageItem>>(`${environment.apiUrl}/restaurants/${restaurantId}/chat/messages`, {
      receiverId,
      message
    }).pipe(
      map(res => res.data || null),
      tap(sent => {
        this.isSending.set(false);
        if (sent) {
          this.activeThread.update(list => [...list, sent]);
          this.contacts.update(list => list.map(c => {
            if (c.userId === receiverId) {
              return { ...c, lastMessage: sent.message, lastMessageTime: sent.createdAt };
            }
            return c;
          }));
        }
      }),
      catchError(() => {
        this.isSending.set(false);
        return of(null);
      })
    );
  }

  markThreadRead(restaurantId: number | string, otherUserId: number) {
    this.http.patch(`${environment.apiUrl}/restaurants/${restaurantId}/chat/threads/${otherUserId}/read`, {}).subscribe();
  }

  fetchUnreadCount(restaurantId: number | string) {
    this.http.get<ApiResponse<{ unreadCount: number }>>(`${environment.apiUrl}/restaurants/${restaurantId}/chat/unread-count`).pipe(
      map(res => res.data?.unreadCount || 0),
      catchError(() => of(0))
    ).subscribe(count => {
      this.unreadTotalCount.set(count);
    });
  }
}
