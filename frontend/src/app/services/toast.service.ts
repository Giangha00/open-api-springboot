import { Injectable, signal } from '@angular/core';
import { ApiProblem } from '../models/api-problem.model';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title: string;
  message: string;
  code?: string;
  errors?: Array<{ field: string; message: string }>;
  timestamp: Date;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private readonly toastsSignal = signal<ToastMessage[]>([]);
  readonly toasts = this.toastsSignal.asReadonly();

  success(title: string, message: string = ''): void {
    this.addToast('success', title, message);
  }

  info(title: string, message: string = ''): void {
    this.addToast('info', title, message);
  }

  warning(title: string, message: string = ''): void {
    this.addToast('warning', title, message);
  }

  error(title: string, errOrMessage: any): void {
    let message = '';
    let code: string | undefined;
    let errors: Array<{ field: string; message: string }> | undefined;

    if (typeof errOrMessage === 'string') {
      message = errOrMessage;
    } else if (errOrMessage?.error) {
      const prob: ApiProblem = errOrMessage.error;
      message = prob.detail || prob.title || errOrMessage.message || 'Lỗi không xác định';
      code = prob.code;
      errors = prob.errors;
    } else if (errOrMessage?.message) {
      message = errOrMessage.message;
    } else {
      message = 'Có lỗi xảy ra trong quá trình xử lý.';
    }

    this.addToast('error', title, message, code, errors);
  }

  remove(id: string): void {
    this.toastsSignal.update((list) => list.filter((t) => t.id !== id));
  }

  private addToast(
    type: 'success' | 'error' | 'info' | 'warning',
    title: string,
    message: string,
    code?: string,
    errors?: Array<{ field: string; message: string }>
  ): void {
    const id = Math.random().toString(36).substring(2, 9);
    const newToast: ToastMessage = {
      id,
      type,
      title,
      message,
      code,
      errors,
      timestamp: new Date()
    };

    this.toastsSignal.update((list) => [...list, newToast]);

    // Auto dismiss after 6 seconds
    setTimeout(() => {
      this.remove(id);
    }, 6000);
  }
}
