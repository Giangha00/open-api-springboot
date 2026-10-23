import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { Product } from '../../models/product.model';
import {
  CreateOrderRequest,
  OrderItemRequest,
  PaymentMethod,
  PaymentRequest
} from '../../models/order.model';

interface CartItem {
  product: Product;
  quantity: number;
}

@Component({
  selector: 'app-order-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>Tạo Đơn Hàng Mới</h1>
          <p>Tạo đơn hàng theo chuẩn OpenAPI 3.1 & Idempotency Header (REST API: POST /api/orders)</p>
        </div>
        <div class="header-actions">
          <a routerLink="/orders" class="btn btn-secondary">
            <span>&larr;</span> Lịch Sử Đơn Hàng
          </a>
        </div>
      </div>

      <div class="grid-layout">
        <!-- Left Column: Products & Items -->
        <div class="col-main">
          <!-- Step 1: Add products -->
          <div class="glass-card mb-4">
            <h2 class="card-title">1. Chọn Sản Phẩm Vào Đơn Hàng</h2>
            <p class="card-subtitle">
              Mỗi dòng số lượng từ 1 đến 20; tối đa 50 dòng; giá được tự động tính tại server.
            </p>

            <div class="product-selector-row">
              <select class="form-control" [(ngModel)]="selectedProductId">
                <option [ngValue]="null">-- Chọn sản phẩm để thêm vào đơn --</option>
                <option *ngFor="let p of availableProducts()" [ngValue]="p.id">
                  {{ p.name }} ({{ p.price | number:'1.0-0' }} VND)
                </option>
              </select>
              <button class="btn btn-primary" (click)="addItemToCart()" [disabled]="!selectedProductId">
                + Thêm
              </button>
            </div>

            <!-- Cart Table -->
            <div *ngIf="cartItems().length === 0" class="empty-cart-state">
              <p>Chưa có sản phẩm nào trong đơn hàng. Vui lòng chọn sản phẩm phía trên.</p>
            </div>

            <div *ngIf="cartItems().length > 0" class="table-responsive mt-3">
              <table class="data-table">
                <thead>
                  <tr>
                    <th>Sản phẩm</th>
                    <th>Đơn giá</th>
                    <th style="width: 140px;">Số lượng (1-20)</th>
                    <th>Thành tiền</th>
                    <th style="width: 60px;"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of cartItems(); let i = index">
                    <td>
                      <strong>{{ item.product.name }}</strong>
                    </td>
                    <td>{{ item.product.price | number:'1.0-0' }} VND</td>
                    <td>
                      <div class="qty-stepper">
                        <button type="button" class="stepper-btn" (click)="changeQty(item, -1)">-</button>
                        <span class="qty-value">{{ item.quantity }}</span>
                        <button type="button" class="stepper-btn" (click)="changeQty(item, 1)">+</button>
                      </div>
                    </td>
                    <td class="line-total">
                      {{ (item.product.price * item.quantity) | number:'1.0-0' }} VND
                    </td>
                    <td>
                      <button type="button" class="btn-remove" (click)="removeItem(i)">&times;</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Step 2: Shipping & Payment -->
          <div class="glass-card mb-4">
            <h2 class="card-title">2. Địa Chỉ & Phương Thức Thanh Toán (Đa hình)</h2>

            <div class="form-group">
              <label>Địa chỉ nhận hàng chi tiết * (Tối đa 300 ký tự)</label>
              <textarea
                class="form-control"
                maxlength="300"
                rows="2"
                [(ngModel)]="shippingAddress"
                placeholder="VD: Số 8 Tôn Thất Thuyết, Dịch Vọng Hậu, Cầu Giấy, Hà Nội"
              ></textarea>
            </div>

            <div class="payment-tabs-section mt-4">
              <label class="section-label">Phương thức thanh toán (OneOf Schema):</label>
              <div class="payment-tabs">
                <button
                  type="button"
                  class="tab-btn"
                  [class.active]="selectedMethod === 'COD'"
                  (click)="selectedMethod = 'COD'"
                >
                  <span class="tab-icon">💵</span>
                  <span>Thanh toán COD</span>
                </button>
                <button
                  type="button"
                  class="tab-btn"
                  [class.active]="selectedMethod === 'CARD'"
                  (click)="selectedMethod = 'CARD'"
                >
                  <span class="tab-icon">💳</span>
                  <span>Thẻ quốc tế (CARD)</span>
                </button>
                <button
                  type="button"
                  class="tab-btn"
                  [class.active]="selectedMethod === 'BANK_TRANSFER'"
                  (click)="selectedMethod = 'BANK_TRANSFER'"
                >
                  <span class="tab-icon">🏦</span>
                  <span>Chuyển khoản ngân hàng</span>
                </button>
              </div>

              <!-- CARD inputs -->
              <div *ngIf="selectedMethod === 'CARD'" class="sub-form glass-card mt-3">
                <div class="form-row">
                  <div class="form-group flex-1">
                    <label>Loại thẻ *</label>
                    <select class="form-control" [(ngModel)]="cardProvider">
                      <option value="VISA">VISA</option>
                      <option value="MasterCard">MasterCard</option>
                      <option value="JCB">JCB</option>
                    </select>
                  </div>
                  <div class="form-group flex-1">
                    <label>4 số cuối thẻ * (Ví dụ: 4242)</label>
                    <input
                      type="text"
                      class="form-control"
                      maxlength="4"
                      pattern="\\d{4}"
                      [(ngModel)]="cardLast4"
                      placeholder="4242"
                    />
                  </div>
                </div>
              </div>

              <!-- BANK_TRANSFER inputs -->
              <div *ngIf="selectedMethod === 'BANK_TRANSFER'" class="sub-form glass-card mt-3">
                <div class="form-row">
                  <div class="form-group flex-1">
                    <label>Ngân hàng *</label>
                    <select class="form-control" [(ngModel)]="bankCode">
                      <option value="VCB">Vietcombank (VCB)</option>
                      <option value="TCB">Techcombank (TCB)</option>
                      <option value="MB">MBBank (MB)</option>
                      <option value="ACB">ACB</option>
                    </select>
                  </div>
                  <div class="form-group flex-1">
                    <label>Mã giao dịch / Tham chiếu *</label>
                    <input
                      type="text"
                      class="form-control"
                      [(ngModel)]="transferRef"
                      placeholder="TXN-2026-998877"
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right Column: Idempotency & Summary -->
        <div class="col-sidebar">
          <!-- Idempotency Settings -->
          <div class="glass-card mb-4 idempotency-card">
            <h3 class="sidebar-title">⚡ Khóa Idempotency-Key</h3>
            <p class="sidebar-desc">
              Header bắt buộc của RFC/OpenAPI giúp chống trùng lặp thanh toán khi mạng chập chờn.
            </p>

            <div class="form-group">
              <label>Idempotency-Key hiện tại:</label>
              <input
                type="text"
                class="form-control font-mono"
                [(ngModel)]="idempotencyKey"
              />
            </div>

            <div class="idemp-actions">
              <button type="button" class="btn btn-secondary btn-sm" (click)="generateNewKey()">
                Tạo Key Mới
              </button>
            </div>
          </div>

          <!-- Order Summary Card -->
          <div class="glass-card summary-card">
            <h3 class="sidebar-title">Tổng Quan Đơn Hàng</h3>

            <div class="summary-row">
              <span>Số loại sản phẩm:</span>
              <strong>{{ cartItems().length }} mặt hàng</strong>
            </div>
            <div class="summary-row">
              <span>Tổng số lượng:</span>
              <strong>{{ totalQty() }} chiếc</strong>
            </div>
            <div class="summary-row">
              <span>Phương thức:</span>
              <span class="badge badge-version">{{ selectedMethod }}</span>
            </div>

            <div class="summary-divider"></div>

            <div class="total-row">
              <span class="total-label">Tổng thanh toán:</span>
              <span class="total-val">{{ estimatedTotal() | number:'1.0-0' }} VND</span>
            </div>

            <button
              type="button"
              class="btn btn-primary btn-block mt-4"
              [disabled]="cartItems().length === 0 || !shippingAddress.trim() || isSubmitting()"
              (click)="submitOrder()"
            >
              {{ isSubmitting() ? 'Đang tạo đơn...' : 'Xác Nhận Đặt Hàng' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mb-4 { margin-bottom: 1.5rem; }
    .mt-3 { margin-top: 1rem; }
    .mt-4 { margin-top: 1.5rem; }
    .font-mono { font-family: var(--font-mono); font-size: 0.8rem; }

    .grid-layout {
      display: grid;
      grid-template-columns: 1fr 380px;
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
      margin-bottom: 1.25rem;
    }

    .product-selector-row {
      display: flex;
      gap: 0.75rem;
    }

    .empty-cart-state {
      padding: 2.5rem 1rem;
      text-align: center;
      color: var(--text-dim);
      border: 1px dashed var(--border-color);
      border-radius: var(--radius-sm);
      margin-top: 1rem;
    }

    .qty-stepper {
      display: inline-flex;
      align-items: center;
      background: var(--bg-input);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-sm);
      overflow: hidden;

      .stepper-btn {
        width: 28px;
        height: 28px;
        background: transparent;
        border: none;
        color: #f1f5f9;
        font-weight: bold;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        &:hover { background: rgba(255, 255, 255, 0.1); }
      }

      .qty-value {
        width: 32px;
        text-align: center;
        font-size: 0.85rem;
        font-weight: 700;
        font-family: var(--font-mono);
      }
    }

    .line-total {
      font-weight: 700;
      color: var(--accent-emerald);
      font-family: var(--font-mono);
    }

    .btn-remove {
      background: transparent;
      border: none;
      color: var(--accent-rose);
      font-size: 1.25rem;
      cursor: pointer;
      &:hover { transform: scale(1.15); }
    }

    .payment-tabs {
      display: flex;
      gap: 0.75rem;
      flex-wrap: wrap;

      .tab-btn {
        flex: 1;
        min-width: 140px;
        background: var(--bg-input);
        border: 1px solid var(--border-color);
        border-radius: var(--radius-sm);
        padding: 0.75rem 0.85rem;
        color: var(--text-muted);
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.35rem;
        cursor: pointer;
        font-size: 0.825rem;
        font-weight: 600;
        transition: all 0.2s ease;

        .tab-icon { font-size: 1.25rem; }

        &.active {
          background: rgba(99, 102, 241, 0.15);
          border-color: var(--primary);
          color: #ffffff;
          box-shadow: 0 0 12px rgba(99, 102, 241, 0.25);
        }
      }
    }

    .sub-form {
      padding: 1rem;
      background: rgba(17, 23, 38, 0.6);
      .form-row {
        display: flex;
        gap: 1rem;
        .flex-1 { flex: 1; margin-bottom: 0; }
      }
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
      line-height: 1.4;
    }

    .idemp-actions {
      display: flex;
      gap: 0.5rem;
    }

    .summary-card {
      .summary-row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 0.875rem;
        margin-bottom: 0.65rem;
        color: var(--text-muted);
        strong { color: #ffffff; }
      }

      .summary-divider {
        height: 1px;
        background: var(--border-color);
        margin: 1.25rem 0;
      }

      .total-row {
        display: flex;
        justify-content: space-between;
        align-items: baseline;
        .total-label {
          font-weight: 700;
          font-size: 1rem;
        }
        .total-val {
          font-size: 1.4rem;
          font-weight: 800;
          color: var(--accent-emerald);
          font-family: var(--font-mono);
        }
      }

      .btn-block {
        width: 100%;
        padding: 0.85rem;
        font-size: 1rem;
      }
    }
  `]
})
export class OrderCreateComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly orderService = inject(OrderService);
  private readonly toastService = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly availableProducts = signal<Product[]>([]);
  readonly cartItems = signal<CartItem[]>([]);
  readonly isSubmitting = signal<boolean>(false);

  selectedProductId: number | null = null;
  shippingAddress = 'Số 8 Tôn Thất Thuyết, Cầu Giấy, Hà Nội';
  selectedMethod: PaymentMethod = 'COD';

  cardProvider = 'VISA';
  cardLast4 = '4242';
  bankCode = 'VCB';
  transferRef = 'TXN-' + Math.floor(100000 + Math.random() * 900000);

  idempotencyKey = '';

  ngOnInit(): void {
    this.generateNewKey();
    this.productService.getProducts().subscribe({
      next: (products) => {
        this.availableProducts.set(products);
        this.checkQueryParams();
      },
      error: (err) => this.toastService.error('Lỗi tải sản phẩm', err)
    });
  }

  generateNewKey(): void {
    this.idempotencyKey = 'REQ-' + Date.now() + '-' + Math.random().toString(36).substring(2, 7);
  }

  private checkQueryParams(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['productId']) {
        const pId = Number(params['productId']);
        const found = this.availableProducts().find((p) => p.id === pId);
        if (found) {
          this.cartItems.set([{ product: found, quantity: 1 }]);
        }
      }
    });
  }

  addItemToCart(): void {
    if (!this.selectedProductId) return;

    const prod = this.availableProducts().find((p) => p.id === this.selectedProductId);
    if (!prod) return;

    const existing = this.cartItems().find((item) => item.product.id === prod.id);
    if (existing) {
      if (existing.quantity < 20) {
        existing.quantity++;
        this.cartItems.update((items) => [...items]);
      } else {
        this.toastService.warning('Giới hạn số lượng', 'Mỗi sản phẩm chỉ được đặt tối đa 20 chiếc.');
      }
    } else {
      if (this.cartItems().length >= 50) {
        this.toastService.warning('Giới hạn đơn hàng', 'Tối đa 50 dòng sản phẩm cho mỗi đơn hàng.');
        return;
      }
      this.cartItems.update((items) => [...items, { product: prod, quantity: 1 }]);
    }
    this.selectedProductId = null;
  }

  changeQty(item: CartItem, delta: number): void {
    const newQty = item.quantity + delta;
    if (newQty >= 1 && newQty <= 20) {
      item.quantity = newQty;
      this.cartItems.update((items) => [...items]);
    }
  }

  removeItem(index: number): void {
    this.cartItems.update((items) => items.filter((_, i) => i !== index));
  }

  totalQty(): number {
    return this.cartItems().reduce((sum, item) => sum + item.quantity, 0);
  }

  estimatedTotal(): number {
    return this.cartItems().reduce((sum, item) => sum + item.product.price * item.quantity, 0);
  }

  submitOrder(): void {
    if (this.cartItems().length === 0) {
      this.toastService.warning('Giỏ hàng rỗng', 'Vui lòng chọn ít nhất 1 sản phẩm.');
      return;
    }
    if (!this.shippingAddress.trim()) {
      this.toastService.warning('Thiếu địa chỉ', 'Vui lòng nhập địa chỉ nhận hàng.');
      return;
    }

    let paymentReq: PaymentRequest;
    if (this.selectedMethod === 'CARD') {
      if (!this.cardLast4 || !/^\d{4}$/.test(this.cardLast4)) {
        this.toastService.warning('Thẻ không hợp lệ', '4 số cuối thẻ phải đúng 4 chữ số.');
        return;
      }
      paymentReq = {
        method: 'CARD',
        provider: this.cardProvider,
        last4: this.cardLast4
      };
    } else if (this.selectedMethod === 'BANK_TRANSFER') {
      if (!this.transferRef.trim()) {
        this.toastService.warning('Thiếu mã tham chiếu', 'Vui lòng nhập mã giao dịch ngân hàng.');
        return;
      }
      paymentReq = {
        method: 'BANK_TRANSFER',
        bankCode: this.bankCode,
        transferRef: this.transferRef
      };
    } else {
      paymentReq = { method: 'COD' };
    }

    const itemRequests: OrderItemRequest[] = this.cartItems().map((it) => ({
      productId: it.product.id,
      quantity: it.quantity
    }));

    const request: CreateOrderRequest = {
      shippingAddress: this.shippingAddress,
      items: itemRequests,
      payment: paymentReq
    };

    this.isSubmitting.set(true);
    this.orderService.createOrder(request, this.idempotencyKey).subscribe({
      next: (order) => {
        this.isSubmitting.set(false);
        this.toastService.success(
          'Tạo đơn thành công!',
          `Đơn hàng mã ${order.code} đã được tạo với trạng thái PENDING.`
        );
        this.router.navigate(['/orders', order.code]);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.toastService.error('Không thể tạo đơn hàng', err);
      }
    });
  }
}
