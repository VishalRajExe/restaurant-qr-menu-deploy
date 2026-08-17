import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  toasts = signal<ToastMessage[]>([]);

  show(arg1: string, arg2?: string, arg3?: string, duration: number = 4000) {
    let type: ToastMessage['type'] = 'info';
    let title = '';
    let message = '';

    if (arg1 === 'success' || arg1 === 'error' || arg1 === 'warning' || arg1 === 'info') {
      type = arg1;
      title = arg2 || '';
      message = arg3 || '';
    } else {
      title = 'Notice';
      message = arg1;
      if (arg2 === 'success' || arg2 === 'error' || arg2 === 'warning' || arg2 === 'info') {
        type = arg2;
      }
    }

    const id = 'toast_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4);
    const newToast: ToastMessage = { id, type, title, message, duration };

    this.toasts.update(list => [...list, newToast]);

    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }
  }

  success(title: string, message: string = '') {
    this.show('success', title, message);
  }

  error(title: string, message: string = '') {
    this.show('error', title, message);
  }

  warning(title: string, message: string = '') {
    this.show('warning', title, message);
  }

  info(title: string, message: string = '') {
    this.show('info', title, message);
  }

  remove(id: string) {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }
}
