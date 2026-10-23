import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import {
  OrderAction,
  OrderEventResponse,
  OrderResponseV1,
  OrderResponseV2,
  OrderStatus
} from '../../models/order.model';

@Component({
  selector: 'app-order-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container" *ngIf="orderV2()">
      <!-- Page Header -->
      <div class="page-header">
        <div class="header-left">
          <div class="order-title-line">
            <h1>Đơn Hàng #{{ orderV2()?.code }}</h1>
            <span class="badge" [ngClass]="'badge-' + orderV2()?.status">
              {{ orderV2()?.status }}
            </span>
            <span class="badge badge-version">ETag / Version: {{ orderV2()?.version }}</span>
          </div>
          <p>
            Tạo ngày: {{ orderV2()?.createdAt | date:'HH:mm dd/MM/yyyy' }} &bull; Khách hàng ID:
            {{ orderV2()?.customerId }}
          </p>
        </div>

        <div class="header-actions">
          <a routerLink="/orders" class="btn btn-secondary">
            &larr; Quay lại danh sách
          </a>
          <button
            *ngIf="orderV2()?.hasInvoice"
            class="btn btn-outline"
            (click)="downloadPdfInvoice()"
          >
            📄 Tải Hóa Đơn PDF
          </button>
        </div>
      </div>

      <!-- Versioning Toggle & Notification Banner -->
      <div class="glass-card mb-4 version-banner">
        <div class="version-toggle">
          <span class="toggle-label">Xem theo chuẩn API:</span>
          <button
            class="btn btn-sm"
            [class.btn-primary]="selectedVersionView === 'v2'"
            [class.btn-secondary]="selectedVersionView !== 'v2'"
            (click)="switchVersionView('v2')"
          >
            v2 (Chuẩn hiện hành)
          </button>
          <button
            class="btn btn-sm"
            [class.btn-warning]="selectedVersionView === 'v1'"
            [class.btn-secondary]="selectedVersionView !== 'v1'"
            (click)="switchVersionView('v1')"
          >
            v1 (Deprecated)
          </button>
        </div>

        <div *ngIf="selectedVersionView === 'v1'" class="v1-notice mt-2">
          <span class="badge badge-deprecated">DEPRECATED</span>
          <span>
            API v1 trả về <code>totalAmount</code> dạng số thuần túy ({{ orderV1()?.totalAmount | number:'1.0-0' }}).
            Sẽ ngừng hỗ trợ từ 2026-12-31 theo RFC spec D8.
          </span>
        </div>
      </div>

      <div class="grid-layout">
        <!-- Main Content -->
        <div class="col-main">
          <!-- Items Table Card -->
          <div class="glass-card mb-4">
            <h3 class="card-title">Sản Phẩm Trong Đơn Hàng</h3>
            <div class="table-responsive mt-3">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Tên sản phẩm</th>
                    <th>Đơn giá snapshot</th>
                    <th>Số lượng</th>
                    <th>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of orderV2()?.items">
                    <td>
                      <strong>{{ item.productName }}</strong>
                      <div class="product-id-sub">Mã SP: #{{ item.productId }}</div>
                    </td>
                    <td class="font-mono">{{ item.unitPrice | number:'1.0-0' }} VND</td>
                    <td class="font-mono">{{ item.quantity }}</td>
                    <td class="font-mono line-total">
                      {{ item.lineTotal | number:'1.0-0' }} VND
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- State Machine Action & Timeline Card -->
          <div class="glass-card mb-4">
            <h3 class="card-title">Nhật Ký Chuyển Trạng Thái (Order Events)</h3>
            <p class="card-subtitle">
              Ghi lại chi tiết mọi hành động chuyển trạng thái theo máy trạng thái 7 trạng thái, 8 hành động.
            </p>

            <div class="timeline">
              <div *ngFor="let ev of events()" class="timeline-item">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                  <div class="timeline-header">
                    <span class="action-tag">{{ ev.action }}</span>
                    <span class="actor-badge">{{ ev.actor }}</span>
                    <span class="timeline-time">{{ ev.at | date:'HH:mm:ss dd/MM/yyyy' }}</span>
                  </div>
                  <div class="status-change">
                    <span *ngIf="ev.fromStatus">{{ ev.fromStatus }} &rarr; </span>
                    <strong>{{ ev.toStatus }}</strong>
                  </div>
                  <p class="timeline-reason" *ngIf="ev.reason">{{ ev.reason }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar Actions & Details -->
        <div class="col-sidebar">
          <!-- Customer Action Card -->
          <div class="glass-card mb-4 action-card">
            <h3 class="sidebar-title">Thao Tác Đơn Hàng</h3>
            <p class="sidebar-desc">Các hành động khả dụng theo vai trò và trạng thái hiện tại.</p>

            <!-- Cancel Action (for PENDING) -->
            <div *ngIf="orderV2()?.status === 'PENDING'" class="action-block">
              <p class="action-info">Đơn hàng đang chờ xác nhận, bạn có thể yêu cầu hủy đơn.</p>
              <button
                class="btn btn-danger btn-block"
                [disabled]="isTransitioning()"
                (click)="performCustomerAction('CANCEL')"
              >
                Hủy Đơn Hàng (CANCEL)
              </button>
            </div>

            <!-- Return Action (for DELIVERED <= 7 days) -->
            <div *ngIf="orderV2()?.status === 'DELIVERED'" class="action-block">
              <p class="action-info">
                Đơn hàng đã giao thành công. Theo chính sách, bạn có 7 ngày để yêu cầu đổi/trả hàng.
              </p>
              <button
                class="btn btn-warning btn-block"
                [disabled]="isTransitioning()"
                (click)="performCustomerAction('REQUEST_RETURN')"
              >
                Yêu Cầu Trả Hàng (REQUEST_RETURN)
              </button>
            </div>

            <div
              *ngIf="orderV2()?.status !== 'PENDING' && orderV2()?.status !== 'DELIVERED'"
              class="no-actions-box"
            >
              <span>Không có thao tác nào khả dụng cho khách hàng ở trạng thái {{ orderV2()?.status }}.</span>
            </div>
          </div>

          <!-- Payment & Delivery Info -->
          <div class="glass-card mb-4">
            <h3 class="sidebar-title">Thông Tin Thanh Toán</h3>
            <div class="info-row">
              <span>Phương thức:</span>
              <strong>{{ orderV2()?.payment?.method }}</strong>
            </div>
            <div class="info-row" *ngIf="orderV2()?.payment?.provider">
              <span>Tổ chức thẻ:</span>
              <strong>{{ orderV2()?.payment?.provider }}</strong>
            </div>
            <div class="info-row" *ngIf="orderV2()?.payment?.last4">
              <span>4 số cuối:</span>
              <strong class="font-mono">•••• {{ orderV2()?.payment?.last4 }}</strong>
            </div>
            <div class="info-row" *ngIf="orderV2()?.payment?.bankCode">
              <span>Ngân hàng:</span>
              <strong>{{ orderV2()?.payment?.bankCode }}</strong>
            </div>
            <div class="info-row" *ngIf="orderV2()?.payment?.transferRef">
              <span>Mã GD:</span>
              <strong class="font-mono">{{ orderV2()?.payment?.transferRef }}</strong>
            </div>

            <div class="summary-divider"></div>

            <div class="info-row total-row">
              <span>Tổng thanh toán:</span>
              <span class="total-val">{{ orderV2()?.total?.value | number:'1.0-0' }} VND</span>
            </div>
          </div>

          <!-- Shipping Address -->
          <div class="glass-card">
            <h3 class="sidebar-title">Địa Chỉ Giao Hàng</h3>
            <p class="address-text">{{ orderV2()?.shippingAddress }}</p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mb-4 { margin-bottom: 1.5rem; }
    .mt-2 { margin-top: 0.5rem; }
    .mt-3 { margin-top: 1rem; }
    .font-mono { font-family: var(--font-mono); }

    .order-title-line {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
      margin-bottom: 0.35rem;
    }

    .version-banner {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      padding: 1rem 1.25rem;

      .version-toggle {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        flex-wrap: wrap;
        .toggle-label {
          font-size: 0.85rem;
          color: var(--text-muted);
          margin-right: 0.5rem;
        }
      }

      .v1-notice {
        font-size: 0.8rem;
        color: #fca5a5;
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }
    }

    .grid-layout {
      display: grid;
      grid-template-columns: 1fr 360px;
      gap: 1.5rem;
      align-items: flex-start;

      @media (max-width: 960px) {
        grid-template-columns: 1fr;
      }
    }

    .card-title {
      font-size: 1.15rem;
      font-weight: 700;
      color: #ffffff;
      margin-bottom: 0.25rem;
    }

    .card-subtitle {
      font-size: 0.825rem;
      color: var(--text-muted);
    }

    .product-id-sub {
      font-size: 0.75rem;
      color: var(--text-dim);
      font-family: var(--font-mono);
    }

    .line-total {
      color: var(--accent-emerald);
      font-weight: 700;
    }

    /* Timeline */
    .timeline {
      margin-top: 1.5rem;
      position: relative;
      padding-left: 1.5rem;
      border-left: 2px solid var(--border-color);

      .timeline-item {
        position: relative;
        margin-bottom: 1.5rem;

        &:last-child {
          margin-bottom: 0;
        }

        .timeline-dot {
          position: absolute;
          left: -1.95rem;
          top: 0.25rem;
          width: 12px;
          height: 12px;
          border-radius: 50%;
          background: var(--primary);
          border: 2px solid var(--bg-dark);
          box-shadow: 0 0 8px var(--primary);
        }

        .timeline-content {
          background: rgba(255, 255, 255, 0.03);
          border: 1px solid var(--border-color);
          border-radius: var(--radius-sm);
          padding: 0.75rem 1rem;
        }

        .timeline-header {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          margin-bottom: 0.35rem;
          flex-wrap: wrap;

          .action-tag {
            font-weight: 700;
            font-size: 0.8rem;
            color: #ffffff;
          }

          .actor-badge {
            font-size: 0.7rem;
            padding: 0.1rem 0.4rem;
            background: rgba(255, 255, 255, 0.1);
            border-radius: 4px;
            color: var(--text-muted);
          }

          .timeline-time {
            margin-left: auto;
            font-size: 0.75rem;
            color: var(--text-dim);
          }
        }

        .status-change {
          font-size: 0.85rem;
          color: #93c5fd;
          margin-bottom: 0.25rem;
        }

        .timeline-reason {
          font-size: 0.8rem;
          color: var(--text-muted);
          font-style: italic;
        }
      }
    }

    .sidebar-title {
      font-size: 1.05rem;
      font-weight: 700;
      color: #ffffff;
      margin-bottom: 0.5rem;
    }

    .sidebar-desc {
      font-size: 0.8rem;
      color: var(--text-muted);
      margin-bottom: 1rem;
    }

    .action-block {
      .action-info {
        font-size: 0.825rem;
        color: var(--text-muted);
        margin-bottom: 0.75rem;
      }
      .btn-block {
        width: 100%;
        padding: 0.65rem;
      }
    }

    .no-actions-box {
      font-size: 0.8rem;
      color: var(--text-dim);
      padding: 0.75rem;
      border: 1px dashed var(--border-color);
      border-radius: var(--radius-sm);
      text-align: center;
    }

    .info-row {
      display: flex;
      justify-content: space-between;
      font-size: 0.85rem;
      color: var(--text-muted);
      margin-bottom: 0.5rem;
      strong { color: #ffffff; }

      &.total-row {
        align-items: baseline;
        .total-val {
          font-size: 1.25rem;
          font-weight: 800;
          color: var(--accent-emerald);
          font-family: var(--font-mono);
        }
      }
    }

    .summary-divider {
      height: 1px;
      background: var(--border-color);
      margin: 1rem 0;
    }

    .address-text {
      font-size: 0.85rem;
      color: var(--text-muted);
      line-height: 1.5;
    }
  `]
})
export class OrderDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);

  readonly orderV2 = signal<OrderResponseV2 | null>(null);
  readonly orderV1 = signal<OrderResponseV1 | null>(null);
  readonly events = signal<OrderEventResponse[]>([]);
  readonly isTransitioning = signal<boolean>(false);

  selectedVersionView: 'v1' | 'v2' = 'v2';
  code = '';

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      this.code = params['code'];
      if (this.code) {
        this.loadOrderData();
      }
    });
  }

  loadOrderData(): void {
    this.orderService.getOrderByCodeV2(this.code).subscribe({
      next: (order) => this.orderV2.set(order),
      error: (err) => this.toastService.error('Không tìm thấy đơn hàng', err)
    });

    this.orderService.getOrderByCodeV1(this.code).subscribe({
      next: (order) => this.orderV1.set(order),
      error: () => {}
    });

    this.orderService.getOrderEvents(this.code).subscribe({
      next: (evs) => this.events.set(evs),
      error: () => {}
    });
  }

  switchVersionView(v: 'v1' | 'v2'): void {
    this.selectedVersionView = v;
  }

  performCustomerAction(action: OrderAction): void {
    const order = this.orderV2();
    if (!order) return;

    let reason: string | undefined;
    if (action === 'CANCEL') {
      if (!confirm(`Bạn có chắc muốn hủy đơn hàng ${order.code}?`)) return;
      reason = 'Khách hàng yêu cầu hủy đơn qua giao diện Web';
    } else if (action === 'REQUEST_RETURN') {
      const input = prompt('Vui lòng nhập lý do yêu cầu trả hàng:');
      if (input === null) return;
      reason = input.trim() || 'Khách yêu cầu trả hàng trong 7 ngày';
    }

    this.isTransitioning.set(true);
    this.orderService
      .transitionOrder(order.code, { action, reason }, order.version)
      .subscribe({
        next: (res) => {
          this.isTransitioning.set(false);
          this.toastService.success('Thao tác thành công', res.message);
          this.loadOrderData();
        },
        error: (err) => {
          this.isTransitioning.set(false);
          this.toastService.error('Không thể thực hiện', err);
        }
      });
  }

  downloadPdfInvoice(): void {
    this.orderService.downloadInvoice(this.code).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${this.code}-invoice.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => this.toastService.error('Lỗi tải hóa đơn', err)
    });
  }
}
