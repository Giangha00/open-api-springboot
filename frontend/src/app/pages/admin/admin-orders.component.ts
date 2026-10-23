import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { OrderAction, OrderResponseV2, OrderStatus } from '../../models/order.model';

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>Quản Lý Đơn Hàng (ADMIN)</h1>
          <p>
            Quyền quản trị viên TechShop: duyệt đơn, xuất kho, upload hóa đơn PDF và xử lý hoàn trả.
          </p>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" (click)="loadOrders()">
            🔄 Làm mới
          </button>
        </div>
      </div>

      <!-- Quick Status Filter Pills -->
      <div class="glass-card mb-4 status-pills">
        <button
          class="pill-btn"
          [class.active]="selectedStatus === null"
          (click)="setStatusFilter(null)"
        >
          Tất cả ({{ orders().length }})
        </button>
        <button
          class="pill-btn"
          [class.active]="selectedStatus === 'PENDING'"
          (click)="setStatusFilter('PENDING')"
        >
          Chờ duyệt (PENDING)
        </button>
        <button
          class="pill-btn"
          [class.active]="selectedStatus === 'CONFIRMED'"
          (click)="setStatusFilter('CONFIRMED')"
        >
          Đã duyệt (CONFIRMED)
        </button>
        <button
          class="pill-btn"
          [class.active]="selectedStatus === 'SHIPPING'"
          (click)="setStatusFilter('SHIPPING')"
        >
          Đang giao (SHIPPING)
        </button>
        <button
          class="pill-btn"
          [class.active]="selectedStatus === 'RETURN_REQUESTED'"
          (click)="setStatusFilter('RETURN_REQUESTED')"
        >
          Yêu cầu trả (RETURN_REQUESTED)
        </button>
      </div>

      <!-- Orders Table -->
      <div class="glass-card">
        <div *ngIf="isLoading()" class="loading-state">
          <div class="spinner"></div>
          <p>Đang tải đơn hàng quản trị...</p>
        </div>

        <div *ngIf="!isLoading() && orders().length === 0" class="empty-state">
          <p>Không có đơn hàng nào.</p>
        </div>

        <div *ngIf="!isLoading() && orders().length > 0" class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Khách hàng</th>
                <th>Thời gian</th>
                <th>Trạng thái</th>
                <th>Tổng tiền</th>
                <th>Hóa đơn PDF</th>
                <th>Thao tác Admin</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let order of filteredOrders()">
                <td>
                  <a [routerLink]="['/orders', order.code]" class="order-code-link">
                    <strong>{{ order.code }}</strong>
                  </a>
                  <div class="version-sub font-mono">v{{ order.version }}</div>
                </td>
                <td>
                  <span class="customer-tag">ID: {{ order.customerId }}</span>
                </td>
                <td class="text-muted text-sm">
                  {{ order.createdAt | date:'HH:mm dd/MM/yyyy' }}
                </td>
                <td>
                  <span class="badge" [ngClass]="'badge-' + order.status">
                    {{ order.status }}
                  </span>
                </td>
                <td class="total-col">
                  <strong>{{ order.total.value | number:'1.0-0' }}</strong> {{ order.total.currency }}
                </td>
                <td>
                  <span *ngIf="order.hasInvoice" class="invoice-badge">Đã upload</span>
                  <button
                    *ngIf="!order.hasInvoice"
                    class="btn btn-outline btn-sm upload-btn"
                    (click)="openUploadModal(order)"
                  >
                    + Upload PDF
                  </button>
                </td>
                <td>
                  <div class="admin-action-buttons">
                    <!-- CONFIRM action -->
                    <button
                      *ngIf="order.status === 'PENDING'"
                      class="btn btn-primary btn-sm"
                      (click)="performAdminAction(order, 'CONFIRM')"
                    >
                      Duyệt Đơn
                    </button>

                    <!-- SHIP action -->
                    <button
                      *ngIf="order.status === 'CONFIRMED'"
                      class="btn btn-success btn-sm"
                      (click)="performAdminAction(order, 'SHIP')"
                    >
                      Xuất Kho
                    </button>

                    <!-- ACCEPT_RETURN action -->
                    <button
                      *ngIf="order.status === 'RETURN_REQUESTED'"
                      class="btn btn-warning btn-sm"
                      (click)="performAdminAction(order, 'ACCEPT_RETURN')"
                    >
                      Nhận Hàng Trả
                    </button>

                    <!-- CANCEL action -->
                    <button
                      *ngIf="order.status === 'PENDING' || order.status === 'CONFIRMED'"
                      class="btn btn-danger btn-sm"
                      (click)="openCancelModal(order)"
                    >
                      Hủy Đơn
                    </button>

                    <a [routerLink]="['/orders', order.code]" class="btn btn-secondary btn-sm">
                      Chi tiết
                    </a>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Cancel Modal (mandates reason for CONFIRMED) -->
      <div class="modal-backdrop" *ngIf="showCancelModal()">
        <div class="modal-content">
          <div class="modal-header">
            <h3>Hủy Đơn Hàng #{{ targetOrder()?.code }}</h3>
            <button class="close-btn" (click)="closeCancelModal()">&times;</button>
          </div>
          <div class="modal-body">
            <p class="modal-desc" *ngIf="targetOrder()?.status === 'CONFIRMED'">
              <strong style="color: var(--accent-rose)">Lưu ý:</strong>
              Đơn hàng đang ở trạng thái <code>CONFIRMED</code>. Theo quy tắc nghiệp vụ Section 10.4,
              ADMIN bắt buộc phải nhập lý do hủy đơn.
            </p>
            <div class="form-group mt-3">
              <label>Lý do hủy đơn *</label>
              <textarea
                class="form-control"
                rows="3"
                [(ngModel)]="cancelReason"
                placeholder="Nhập lý do chi tiết..."
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" (click)="closeCancelModal()">Đóng</button>
            <button class="btn btn-danger" (click)="submitCancel()">Xác Nhận Hủy</button>
          </div>
        </div>
      </div>

      <!-- Upload Invoice PDF Modal -->
      <div class="modal-backdrop" *ngIf="showUploadModal()">
        <div class="modal-content">
          <div class="modal-header">
            <h3>Tải Lên Hóa Đơn PDF cho #{{ targetOrder()?.code }}</h3>
            <button class="close-btn" (click)="closeUploadModal()">&times;</button>
          </div>
          <div class="modal-body">
            <p class="modal-desc">
              Chỉ chấp nhận file định dạng <strong>application/pdf</strong>, dung lượng tối đa <strong>5 MB</strong>.
            </p>
            <div class="form-group mt-3">
              <label>Chọn file PDF</label>
              <input
                type="file"
                class="form-control"
                accept="application/pdf"
                (change)="onFileSelected($event)"
              />
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" (click)="closeUploadModal()">Đóng</button>
            <button
              class="btn btn-primary"
              [disabled]="!selectedFile"
              (click)="submitUploadInvoice()"
            >
              Tải Lên Hóa Đơn
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mb-4 { margin-bottom: 1.5rem; }
    .mt-3 { margin-top: 1rem; }
    .text-sm { font-size: 0.8rem; }
    .text-muted { color: var(--text-muted); }
    .font-mono { font-family: var(--font-mono); }

    .status-pills {
      display: flex;
      gap: 0.5rem;
      padding: 0.75rem 1rem;
      flex-wrap: wrap;

      .pill-btn {
        background: transparent;
        border: 1px solid var(--border-color);
        color: var(--text-muted);
        padding: 0.35rem 0.75rem;
        border-radius: var(--radius-full);
        font-size: 0.8rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.15s ease;

        &:hover {
          background: rgba(255, 255, 255, 0.05);
          color: #ffffff;
        }

        &.active {
          background: rgba(99, 102, 241, 0.2);
          border-color: var(--primary);
          color: #ffffff;
        }
      }
    }

    .order-code-link {
      color: #60a5fa;
      font-family: var(--font-mono);
      &:hover { text-decoration: underline; }
    }
    .version-sub {
      font-size: 0.7rem;
      color: var(--text-dim);
    }
    .customer-tag {
      font-size: 0.75rem;
      background: rgba(255, 255, 255, 0.05);
      padding: 0.2rem 0.4rem;
      border-radius: 4px;
      color: var(--text-muted);
      font-family: var(--font-mono);
    }

    .total-col {
      font-family: var(--font-mono);
      color: var(--accent-emerald);
    }

    .invoice-badge {
      font-size: 0.75rem;
      color: #34d399;
      font-weight: 700;
    }
    .upload-btn {
      font-size: 0.75rem;
      padding: 0.25rem 0.5rem;
    }

    .admin-action-buttons {
      display: flex;
      gap: 0.4rem;
      flex-wrap: wrap;
    }

    .loading-state, .empty-state {
      text-align: center;
      padding: 3.5rem 1rem;
      color: var(--text-muted);
    }

    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid rgba(255, 255, 255, 0.1);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 1rem;
    }

    .modal-desc {
      font-size: 0.85rem;
      color: var(--text-muted);
      line-height: 1.5;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class AdminOrdersComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  readonly authService = inject(AuthService);

  readonly orders = signal<OrderResponseV2[]>([]);
  readonly isLoading = signal<boolean>(true);

  selectedStatus: OrderStatus | null = null;

  readonly showCancelModal = signal<boolean>(false);
  readonly showUploadModal = signal<boolean>(false);
  readonly targetOrder = signal<OrderResponseV2 | null>(null);

  cancelReason = '';
  selectedFile: File | null = null;

  ngOnInit(): void {
    // Automatically switch to admin profile if not already
    this.authService.selectProfile('admin');
    this.loadOrders();
  }

  setStatusFilter(status: OrderStatus | null): void {
    this.selectedStatus = status;
  }

  filteredOrders(): OrderResponseV2[] {
    if (!this.selectedStatus) return this.orders();
    return this.orders().filter((o) => o.status === this.selectedStatus);
  }

  loadOrders(): void {
    this.isLoading.set(true);
    // As admin, fetch up to 100 recent orders
    this.orderService.getOrders(undefined, undefined, undefined, undefined, 0, 100, 'createdAt,desc').subscribe({
      next: (pageRes) => {
        this.orders.set(pageRes.content);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.toastService.error('Lỗi tải đơn hàng', err);
      }
    });
  }

  performAdminAction(order: OrderResponseV2, action: OrderAction): void {
    let reason = 'Admin thực hiện hành động ' + action;
    this.orderService.adminTransitionOrder(order.code, { action, reason }, order.version).subscribe({
      next: (res) => {
        this.toastService.success('Thành công', res.message);
        this.loadOrders();
      },
      error: (err) => this.toastService.error('Lỗi thực hiện', err)
    });
  }

  openCancelModal(order: OrderResponseV2): void {
    this.targetOrder.set(order);
    this.cancelReason = '';
    this.showCancelModal.set(true);
  }

  closeCancelModal(): void {
    this.showCancelModal.set(false);
    this.targetOrder.set(null);
  }

  submitCancel(): void {
    const order = this.targetOrder();
    if (!order) return;

    if (order.status === 'CONFIRMED' && !this.cancelReason.trim()) {
      this.toastService.warning('Bắt buộc nhập lý do', 'Khi đơn hàng đã CONFIRMED, Admin bắt buộc phải có lý do hủy.');
      return;
    }

    this.orderService.adminTransitionOrder(order.code, { action: 'CANCEL', reason: this.cancelReason }, order.version).subscribe({
      next: (res) => {
        this.closeCancelModal();
        this.toastService.success('Đã hủy đơn hàng', res.message);
        this.loadOrders();
      },
      error: (err) => this.toastService.error('Lỗi khi hủy đơn', err)
    });
  }

  openUploadModal(order: OrderResponseV2): void {
    this.targetOrder.set(order);
    this.selectedFile = null;
    this.showUploadModal.set(true);
  }

  closeUploadModal(): void {
    this.showUploadModal.set(false);
    this.targetOrder.set(null);
    this.selectedFile = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];
    }
  }

  submitUploadInvoice(): void {
    const order = this.targetOrder();
    if (!order || !this.selectedFile) return;

    this.orderService.uploadInvoice(order.code, this.selectedFile).subscribe({
      next: (res) => {
        this.closeUploadModal();
        this.toastService.success('Upload thành công', res.message);
        this.loadOrders();
      },
      error: (err) => this.toastService.error('Lỗi tải hóa đơn', err)
    });
  }
}
