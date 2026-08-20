import { Injectable, inject, signal } from '@angular/core';
import { Observable, isObservable } from 'rxjs';
import { ToastService } from './toast.service';

export interface UndoAction {
  id: string;
  message: string;
  subMessage?: string;
  durationMs: number;
  remainingSeconds: number;
  progress: number;
  onUndo: () => Observable<any> | Promise<any> | void;
  entityType?: string;
  createdAt: number;
}

@Injectable({
  providedIn: 'root'
})
export class UndoService {
  private toastService = inject(ToastService);

  activeUndo = signal<UndoAction | null>(null);
  isExecutingUndo = signal<boolean>(false);

  private timerInterval: any = null;

  constructor() {
    // Listen for Ctrl+Z or Cmd+Z globally
    if (typeof window !== 'undefined') {
      window.addEventListener('keydown', (e: KeyboardEvent) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'z' && !e.shiftKey) {
          const current = this.activeUndo();
          if (current && !this.isExecutingUndo()) {
            e.preventDefault();
            this.executeUndo();
          }
        }
      });
    }
  }

  showUndo(message: string, onUndo: () => Observable<any> | Promise<any> | void, durationSeconds: number = 7, subMessage?: string) {
    this.clearTimer();

    const durationMs = durationSeconds * 1000;
    const action: UndoAction = {
      id: 'undo_' + Date.now(),
      message,
      subMessage,
      durationMs,
      remainingSeconds: durationSeconds,
      progress: 100,
      onUndo,
      createdAt: Date.now()
    };

    this.activeUndo.set(action);

    const startTime = Date.now();
    const intervalMs = 100;

    this.timerInterval = setInterval(() => {
      const elapsed = Date.now() - startTime;
      const remainingMs = Math.max(0, durationMs - elapsed);
      const remainingSec = Math.ceil(remainingMs / 1000);
      const progress = (remainingMs / durationMs) * 100;

      if (remainingMs <= 0) {
        this.dismiss();
      } else {
        this.activeUndo.update(curr => {
          if (!curr) return null;
          return { ...curr, remainingSeconds: remainingSec, progress };
        });
      }
    }, intervalMs);
  }

  executeUndo() {
    const current = this.activeUndo();
    if (!current || this.isExecutingUndo()) return;

    this.isExecutingUndo.set(true);
    this.clearTimer();

    try {
      const result = current.onUndo();

      if (isObservable(result)) {
        result.subscribe({
          next: () => {
            this.finishUndoSuccess(current.message);
          },
          error: (err) => {
            this.finishUndoError(err);
          }
        });
      } else if (result instanceof Promise) {
        result
          .then(() => this.finishUndoSuccess(current.message))
          .catch((err) => this.finishUndoError(err));
      } else {
        this.finishUndoSuccess(current.message);
      }
    } catch (err: any) {
      this.finishUndoError(err);
    }
  }

  dismiss() {
    this.clearTimer();
    this.activeUndo.set(null);
    this.isExecutingUndo.set(false);
  }

  private finishUndoSuccess(originalMsg: string) {
    this.isExecutingUndo.set(false);
    this.activeUndo.set(null);
    this.toastService.success('Action Reverted', 'Previous state successfully restored.');
  }

  private finishUndoError(err: any) {
    this.isExecutingUndo.set(false);
    this.activeUndo.set(null);
    const msg = err?.error?.message || err?.message || 'Could not reverse action.';
    this.toastService.error('Undo Failed', msg);
  }

  private clearTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }
}
