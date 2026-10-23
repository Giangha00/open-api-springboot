import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { AuthService } from '../../services/auth.service';
import { DailyRevenueReportResponse } from '../../models/order.model';

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>Báo Cáo Doanh Thu Theo Ngày</h1>
          <p>Thống kê số lượng đơn hàng và doanh thu thực tế (REST API: GET /api/admin/reports/daily-revenue)</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="loadReport()">
            🔄 Cập nhật báo cáo
          </button>
        </div>
      </div>

      <!-- Date Filter Bar -->
      <div class="glass-card mb-4 filter-bar">
        <div class="filter-inputs">
          <div class="filter-field">
            <label>Từ ngày:</label>
            <input type="date" class="form-control" [(ngModel)]="fromDate" />
          </div>
          <div class="filter-field">
            <label>Đến ngày:</label>
            <input type="date" class="form-control" [(ngModel)]="toDate" />
          </div>
          <button class="btn btn-secondary filter-btn" (click)="loadReport()">
            Áp Dụng Lọc
          </button>
        </div>
      </div>

      <!-- KPI Summary Cards -->
      <div class="kpi-grid mb-4">
        <div class="glass-card kpi-card">
          <span class="kpi-label">Tổng Doanh Thu</span>
          <div class="kpi-val">{{ totalRevenue() | number:'1.0-0' }} <span class="curr">VND</span></div>
          <span class="kpi-sub">Trong khoảng thời gian đã chọn</span>
        </div>

        <div class="glass-card kpi-card">
          <span class="kpi-label">Tổng Số Đơn Hàng</span>
          <div class="kpi-val text-cyan">{{ totalOrders() }} <span class="curr">đơn</span></div>
          <span class="kpi-sub">Đơn không bị hủy / trả lại</span>
        </div>

        <div class="glass-card kpi-card">
          <span class="kpi-label">Giá Trị Trung Bình / Đơn</span>
          <div class="kpi-val text-amber">
            {{ (totalOrders() > 0 ? totalRevenue() / totalOrders() : 0) | number:'1.0-0' }}
            <span class="curr">VND</span>
          </div>
          <span class="kpi-sub">AOV (Average Order Value)</span>
        </div>
      </div>

      <!-- Visual Chart Section -->
      <div class="glass-card mb-4" *ngIf="reports().length > 0">
        <h3 class="card-title">Biểu Đồ Doanh Thu Từng Ngày</h3>
        <p class="card-subtitle">Xu hướng tăng trưởng doanh thu theo ngày</p>

        <div class="chart-wrapper mt-4">
          <div *ngFor="let item of reports()" class="chart-col">
            <div class="col-bar-container">
              <div
                class="col-bar"
                [style.height.%]="getBarHeight(item.totalRevenue)"
                [title]="(item.totalRevenue | number:'1.0-0') + ' VND'"
              >
                <span class="bar-tooltip" *ngIf="item.totalRevenue > 0">
                  {{ item.totalRevenue / 1000000 | number:'1.1-1' }}M
                </span>
              </div>
            </div>
            <div class="col-date">{{ item.date | slice:5 }}</div>
          </div>
        </div>
      </div>

      <!-- Detail Table -->
      <div class="glass-card">
        <h3 class="card-title">Bảng Chi Tiết Doanh Thu</h3>
        <div *ngIf="isLoading()" class="loading-state">
          <div class="spinner"></div>
          <p>Đang tải dữ liệu thống kê...</p>
        </div>

        <div *ngIf="!isLoading() && reports().length === 0" class="empty-state">
          <p>Không có dữ liệu doanh thu trong khoảng thời gian này.</p>
        </div>

        <div *ngIf="!isLoading() && reports().length > 0" class="table-responsive mt-3">
          <table class="data-table">
            <thead>
              <tr>
                <th>Ngày</th>
                <th>Số đơn hàng</th>
                <th>Doanh thu ghi nhận</th>
                <th>Đơn vị</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let row of reports()">
                <td><strong>{{ row.date }}</strong></td>
                <td>
                  <span class="badge" [ngClass]="row.totalOrders > 0 ? 'badge-CONFIRMED' : 'badge-RETURNED'">
                    {{ row.totalOrders }} đơn
                  </span>
                </td>
                <td class="font-mono revenue-val">
                  {{ row.totalRevenue | number:'1.0-0' }}
                </td>
                <td class="text-muted">{{ row.currency }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mb-4 { margin-bottom: 1.5rem; }
    .mt-3 { margin-top: 1rem; }
    .mt-4 { margin-top: 1.5rem; }
    .font-mono { font-family: var(--font-mono); }

    .filter-bar {
      padding: 1rem 1.25rem;
      .filter-inputs {
        display: flex;
        gap: 1rem;
        align-items: flex-end;
        flex-wrap: wrap;

        .filter-field {
          flex: 1;
          min-width: 180px;
          label {
            display: block;
            font-size: 0.8rem;
            font-weight: 600;
            color: var(--text-muted);
            margin-bottom: 0.35rem;
          }
        }
        .filter-btn {
          height: 38px;
        }
      }
    }

    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1.25rem;
    }

    .kpi-card {
      display: flex;
      flex-direction: column;
      padding: 1.25rem 1.5rem;

      .kpi-label {
        font-size: 0.8rem;
        font-weight: 700;
        text-transform: uppercase;
        color: var(--text-dim);
        letter-spacing: 0.05em;
        margin-bottom: 0.5rem;
      }

      .kpi-val {
        font-size: 1.65rem;
        font-weight: 800;
        font-family: var(--font-mono);
        color: var(--accent-emerald);
        line-height: 1.2;
        margin-bottom: 0.35rem;

        &.text-cyan { color: var(--accent-cyan); }
        &.text-amber { color: var(--accent-amber); }

        .curr {
          font-size: 0.9rem;
          font-weight: 600;
          color: var(--text-dim);
        }
      }

      .kpi-sub {
        font-size: 0.775rem;
        color: var(--text-muted);
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

    .chart-wrapper {
      display: flex;
      align-items: flex-end;
      gap: 0.75rem;
      height: 200px;
      padding: 1rem 0;
      overflow-x: auto;
    }

    .chart-col {
      flex: 1;
      min-width: 44px;
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;

      .col-bar-container {
        flex: 1;
        width: 100%;
        display: flex;
        align-items: flex-end;
        justify-content: center;
      }

      .col-bar {
        width: 75%;
        max-width: 32px;
        min-height: 4px;
        background: linear-gradient(180deg, #6366f1 0%, #3b82f6 100%);
        border-radius: 4px 4px 0 0;
        position: relative;
        transition: height 0.4s ease;

        &:hover {
          background: linear-gradient(180deg, #818cf8 0%, #60a5fa 100%);
        }

        .bar-tooltip {
          position: absolute;
          top: -20px;
          left: 50%;
          transform: translateX(-50%);
          font-size: 0.65rem;
          font-family: var(--font-mono);
          color: #a5b4fc;
          font-weight: bold;
          white-space: nowrap;
        }
      }

      .col-date {
        font-size: 0.75rem;
        font-family: var(--font-mono);
        color: var(--text-dim);
        margin-top: 0.5rem;
      }
    }

    .revenue-val {
      font-size: 1.05rem;
      font-weight: 700;
      color: var(--accent-emerald);
    }

    .loading-state, .empty-state {
      text-align: center;
      padding: 3rem 1rem;
      color: var(--text-muted);
    }

    .spinner {
      width: 32px;
      height: 32px;
      border: 3px solid rgba(255, 255, 255, 0.1);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 0.75rem;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class AdminReportsComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  readonly authService = inject(AuthService);

  readonly reports = signal<DailyRevenueReportResponse[]>([]);
  readonly isLoading = signal<boolean>(true);

  fromDate = '';
  toDate = '';

  ngOnInit(): void {
    this.authService.selectProfile('admin');

    // Default last 14 days
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 13);

    this.fromDate = start.toISOString().substring(0, 10);
    this.toDate = end.toISOString().substring(0, 10);

    this.loadReport();
  }

  loadReport(): void {
    this.isLoading.set(true);
    this.orderService.getDailyRevenue(this.fromDate, this.toDate).subscribe({
      next: (list) => {
        this.reports.set(list);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.toastService.error('Lỗi tải báo cáo', err);
      }
    });
  }

  totalRevenue(): number {
    return this.reports().reduce((sum, r) => sum + r.totalRevenue, 0);
  }

  totalOrders(): number {
    return this.reports().reduce((sum, r) => sum + r.totalOrders, 0);
  }

  getBarHeight(revenue: number): number {
    const maxRev = Math.max(...this.reports().map((r) => r.totalRevenue), 1);
    if (revenue === 0) return 3;
    return Math.max(8, (revenue / maxRev) * 100);
  }
}
