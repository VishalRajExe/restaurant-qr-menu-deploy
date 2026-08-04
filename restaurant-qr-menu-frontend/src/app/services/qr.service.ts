import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map, switchMap } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { BranchService, BranchData } from './branch.service';

export interface QrCodeData {
  id: string;
  tableNumber: string;
  label?: string;
  qrToken?: string;
  /**
   * qrImageUrl — Cloudinary-hosted actual scannable QR code image.
   * Generated server-side using ZXing and uploaded to Cloudinary.
   */
  qrCodeUrl?: string;
  targetUrl?: string;
  scansCount?: number;
  status?: 'ACTIVE' | 'INACTIVE';
}

@Injectable({
  providedIn: 'root'
})
export class QrService {
  private http          = inject(HttpClient);
  private branchService = inject(BranchService);

  qrCodesList = signal<QrCodeData[]>([]);

  /**
   * Build the frontend guest menu URL for a given table number.
   * Format: http://localhost:4200/menu/{token}?table={tableNum}
   * This is also what the QR code encodes (via backend's qr.base-url config).
   */
  private buildMenuUrl(token: string, tableNumber: string): string {
    const tableNum = tableNumber.replace(/^Table\s*/i, '').trim();
    return `${environment.frontendUrl || 'http://localhost:4200'}/menu/${token}?table=${encodeURIComponent(tableNum)}`;
  }

  /**
   * Build a fallback QR image URL using qrserver.com (used if Cloudinary URL is absent).
   */
  private buildFallbackQrImageUrl(menuUrl: string): string {
    return `https://api.qrserver.com/v1/create-qr-code/?size=300x300&ecc=H&data=${encodeURIComponent(menuUrl)}`;
  }

  private mapQrCode(q: any): QrCodeData {
    const tableNum = q.tableNumber || q.label || `Table ${q.id}`;
    const token    = q.token || q.qrCodeToken || q.qrToken || `token_${q.id}`;
    const menuUrl  = this.buildMenuUrl(token, tableNum);
    const qrImageUrl = q.qrImageUrl || this.buildFallbackQrImageUrl(menuUrl);

    return {
      id:          String(q.id),
      tableNumber: tableNum,
      label:       q.label || tableNum,
      qrToken:     token,
      targetUrl:   menuUrl,
      qrCodeUrl:   qrImageUrl,
      scansCount:  q.scanCount || q.scansCount || 0,
      status:      q.status || 'ACTIVE'
    };
  }

  fetchQrCodes(restaurantId: string): Observable<QrCodeData[]> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    return this.http.get<ApiResponse<any[]>>(`${environment.apiUrl}/restaurants/${numericId}/qr-codes`).pipe(
      map((res: ApiResponse<any[]>) => {
        if (res && res.success && Array.isArray(res.data)) {
          const mapped = res.data
            .filter((q: any) => !q.isDeleted)
            .map((q: any) => this.mapQrCode(q));
          this.qrCodesList.set(mapped);
          return mapped;
        }
        return this.qrCodesList();
      }),
      catchError((err: { message: string }) => {
        console.warn('Fetch QR codes failed:', err.message);
        return of(this.qrCodesList());
      })
    );
  }

  /**
   * Generate a QR Code.
   * - First fetches branches to get a real branchId
   * - Backend generates the QR image with ZXing and uploads to Cloudinary
   * - Returns the Cloudinary-hosted qrImageUrl
   */
  generateQrCode(restaurantId: string, tableNumber: string, label?: string): Observable<QrCodeData> {
    const numericId = parseInt(restaurantId.replace('r', ''), 10) || 1;

    // Ensure branches are loaded to get a real branchId
    return this.branchService.fetchBranches(restaurantId).pipe(
      switchMap((branches: BranchData[]) => {
        const branchId = branches.length > 0
          ? parseInt(branches[0].id, 10)
          : 1; // fallback to 1 if no branches yet

        const body = {
          branchId,
          tableNumber,
          label: label || tableNumber
        };

        return this.http.post<ApiResponse<any>>(`${environment.apiUrl}/restaurants/${numericId}/qr-codes`, body).pipe(
          map((res: ApiResponse<any>) => {
            const q = res?.data || {};
            const mapped = this.mapQrCode({
              ...q,
              tableNumber: q.tableNumber || tableNumber,
              label: q.label || label || tableNumber
            });
            this.qrCodesList.update((list: QrCodeData[]) => [mapped, ...list]);
            return mapped;
          }),
          catchError((err: { message: string }) => {
            console.warn('QR generate API failed, creating local entry:', err.message);
            // Local fallback entry with qrserver.com QR image
            const localToken = 'local_' + Date.now();
            const menuUrl = this.buildMenuUrl(localToken, tableNumber);
            const item: QrCodeData = {
              id: 'qr_' + Date.now(),
              tableNumber,
              label: label || tableNumber,
              qrToken: localToken,
              targetUrl: menuUrl,
              qrCodeUrl: this.buildFallbackQrImageUrl(menuUrl),
              scansCount: 0,
              status: 'ACTIVE'
            };
            this.qrCodesList.update((list: QrCodeData[]) => [item, ...list]);
            return of(item);
          })
        );
      }),
      catchError((err: { message: string }) => {
        console.warn('Branch fetch failed before QR generation:', err.message);
        const localToken = 'local_' + Date.now();
        const menuUrl = this.buildMenuUrl(localToken, tableNumber);
        const item: QrCodeData = {
          id: 'qr_' + Date.now(),
          tableNumber,
          label: label || tableNumber,
          qrToken: localToken,
          targetUrl: menuUrl,
          qrCodeUrl: this.buildFallbackQrImageUrl(menuUrl),
          scansCount: 0,
          status: 'ACTIVE'
        };
        this.qrCodesList.update((list: QrCodeData[]) => [item, ...list]);
        return of(item);
      })
    );
  }

  deactivateQrCode(restaurantId: string, qrId: string): Observable<boolean> {
    const numericRestId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    const numericQrId   = parseInt(qrId, 10);

    this.qrCodesList.update((list: QrCodeData[]) =>
      list.map((q: QrCodeData) => q.id === qrId ? { ...q, status: 'INACTIVE' as const } : q)
    );

    if (!isNaN(numericQrId)) {
      return this.http.patch<ApiResponse<any>>(
        `${environment.apiUrl}/restaurants/${numericRestId}/qr-codes/${numericQrId}/deactivate`, {}
      ).pipe(
        map(() => true),
        catchError(() => of(true))
      );
    }
    return of(true);
  }

  deleteQrCode(restaurantId: string, qrId: string): Observable<boolean> {
    this.qrCodesList.update((list: QrCodeData[]) => list.filter((q: QrCodeData) => q.id !== qrId));

    const numericRestId = parseInt(restaurantId.replace('r', ''), 10) || 1;
    const numericQrId   = parseInt(qrId, 10);

    if (!isNaN(numericQrId)) {
      return this.http.delete<ApiResponse<any>>(
        `${environment.apiUrl}/restaurants/${numericRestId}/qr-codes/${numericQrId}`
      ).pipe(
        map(() => true),
        catchError(() => of(true))
      );
    }
    return of(true);
  }
}
