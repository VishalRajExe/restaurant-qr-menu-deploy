import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';
import { map, tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { Order } from './order.service';

export type TableStatus = 'AVAILABLE' | 'RESERVED' | 'OCCUPIED' | 'CLEANING';

export interface DiningTableData {
  id: number;
  restaurantId: number;
  restaurantName?: string;
  branchId?: number;
  branchName?: string;
  tableNumber: string;
  capacity: number;
  status: TableStatus;

  // QR Code
  qrCodeId?: number;
  qrToken?: string;
  qrImageUrl?: string;
  scanCount?: number;

  // Reservation
  reservationName?: string;
  reservationPhone?: string;
  reservationTime?: string;
  reservationGuests?: number;
  reservationNotes?: string;

  // Session & Live Orders
  activeSessionId?: string;
  sessionStartTime?: string;
  activeOrdersCount: number;
  currentTotalAmount: number;
  activeOrders?: Order[];
}

export interface TableStatsData {
  totalTables: number;
  availableTables: number;
  occupiedTables: number;
  reservedTables: number;
  cleaningTables: number;
}

export interface CreateTablePayload {
  tableNumber: string;
  capacity: number;
  branchId?: number;
  status?: TableStatus;
}

export interface ReserveTablePayload {
  guestName: string;
  guestPhone?: string;
  reservationTime: string;
  guestCount: number;
  notes?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TableService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/restaurants`;

  tablesList = signal<DiningTableData[]>([]);
  tableStats = signal<TableStatsData>({
    totalTables: 0,
    availableTables: 0,
    occupiedTables: 0,
    reservedTables: 0,
    cleaningTables: 0
  });

  fetchTables(restaurantId: number | string = 1, branchId?: number): Observable<DiningTableData[]> {
    let url = `${this.apiUrl}/${restaurantId}/tables`;
    if (branchId) {
      url += `?branchId=${branchId}`;
    }

    return this.http.get<{ success: boolean; data: DiningTableData[] }>(url).pipe(
      map(res => res.data || []),
      tap(tables => {
        this.tablesList.set(tables);
        this.calculateLocalStats(tables);
      })
    );
  }

  fetchStats(restaurantId: number | string = 1): Observable<TableStatsData> {
    return this.http.get<{ success: boolean; data: TableStatsData }>(`${this.apiUrl}/${restaurantId}/tables/stats`).pipe(
      map(res => res.data),
      tap(stats => {
        if (stats) {
          this.tableStats.set(stats);
        }
      })
    );
  }

  getTableDetails(restaurantId: number | string, tableId: number | string): Observable<DiningTableData> {
    return this.http.get<{ success: boolean; data: DiningTableData }>(`${this.apiUrl}/${restaurantId}/tables/${tableId}`).pipe(
      map(res => res.data)
    );
  }

  createTable(restaurantId: number | string, payload: CreateTablePayload): Observable<DiningTableData> {
    return this.http.post<{ success: boolean; data: DiningTableData }>(`${this.apiUrl}/${restaurantId}/tables`, payload).pipe(
      map(res => res.data),
      tap(newTable => {
        this.tablesList.update(list => [...list, newTable]);
        this.calculateLocalStats(this.tablesList());
      })
    );
  }

  updateTable(restaurantId: number | string, tableId: number | string, payload: Partial<CreateTablePayload>): Observable<DiningTableData> {
    return this.http.put<{ success: boolean; data: DiningTableData }>(`${this.apiUrl}/${restaurantId}/tables/${tableId}`, payload).pipe(
      map(res => res.data),
      tap(updated => {
        this.tablesList.update(list => list.map(t => t.id === updated.id ? updated : t));
        this.calculateLocalStats(this.tablesList());
      })
    );
  }

  deleteTable(restaurantId: number | string, tableId: number | string): Observable<void> {
    return this.http.delete<{ success: boolean }>(`${this.apiUrl}/${restaurantId}/tables/${tableId}`).pipe(
      map(() => void 0),
      tap(() => {
        this.tablesList.update(list => list.filter(t => String(t.id) !== String(tableId)));
        this.calculateLocalStats(this.tablesList());
      })
    );
  }

  updateStatus(restaurantId: number | string, tableId: number | string, status: TableStatus): Observable<DiningTableData> {
    return this.http.patch<{ success: boolean; data: DiningTableData }>(`${this.apiUrl}/${restaurantId}/tables/${tableId}/status`, { status }).pipe(
      map(res => res.data),
      tap(updated => {
        this.tablesList.update(list => list.map(t => t.id === updated.id ? updated : t));
        this.calculateLocalStats(this.tablesList());
      })
    );
  }

  reserveTable(restaurantId: number | string, tableId: number | string, payload: ReserveTablePayload): Observable<DiningTableData> {
    return this.http.post<{ success: boolean; data: DiningTableData }>(`${this.apiUrl}/${restaurantId}/tables/${tableId}/reserve`, payload).pipe(
      map(res => res.data),
      tap(reserved => {
        this.tablesList.update(list => list.map(t => t.id === reserved.id ? reserved : t));
        this.calculateLocalStats(this.tablesList());
      })
    );
  }

  closeTable(restaurantId: number | string, tableId: number | string): Observable<DiningTableData> {
    return this.http.post<{ success: boolean; data: DiningTableData }>(`${this.apiUrl}/${restaurantId}/tables/${tableId}/close`, {}).pipe(
      map(res => res.data),
      tap(closed => {
        this.tablesList.update(list => list.map(t => t.id === closed.id ? closed : t));
        this.calculateLocalStats(this.tablesList());
      })
    );
  }

  restoreTable(restaurantId: number | string, tableId: number | string): Observable<DiningTableData> {
    return this.http.post<{ success: boolean; data: DiningTableData }>(`${this.apiUrl}/${restaurantId}/tables/${tableId}/restore`, {}).pipe(
      map(res => res.data),
      tap(restored => {
        this.tablesList.update(list => [...list.filter(t => t.id !== restored.id), restored].sort((a, b) => a.tableNumber.localeCompare(b.tableNumber)));
        this.calculateLocalStats(this.tablesList());
      })
    );
  }

  private calculateLocalStats(tables: DiningTableData[]) {
    const total = tables.length;
    const available = tables.filter(t => t.status === 'AVAILABLE').length;
    const occupied = tables.filter(t => t.status === 'OCCUPIED').length;
    const reserved = tables.filter(t => t.status === 'RESERVED').length;
    const cleaning = tables.filter(t => t.status === 'CLEANING').length;

    this.tableStats.set({
      totalTables: total,
      availableTables: available,
      occupiedTables: occupied,
      reservedTables: reserved,
      cleaningTables: cleaning
    });
  }
}
