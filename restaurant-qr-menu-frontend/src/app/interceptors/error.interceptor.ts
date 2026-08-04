import { HttpInterceptorFn, HttpErrorResponse, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { catchError, from, switchMap, throwError } from 'rxjs';

let isRefreshing = false;

const API_BASE = 'http://localhost:8080/api/v1';

export const errorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // ── 401 Unauthorized — try token refresh before giving up ───────────────
      if (error.status === 401
        && !req.url.includes('/auth/refresh')
        && !req.url.includes('/auth/login')) {

        const refreshToken = localStorage.getItem('jwt_refresh_token');

        if (refreshToken && !isRefreshing) {
          isRefreshing = true;

          // Use native fetch to avoid circular HttpClient interceptor loops
          const refreshPromise = fetch(`${API_BASE}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
          }).then((r: Response) => r.json() as Promise<Record<string, unknown>>);

          return from(refreshPromise).pipe(
            switchMap((res: Record<string, unknown>) => {
              isRefreshing = false;
              const data = (res['data'] as Record<string, unknown> | undefined) ?? {};
              const newToken = (data['accessToken'] ?? res['accessToken']) as string | undefined;

              if (newToken) {
                localStorage.setItem('jwt_access_token', newToken);
                const newRefresh = (data['refreshToken'] ?? res['refreshToken']) as string | undefined;
                if (newRefresh) {
                  localStorage.setItem('jwt_refresh_token', newRefresh);
                }
                const retried = req.clone({
                  setHeaders: { Authorization: `Bearer ${newToken}` }
                });
                return next(retried);
              }
              clearSessionAndRedirect();
              return throwError(() => new Error('Session expired. Please log in again.'));
            }),
            catchError(() => {
              isRefreshing = false;
              clearSessionAndRedirect();
              return throwError(() => new Error('Session expired. Please log in again.'));
            })
          );
        }

        clearSessionAndRedirect();
        return throwError(() => new Error('Authentication required. Please log in.'));
      }

      // ── Build meaningful error messages ─────────────────────────────────────
      let errorMessage = 'An unexpected error occurred.';
      const errBody = error.error as Record<string, unknown> | null;

      if (error.error instanceof ErrorEvent) {
        errorMessage = `Network Error: ${error.error.message}`;
      } else if (errBody && typeof errBody === 'object') {
        if (errBody['message']) {
          errorMessage = String(errBody['message']);
        } else if (errBody['errors']) {
          errorMessage = typeof errBody['errors'] === 'string'
            ? errBody['errors']
            : JSON.stringify(errBody['errors']);
        }
      } else {
        switch (error.status) {
          case 403: errorMessage = 'Access denied: You do not have permission.'; break;
          case 404: errorMessage = 'Requested resource was not found.'; break;
          case 409: errorMessage = 'Conflict: Entry with these details already exists.'; break;
          case 422: errorMessage = 'Validation failed. Please check your inputs.'; break;
          case 500: errorMessage = 'Server error. Please try again later.'; break;
          case 503: errorMessage = 'Service unavailable. Please try again later.'; break;
        }
      }

      if (error.status !== 401) {
        console.warn(`[HTTP ${error.status}]`, errorMessage, req.url);
      }

      return throwError(() => new Error(errorMessage));
    })
  );
};

function clearSessionAndRedirect(): void {
  localStorage.removeItem('jwt_access_token');
  localStorage.removeItem('jwt_refresh_token');
  localStorage.removeItem('user_session');
  sessionStorage.clear();
  if (typeof window !== 'undefined' && !window.location.pathname.includes('/login')) {
    window.location.href = '/login';
  }
}
