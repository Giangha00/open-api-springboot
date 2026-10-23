import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ProductService } from '../../services/product.service';
import { ToastService } from '../../services/toast.service';
import { Product, ProductRequest } from '../../models/product.model';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>Danh Mục Sản Phẩm</h1>
          <p>Quản lý các mặt hàng công nghệ tại TechShop Catalog (REST API: /api/v1/products)</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-primary" (click)="openCreateModal()">
            <span>+</span> Thêm Sản Phẩm Mới
          </button>
        </div>
      </div>

      <!-- Search & Stats Bar -->
      <div class="glass-card mb-4 search-bar">
        <div class="search-input-wrapper">
          <input
            type="text"
            class="form-control"
            placeholder="Tìm kiếm sản phẩm theo tên hoặc mô tả..."
            [(ngModel)]="searchQuery"
          />
        </div>
        <div class="stats-text">
          Hiển thị <strong>{{ filteredProducts().length }}</strong> / {{ products().length }} sản phẩm
        </div>
      </div>

      <!-- Loading State -->
      <div *ngIf="isLoading()" class="loading-state glass-card">
        <div class="spinner"></div>
        <p>Đang tải danh sách sản phẩm...</p>
      </div>

      <!-- Products Grid -->
      <div *ngIf="!isLoading()" class="product-grid">
        <div *ngFor="let p of filteredProducts()" class="glass-card product-card interactive">
          <div class="card-badge">#{{ p.id }}</div>
          <div class="product-info">
            <h3 class="product-name">{{ p.name }}</h3>
            <p class="product-desc">{{ p.description || 'Chưa có mô tả chi tiết.' }}</p>
            <div class="product-price">
              <span class="price-val">{{ p.price | number:'1.0-0' }}</span>
              <span class="price-curr">VND</span>
            </div>
          </div>
          <div class="product-actions">
            <button class="btn btn-secondary btn-sm" (click)="openEditModal(p)">Sửa</button>
            <button class="btn btn-danger btn-sm" (click)="deleteProduct(p)">Xóa</button>
            <a [routerLink]="['/orders/create']" [queryParams]="{ productId: p.id }" class="btn btn-outline btn-sm buy-now">
              Mua Ngay
            </a>
          </div>
        </div>
      </div>

      <!-- Create / Edit Modal -->
      <div class="modal-backdrop" *ngIf="showModal()">
        <div class="modal-content">
          <div class="modal-header">
            <h3>{{ editingProduct() ? 'Chỉnh Sửa Sản Phẩm' : 'Thêm Sản Phẩm Mới' }}</h3>
            <button class="close-btn" (click)="closeModal()">&times;</button>
          </div>

          <form (ngSubmit)="saveProduct()">
            <div class="form-group">
              <label>Tên sản phẩm * (Tối đa 120 ký tự)</label>
              <input
                type="text"
                class="form-control"
                maxlength="120"
                required
                [(ngModel)]="formData.name"
                name="name"
                placeholder="VD: Bàn phím cơ K8 Pro"
              />
            </div>

            <div class="form-group">
              <label>Mô tả sản phẩm (Tối đa 500 ký tự)</label>
              <textarea
                class="form-control"
                maxlength="500"
                [(ngModel)]="formData.description"
                name="description"
                placeholder="VD: Switch Gateron Brown, layout 87 phím..."
              ></textarea>
            </div>

            <div class="form-group">
              <label>Giá bán (VND) *</label>
              <input
                type="number"
                class="form-control"
                required
                min="0"
                step="1000"
                [(ngModel)]="formData.price"
                name="price"
                placeholder="VD: 1590000"
              />
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" (click)="closeModal()">Hủy</button>
              <button type="submit" class="btn btn-primary" [disabled]="isSubmitting()">
                {{ isSubmitting() ? 'Đang lưu...' : (editingProduct() ? 'Cập Nhật' : 'Tạo Mới') }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mb-4 {
      margin-bottom: 1.5rem;
    }

    .search-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1.5rem;
      padding: 1rem 1.25rem;
      flex-wrap: wrap;

      .search-input-wrapper {
        flex: 1;
        min-width: 280px;
      }
      .stats-text {
        color: var(--text-muted);
        font-size: 0.875rem;
      }
    }

    .loading-state {
      text-align: center;
      padding: 3rem 1rem;
      color: var(--text-muted);

      .spinner {
        width: 36px;
        height: 36px;
        border: 3px solid rgba(255, 255, 255, 0.1);
        border-top-color: var(--primary);
        border-radius: 50%;
        animation: spin 0.8s linear infinite;
        margin: 0 auto 1rem;
      }
    }

    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 1.5rem;
    }

    .product-card {
      display: flex;
      flex-direction: column;
      position: relative;

      .card-badge {
        position: absolute;
        top: 1rem;
        right: 1rem;
        font-size: 0.75rem;
        font-family: var(--font-mono);
        color: var(--text-dim);
      }

      .product-info {
        flex: 1;
        margin-bottom: 1.25rem;

        .product-name {
          font-size: 1.15rem;
          font-weight: 700;
          color: #ffffff;
          margin-bottom: 0.4rem;
          padding-right: 2rem;
        }

        .product-desc {
          font-size: 0.85rem;
          color: var(--text-muted);
          margin-bottom: 1rem;
          line-height: 1.5;
        }

        .product-price {
          display: flex;
          align-items: baseline;
          gap: 0.35rem;

          .price-val {
            font-size: 1.4rem;
            font-weight: 800;
            color: var(--accent-emerald);
            font-family: var(--font-mono);
          }
          .price-curr {
            font-size: 0.8rem;
            font-weight: 600;
            color: var(--text-dim);
          }
        }
      }

      .product-actions {
        display: flex;
        gap: 0.5rem;
        border-top: 1px solid var(--border-color);
        padding-top: 1rem;

        .buy-now {
          margin-left: auto;
        }
      }
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class ProductListComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly toastService = inject(ToastService);

  readonly products = signal<Product[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly isSubmitting = signal<boolean>(false);
  readonly showModal = signal<boolean>(false);
  readonly editingProduct = signal<Product | null>(null);

  searchQuery = '';

  formData: ProductRequest = {
    name: '',
    description: '',
    price: 0
  };

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.isLoading.set(true);
    this.productService.getProducts().subscribe({
      next: (list) => {
        this.products.set(list);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.toastService.error('Lỗi tải sản phẩm', err);
      }
    });
  }

  filteredProducts(): Product[] {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) return this.products();
    return this.products().filter(
      (p) =>
        p.name.toLowerCase().includes(q) ||
        (p.description && p.description.toLowerCase().includes(q))
    );
  }

  openCreateModal(): void {
    this.editingProduct.set(null);
    this.formData = { name: '', description: '', price: 0 };
    this.showModal.set(true);
  }

  openEditModal(p: Product): void {
    this.editingProduct.set(p);
    this.formData = {
      name: p.name,
      description: p.description,
      price: p.price
    };
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingProduct.set(null);
  }

  saveProduct(): void {
    if (!this.formData.name || this.formData.price < 0) {
      this.toastService.warning('Dữ liệu chưa hợp lệ', 'Vui lòng nhập tên và giá bán hợp lệ.');
      return;
    }

    this.isSubmitting.set(true);
    const editItem = this.editingProduct();

    if (editItem) {
      this.productService.updateProduct(editItem.id, this.formData).subscribe({
        next: (updated) => {
          this.isSubmitting.set(false);
          this.closeModal();
          this.toastService.success('Cập nhật thành công', `Đã cập nhật sản phẩm "${updated.name}"`);
          this.loadProducts();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.toastService.error('Lỗi cập nhật', err);
        }
      });
    } else {
      this.productService.createProduct(this.formData).subscribe({
        next: (created) => {
          this.isSubmitting.set(false);
          this.closeModal();
          this.toastService.success('Tạo sản phẩm mới', `Đã tạo sản phẩm "${created.name}"`);
          this.loadProducts();
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.toastService.error('Lỗi tạo sản phẩm', err);
        }
      });
    }
  }

  deleteProduct(p: Product): void {
    if (!confirm(`Bạn có chắc chắn muốn xóa sản phẩm "${p.name}"?`)) return;

    this.productService.deleteProduct(p.id).subscribe({
      next: () => {
        this.toastService.success('Đã xóa', `Đã xóa sản phẩm "${p.name}"`);
        this.loadProducts();
      },
      error: (err) => {
        this.toastService.error('Lỗi khi xóa', err);
      }
    });
  }
}
