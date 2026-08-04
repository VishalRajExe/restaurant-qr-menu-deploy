import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, map } from 'rxjs';
import { environment } from '../environments/environment';
import { ApiResponse } from '../models/api-response.model';

export interface MediaAsset {
  id: string;
  url: string;
  thumbnailUrl?: string;
  fileName?: string;
  fileSize?: number;
  mimeType?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UploadService {
  private http = inject(HttpClient);

  /**
   * Upload image to Cloudinary via backend.
   * Backend endpoint: POST /media/restaurants/{restaurantId}/upload
   * Returns the Cloudinary secure_url via MediaAsset.url
   */
  uploadImage(file: File, restaurantId: string | number = 1): Observable<string> {
    const numericId = typeof restaurantId === 'string'
      ? (parseInt(restaurantId.replace('r', ''), 10) || 1)
      : restaurantId;

    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', 'menu-items');

    return this.http.post<ApiResponse<MediaAsset>>(
      `${environment.apiUrl}/media/restaurants/${numericId}/upload`,
      formData
    ).pipe(
      map((res: ApiResponse<MediaAsset>) => {
        if (res && res.success && res.data && res.data.url) {
          return res.data.url;
        }
        throw new Error('Upload failed: No URL in response');
      }),
      catchError((err: { message: string }) => {
        console.warn('Cloudinary upload failed, using local base64 preview:', err.message);
        // Fallback: convert to base64 DataURL for local preview
        return new Observable<string>(subscriber => {
          const reader = new FileReader();
          reader.onload = () => {
            subscriber.next(reader.result as string);
            subscriber.complete();
          };
          reader.onerror = () => subscriber.error(err);
          reader.readAsDataURL(file);
        });
      })
    );
  }

  /**
   * Delete a media asset by ID.
   * Backend endpoint: DELETE /media/restaurants/{restaurantId}/assets/{assetId}
   */
  deleteAsset(restaurantId: string | number, assetId: string | number): Observable<boolean> {
    const rId = typeof restaurantId === 'string' ? (parseInt(restaurantId, 10) || 1) : restaurantId;
    const aId = typeof assetId === 'string' ? parseInt(assetId, 10) : assetId;

    return this.http.delete<ApiResponse<void>>(`${environment.apiUrl}/media/restaurants/${rId}/assets/${aId}`).pipe(
      map(() => true),
      catchError(() => of(true))
    );
  }

  /**
   * Get CDN optimized URL for an asset.
   * Backend endpoint: GET /media/restaurants/{restaurantId}/assets/{assetId}/cdn-url
   */
  getCdnUrl(restaurantId: string | number, assetId: string | number, width = 500, height = 500): Observable<string> {
    const rId = typeof restaurantId === 'string' ? (parseInt(restaurantId, 10) || 1) : restaurantId;
    const aId = typeof assetId === 'string' ? parseInt(assetId, 10) : assetId;

    return this.http.get<ApiResponse<string>>(
      `${environment.apiUrl}/media/restaurants/${rId}/assets/${aId}/cdn-url?width=${width}&height=${height}&cropMode=fill`
    ).pipe(
      map((res: ApiResponse<string>) => res?.data || ''),
      catchError(() => of(''))
    );
  }
}
