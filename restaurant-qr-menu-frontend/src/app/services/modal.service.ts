import { Injectable, signal } from '@angular/core';

export interface ModalConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  type?: 'danger' | 'warning' | 'info';
  onConfirm: () => void;
  onCancel?: () => void;
}

@Injectable({
  providedIn: 'root'
})
export class ModalService {
  activeModal = signal<ModalConfig | null>(null);

  confirm(config: ModalConfig) {
    this.activeModal.set(config);
  }

  close() {
    const current = this.activeModal();
    if (current && current.onCancel) {
      current.onCancel();
    }
    this.activeModal.set(null);
  }

  proceed() {
    const current = this.activeModal();
    if (current) {
      current.onConfirm();
    }
    this.activeModal.set(null);
  }
}
