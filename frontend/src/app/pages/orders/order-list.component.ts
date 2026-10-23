import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { OrderResponseV2, OrderStatus } from '../../models/order.model';
import { PageResponse } from '../../models/page-response.model';

@Component({
  selector: 'app-order-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>Danh Sách Đơn Hàng</h1>
          <p>
            Xem lịch sử đơn hàng cá nhân của <strong>{{ authService.activeProfile().name }}</strong>
            (GET /api/orders)
          </p>
        </div>
        <div class="header-actions">
          <a routerLink="/orders/create" class="btn btn-primary">
            <span>+</span> Đặt Hàng Mới
          </a>
        </div>
      </div>

      <!-- Filters Glass Card -->
      <div class="glass-card mb-4 filters-card">
        <div class="filter-grid">
          <div class="filter-item">
            <label>Trạng thái</label>
            <select class="form-control" [(ngModel)]="selectedStatus" (change)="onFilterChange()">
              <option [ngValue]="null">Tất cả trạng thái</option>
              <option value="PENDING">PENDING (Chờ xác nhận)</option>
              <option value="CONFIRMED">CONFIRMED (Đã duyệt)</option>
              <option value="SHIPPING">SHIPPING (Đang giao)</option>
              <option value="DELIVERED">DELIVERED (Đã giao)</option>
              <option value="CANCELLED">CANCELLED (Đã hủy)</option>
              <option value="RETURN_REQUESTED">RETURN_REQUESTED (Yêu cầu trả)</option>
              <option value="RETURNED">RETURNED (Đã trả hàng)</option>
            </select>
          </div>

          <div class="filter-item">
            <label>Từ ngày</label>
            <input
              type="date"
              class="form-control"
              [(ngModel)]="fromDate"
              (change)="onFilterChange()"
            />
          </div>

          <div class="filter-item">
            <label>Đến ngày</label>
            <input
              type="date"
              class="form-control"
              [(ngModel)]="toDate"
              (change)="onFilterChange()"
            />
          </div>

          <div class="filter-item">
            <label>Giá trị tối thiểu (VND)</label>
            <input
              type="number"
              class="form-control"
              placeholder="VD: 1000000"
              [(ngModel)]="minTotal"
              (input)="onFilterChange()"
            />
          </div>

          <div class="filter-item">
            <label>Sắp xếp (Sort)</label>
            <select class="form-control" [(ngModel)]="selectedSort" (change)="onFilterChange()">
              <option value="createdAt,desc">Mới nhất trước</option>
              <option value="createdAt,asc">Cũ nhất trước</option>
              <option value="totalAmount,desc">Tổng tiền cao nhất</option>
              <option value="totalAmount,asc">Tổng tiền thấp nhất</option>
            </select>
          </div>
        </div>
      </div>

      <!-- Orders List Table -->
      <div class="glass-card">
        <div *ngIf="isLoading()" class="loading-state">
          <div class="spinner"></div>
          <p>Đang tải danh sách đơn hàng...</p>
        </div>

        <div *ngIf="!isLoading() && orders().length === 0" class="empty-state">
          <p>Không tìm thấy đơn hàng nào phù hợp với bộ lọc hiện tại.</p>
          <a routerLink="/orders/create" class="btn btn-outline btn-sm mt-3">Tạo đơn hàng ngay</a>
        </div>

        <div *ngIf="!isLoading() && orders().length > 0" class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Thời điểm tạo</th>
                <th>Trạng thái</th>
                <th>Tổng thanh toán</th>
                <th>PT Thanh toán</th>
                <th>Version (ETag)</th>
                <th>Hóa đơn PDF</th>
                <th style="text-align: right;">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let order of orders()" class="order-row">
                <td>
                  <a [routerLink]="['/orders', order.code]" class="order-code-link">
                    <strong>{{ order.code }}</strong>
                  </a>
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
                  <span class="payment-method-tag">{{ order.payment.method }}</span>
                </td>
                <td>
                  <span class="badge badge-version">v{{ order.version }}</span>
                </td>
                <td>
                  <span *ngIf="order.hasInvoice" class="invoice-badge">Đã có</span>
                  <span *ngIf="!order.hasInvoice" class="no-invoice-badge">Chưa có</span>
                </td>
                <td style="text-align: right;">
                  <a [routerLink]="['/orders', order.code]" class="btn btn-secondary btn-sm">
                    Chi Tiết &rarr;
                  </a>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination Controls -->
        <div *ngIf="!isLoading() && totalElements() > 0" class="pagination-bar">
          <div class="pagination-info">
            Trang <strong>{{ page() + 1 }}</strong> / {{ totalPages() }} (Tổng số {{ totalElements() }} đơn)
          </div>
          <div class="pagination-buttons">
            <button
              class="btn btn-secondary btn-sm"
              [disabled]="page() === 0"
              (click)="changePage(page() - 1)"
            >
              &larr; Trang trước
            </button>
            <button
              class="btn btn-secondary btn-sm"
              [disabled]="page() >= totalPages() - 1"
              (click)="changePage(page() + 1)"
            >
              Trang sau &rarr;
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

    .filters-card {
      padding: 1.25rem;
    }

    .filter-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 1rem;
      align-items: flex-end;
    }

    .filter-item {
      label {
        display: block;
        font-size: 0.8rem;
        font-weight: 600;
        color: var(--text-muted);
        margin-bottom: 0.35rem;
      }
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

    .order-code-link {
      color: #60a5fa;
      font-family: var(--font-mono);
      &:hover { color: #93c5fd; text-decoration: underline; }
    }

    .total-col {
      font-family: var(--font-mono);
      color: var(--accent-emerald);
    }

    .payment-method-tag {
      font-size: 0.8rem;
      font-weight: 600;
      color: var(--text-muted);
      background: rgba(255, 255, 255, 0.05);
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
    }

    .invoice-badge {
      font-size: 0.75rem;
      color: #34d399;
      font-weight: 600;
    }
    .no-invoice-badge {
      font-size: 0.75rem;
      color: var(--text-dim);
    }

    .pagination-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 1.25rem;
      margin-top: 1.25rem;
      border-top: 1px solid var(--border-color);
      flex-wrap: wrap;
      gap: 1rem;
      font-size: 0.875rem;
      color: var(--text-muted);
    }

    .pagination-buttons {
      display: flex;
      gap: 0.5rem;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class OrderListComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  readonly authService = inject(AuthService);

  readonly orders = signal<OrderResponseV2[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly page = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly totalElements = signal<number>(0);
  readonly totalPages = signal<number>(1);

  selectedStatus: OrderStatus | null = null;
  fromDate = '';
  toDate = '';
  minTotal: number | null = null;
  selectedSort = 'createdAt,desc';

  ngOnInit(): void {
    this.loadOrders();
  }

  onFilterChange(): void {
    this.page.set(0);
    this.loadOrders();
  }

  changePage(newPage: number): void {
    this.page.set(newPage);
    this.loadOrders();
  }

  loadOrders(): void {
    this.isLoading.set(true);
    this.orderService
      .getOrders(
        this.selectedStatus || undefined,
        this.fromDate || undefined,
        this.toDate || undefined,
        this.minTotal != null && this.minTotal > 0 ? this.minTotal : undefined,
        this.page(),
        this.pageSize(),
        this.selectedSort
      )
      .subscribe({
        next: (pageRes: PageResponse<OrderResponseV2>) => {
          this.orders.set(pageRes.content);
          this.totalElements.set(pageRes.totalElements);
          this.totalPages.set(pageRes.totalPages);
          this.isLoading.set(false);
        },
        error: (err) => {
          this.isLoading.set(false);
          this.toastService.error('Lỗi tải danh sách đơn hàng', err);
        }
      });
  }
}
