import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { OrderAction, OrderResponseV2 } from '../../models/order.model';

@Component({
  selector: 'app-shipping-simulator',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>Mô Phỏng Webhook Đối Tác Vận Chuyển</h1>
          <p>
            Mô phỏng đơn vị giao vận (GHN / GHTK / ViettelPost) gọi Webhook cập nhật đơn hàng
            (REST API: POST /api/webhooks/shipping &bull; Header X-Partner-Key)
          </p>
        </div>
      </div>

      <div class="grid-layout">
        <!-- Webhook Trigger Form -->
        <div class="col-main">
          <div class="glass-card mb-4">
            <h3 class="card-title">Cấu Hình Gửi Webhook Giao Vận</h3>
            <p class="card-subtitle">
              Hệ thống sẽ xác thực API Key qua header <code>X-Partner-Key</code> và tự động xử lý chuyển trạng thái đơn.
            </p>

            <form (ngSubmit)="sendWebhook()" class="mt-4">
              <div class="form-group">
                <label>X-Partner-Key (API Key Header) *</label>
                <input
                  type="text"
                  class="form-control font-mono"
                  [(ngModel)]="partnerKey"
                  name="partnerKey"
                  required
                />
                <span class="field-hint">Mặc định: partner-secret-key-2026</span>
              </div>

              <div class="form-group">
                <label>Mã Đơn Hàng *</label>
                <div class="code-selector-row">
                  <input
                    type="text"
                    class="form-control font-mono"
                    [(ngModel)]="orderCode"
                    name="orderCode"
                    placeholder="VD: ORD-2026-000123"
                    required
                  />
                  <select class="form-control" (change)="onSelectOrder($event)">
                    <option value="">-- Chọn nhanh từ đơn hiện có --</option>
                    <option *ngFor="let o of shippingCandidateOrders()" [value]="o.code">
                      {{ o.code }} ({{ o.status }})
                    </option>
                  </select>
                </div>
              </div>

              <div class="form-group">
                <label>Hành Động Vận Chuyển (Action) *</label>
                <div class="action-picker">
                  <label class="action-option" [class.selected]="selectedAction === 'SHIP'">
                    <input type="radio" [(ngModel)]="selectedAction" name="action" value="SHIP" />
                    <div class="action-details">
                      <strong>SHIP (Bắt đầu giao)</strong>
                      <span>Chuyển đơn từ CONFIRMED &rarr; SHIPPING</span>
                    </div>
                  </label>

                  <label class="action-option" [class.selected]="selectedAction === 'DELIVER'">
                    <input type="radio" [(ngModel)]="selectedAction" name="action" value="DELIVER" />
                    <div class="action-details">
                      <strong>DELIVER (Giao thành công)</strong>
                      <span>Chuyển đơn từ SHIPPING &rarr; DELIVERED (Bắt đầu 7 ngày đổi trả)</span>
                    </div>
                  </label>

                  <label class="action-option" [class.selected]="selectedAction === 'FAIL_DELIVERY'">
                    <input type="radio" [(ngModel)]="selectedAction" name="action" value="FAIL_DELIVERY" />
                    <div class="action-details">
                      <strong>FAIL_DELIVERY (Giao thất bại)</strong>
                      <span>Chuyển lại về CONFIRMED để chờ giao lại đợt 2</span>
                    </div>
                  </label>
                </div>
              </div>

              <div class="form-group">
                <label>Ghi chú / Lý do giao hàng (Notes)</label>
                <textarea
                  class="form-control"
                  rows="2"
                  [(ngModel)]="notes"
                  name="notes"
                  placeholder="VD: Bưu tá Nguyễn Văn A đã giao hàng tận tay khách"
                ></textarea>
              </div>

              <button
                type="submit"
                class="btn btn-primary btn-block"
                [disabled]="!orderCode || isSubmitting()"
              >
                {{ isSubmitting() ? 'Đang gửi Webhook...' : '🚀 Gửi Webhook Giao Vận' }}
              </button>
            </form>
          </div>
        </div>

        <!-- Right Column: Response Inspector -->
        <div class="col-sidebar">
          <div class="glass-card mb-4 inspector-card">
            <h3 class="sidebar-title">🔍 Kết Quả Phản Hồi Webhook</h3>
            <p class="sidebar-desc">Kiểm tra kết quả phản hồi từ backend sau khi đối tác bắn Webhook.</p>

            <div *ngIf="!lastResponse() && !lastError()" class="empty-inspector">
              <span>Chưa có dữ liệu phản hồi. Nhấn "Gửi Webhook Giao Vận" để xem phản hồi.</span>
            </div>

            <div *ngIf="lastResponse()" class="response-box success">
              <div class="res-status-line">
                <span class="badge badge-DELIVERED">HTTP 200 OK</span>
                <span class="res-code">{{ lastResponse()?.code }}</span>
              </div>
              <p class="res-msg">{{ lastResponse()?.message }}</p>
              <pre class="json-preview">{{ lastResponse() | json }}</pre>
            </div>

            <div *ngIf="lastError()" class="response-box error">
              <div class="res-status-line">
                <span class="badge badge-CANCELLED">HTTP {{ lastError()?.status || 'LỖI' }}</span>
                <span class="res-code">{{ lastError()?.code || 'ERROR' }}</span>
              </div>
              <p class="res-msg">{{ lastError()?.detail || lastError()?.message }}</p>
              <pre class="json-preview">{{ lastError() | json }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mb-4 { margin-bottom: 1.5rem; }
    .mt-4 { margin-top: 1.5rem; }
    .font-mono { font-family: var(--font-mono); }

    .grid-layout {
      display: grid;
      grid-template-columns: 1fr 420px;
      gap: 1.5rem;
      align-items: flex-start;

      @media (max-width: 960px) {
        grid-template-columns: 1fr;
      }
    }

    .card-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: #ffffff;
      margin-bottom: 0.35rem;
    }
    .card-subtitle {
      font-size: 0.85rem;
      color: var(--text-muted);
    }

    .field-hint {
      display: block;
      font-size: 0.75rem;
      color: var(--text-dim);
      margin-top: 0.25rem;
    }

    .code-selector-row {
      display: flex;
      gap: 0.75rem;
    }

    .action-picker {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;

      .action-option {
        display: flex;
        align-items: flex-start;
        gap: 0.75rem;
        padding: 0.75rem 1rem;
        background: var(--bg-input);
        border: 1px solid var(--border-color);
        border-radius: var(--radius-sm);
        cursor: pointer;
        transition: all 0.2s ease;

        input[type="radio"] {
          margin-top: 0.25rem;
        }

        .action-details {
          display: flex;
          flex-direction: column;
          strong { font-size: 0.9rem; color: #ffffff; }
          span { font-size: 0.775rem; color: var(--text-muted); }
        }

        &.selected {
          border-color: var(--primary);
          background: rgba(99, 102, 241, 0.12);
        }
      }
    }

    .btn-block {
      width: 100%;
      padding: 0.85rem;
      font-size: 1rem;
    }

    .sidebar-title {
      font-size: 1.05rem;
      font-weight: 700;
      color: #ffffff;
      margin-bottom: 0.35rem;
    }
    .sidebar-desc {
      font-size: 0.8rem;
      color: var(--text-muted);
      margin-bottom: 1rem;
    }

    .empty-inspector {
      padding: 2.5rem 1rem;
      border: 1px dashed var(--border-color);
      border-radius: var(--radius-sm);
      text-align: center;
      font-size: 0.85rem;
      color: var(--text-dim);
    }

    .response-box {
      border-radius: var(--radius-sm);
      padding: 1rem;
      border: 1px solid var(--border-color);

      &.success {
        background: rgba(16, 185, 129, 0.08);
        border-color: rgba(16, 185, 129, 0.3);
      }
      &.error {
        background: rgba(239, 68, 68, 0.08);
        border-color: rgba(239, 68, 68, 0.3);
      }

      .res-status-line {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.5rem;
      }
      .res-code {
        font-family: var(--font-mono);
        font-weight: 700;
        font-size: 0.85rem;
        color: #ffffff;
      }
      .res-msg {
        font-size: 0.85rem;
        margin-bottom: 0.75rem;
        color: var(--text-main);
      }
      .json-preview {
        background: #0a0e17;
        border-radius: 6px;
        padding: 0.75rem;
        font-size: 0.75rem;
        color: #94a3b8;
        overflow-x: auto;
        max-height: 250px;
      }
    }
  `]
})
export class ShippingSimulatorComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  readonly authService = inject(AuthService);

  readonly shippingCandidateOrders = signal<OrderResponseV2[]>([]);
  readonly isSubmitting = signal<boolean>(false);
  readonly lastResponse = signal<any>(null);
  readonly lastError = signal<any>(null);

  partnerKey = 'partner-secret-key-2026';
  orderCode = '';
  selectedAction: OrderAction = 'SHIP';
  notes = 'Bưu tá xác nhận qua ứng dụng giao vận';

  ngOnInit(): void {
    this.authService.selectProfile('partner');
    this.loadOrders();
  }

  loadOrders(): void {
    // Load orders that can transition
    this.orderService.getOrders(undefined, undefined, undefined, undefined, 0, 50, 'createdAt,desc').subscribe({
      next: (res) => {
        const candidates = res.content.filter(
          (o) => o.status === 'CONFIRMED' || o.status === 'SHIPPING'
        );
        this.shippingCandidateOrders.set(candidates);
        if (candidates.length > 0 && !this.orderCode) {
          this.orderCode = candidates[0].code;
        }
      },
      error: () => {}
    });
  }

  onSelectOrder(event: Event): void {
    const target = event.target as HTMLSelectElement;
    if (target.value) {
      this.orderCode = target.value;
      const found = this.shippingCandidateOrders().find((o) => o.code === target.value);
      if (found) {
        if (found.status === 'CONFIRMED') {
          this.selectedAction = 'SHIP';
        } else if (found.status === 'SHIPPING') {
          this.selectedAction = 'DELIVER';
        }
      }
    }
  }

  sendWebhook(): void {
    if (!this.orderCode) {
      this.toastService.warning('Thiếu mã đơn', 'Vui lòng chọn hoặc nhập mã đơn hàng.');
      return;
    }

    this.isSubmitting.set(true);
    this.lastResponse.set(null);
    this.lastError.set(null);

    this.orderService
      .sendShippingWebhook({
        code: this.orderCode,
        action: this.selectedAction,
        notes: this.notes
      })
      .subscribe({
        next: (res) => {
          this.isSubmitting.set(false);
          this.lastResponse.set(res);
          this.toastService.success(
            'Webhook thành công',
            `Đơn hàng ${res.code} đã chuyển sang ${res.toStatus}`
          );
          this.loadOrders();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          const errBody = err.error || { message: err.message, status: err.status };
          this.lastError.set(errBody);
          this.toastService.error('Webhook thất bại', err);
        }
      });
  }
}
