import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container" *ngIf="toastService.toasts().length > 0">
      <div
        *ngFor="let toast of toastService.toasts()"
        class="toast-card"
        [ngClass]="'toast-' + toast.type"
      >
        <div class="toast-indicator"></div>
        <div class="toast-body">
          <div class="toast-title-row">
            <span class="toast-title">{{ toast.title }}</span>
            <span *ngIf="toast.code" class="toast-code">{{ toast.code }}</span>
          </div>
          <p class="toast-message">{{ toast.message }}</p>

          <ul *ngIf="toast.errors && toast.errors.length > 0" class="toast-errors">
            <li *ngFor="let err of toast.errors">
              <strong>{{ err.field }}:</strong> {{ err.message }}
            </li>
          </ul>
        </div>
        <button class="toast-close" (click)="toastService.remove(toast.id)">&times;</button>
      </div>
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 1.5rem;
      right: 1.5rem;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      max-width: 420px;
      width: calc(100% - 3rem);
      pointer-events: none;
    }

    .toast-card {
      pointer-events: auto;
      display: flex;
      align-items: flex-start;
      background: #131c2e;
      border: 1px solid rgba(255, 255, 255, 0.12);
      border-radius: 12px;
      padding: 0.85rem 1rem;
      box-shadow: 0 12px 30px rgba(0, 0, 0, 0.5);
      animation: slideIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      position: relative;
      overflow: hidden;

      &.toast-success {
        border-color: rgba(16, 185, 129, 0.3);
        .toast-indicator { background: #10b981; }
      }
      &.toast-error {
        border-color: rgba(239, 68, 68, 0.35);
        .toast-indicator { background: #ef4444; }
      }
      &.toast-warning {
        border-color: rgba(245, 158, 11, 0.3);
        .toast-indicator { background: #f59e0b; }
      }
      &.toast-info {
        border-color: rgba(99, 102, 241, 0.3);
        .toast-indicator { background: #6366f1; }
      }
    }

    .toast-indicator {
      position: absolute;
      top: 0;
      left: 0;
      bottom: 0;
      width: 4px;
    }

    .toast-body {
      flex: 1;
      padding-left: 0.5rem;
    }

    .toast-title-row {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.25rem;
    }

    .toast-title {
      font-weight: 700;
      font-size: 0.9rem;
      color: #f8fafc;
    }

    .toast-code {
      font-family: var(--font-mono);
      font-size: 0.7rem;
      padding: 0.15rem 0.4rem;
      background: rgba(239, 68, 68, 0.2);
      color: #fca5a5;
      border-radius: 4px;
    }

    .toast-message {
      font-size: 0.825rem;
      color: #94a3b8;
      line-height: 1.4;
    }

    .toast-errors {
      margin-top: 0.4rem;
      padding-left: 1rem;
      font-size: 0.775rem;
      color: #fda4af;
      li { margin-bottom: 0.2rem; }
    }

    .toast-close {
      background: transparent;
      border: none;
      color: #64748b;
      font-size: 1.25rem;
      cursor: pointer;
      line-height: 1;
      padding-left: 0.5rem;
      &:hover { color: #cbd5e1; }
    }

    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }
  `]
})
export class ToastComponent {
  readonly toastService = inject(ToastService);
}
