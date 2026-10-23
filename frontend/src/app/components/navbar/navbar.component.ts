import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <header class="app-header">
      <div class="nav-inner">
        <a routerLink="/" class="brand-link">
          <div class="brand-logo">
            <span>TS</span>
          </div>
          <div class="brand-text">
            <span class="brand-name">TechShop</span>
            <span class="brand-tag">OpenAPI 3.1</span>
          </div>
        </a>

        <nav class="nav-links">
          <a routerLink="/products" routerLinkActive="active" class="nav-item">Sản phẩm</a>
          <a routerLink="/orders/create" routerLinkActive="active" class="nav-item">Đặt hàng</a>
          <a routerLink="/orders" routerLinkActive="active" [routerLinkActiveOptions]="{exact: true}" class="nav-item">Đơn của tôi</a>
          <a routerLink="/admin/orders" routerLinkActive="active" class="nav-item">QL Đơn hàng</a>
          <a routerLink="/admin/reports" routerLinkActive="active" class="nav-item">Báo cáo</a>
          <a routerLink="/partner/shipping" routerLinkActive="active" class="nav-item">Webhook Đối tác</a>
          <a routerLink="/swagger" routerLinkActive="active" class="nav-item swagger-nav">Swagger UI</a>
        </nav>

        <div class="role-selector-container">
          <span class="role-label">Vai trò:</span>
          <select
            class="role-select"
            [value]="getCurrentProfileKey()"
            (change)="onRoleChange($event)"
          >
            <option value="customer1">Khách: Tuấn (id=1)</option>
            <option value="customer2">Khách: Mai (id=2)</option>
            <option value="admin">Quản trị viên (Admin)</option>
            <option value="partner">Đối tác Vận chuyển</option>
          </select>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .app-header {
      background: rgba(10, 14, 23, 0.85);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border-bottom: 1px solid var(--border-color);
      position: sticky;
      top: 0;
      z-index: 500;
    }

    .nav-inner {
      max-width: 1380px;
      margin: 0 auto;
      padding: 0.75rem 1.5rem;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1.5rem;
      flex-wrap: wrap;
    }

    .brand-link {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      text-decoration: none;
    }

    .brand-logo {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: linear-gradient(135deg, #6366f1 0%, #06b6d4 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #ffffff;
      font-weight: 800;
      font-size: 0.95rem;
      box-shadow: 0 4px 12px rgba(99, 102, 241, 0.35);
    }

    .brand-text {
      display: flex;
      flex-direction: column;
      line-height: 1.1;

      .brand-name {
        font-weight: 800;
        font-size: 1.1rem;
        color: #ffffff;
        letter-spacing: -0.02em;
      }

      .brand-tag {
        font-size: 0.65rem;
        font-weight: 700;
        text-transform: uppercase;
        color: var(--accent-cyan);
        letter-spacing: 0.05em;
      }
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      flex-wrap: wrap;
    }

    .nav-item {
      padding: 0.45rem 0.8rem;
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--text-muted);
      border-radius: var(--radius-sm);
      transition: all 0.15s ease;

      &:hover {
        color: #ffffff;
        background: rgba(255, 255, 255, 0.05);
      }

      &.active {
        color: #ffffff;
        background: rgba(99, 102, 241, 0.2);
        border: 1px solid rgba(99, 102, 241, 0.35);
      }

      &.swagger-nav {
        color: #34d399;
        &.active {
          background: rgba(16, 185, 129, 0.2);
          border-color: rgba(16, 185, 129, 0.4);
        }
      }
    }

    .role-selector-container {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: rgba(255, 255, 255, 0.04);
      padding: 0.3rem 0.6rem;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-color);

      .role-label {
        font-size: 0.75rem;
        color: var(--text-dim);
        font-weight: 600;
      }

      .role-select {
        background: transparent;
        color: #f1f5f9;
        font-family: var(--font-main);
        font-size: 0.825rem;
        font-weight: 600;
        border: none;
        outline: none;
        cursor: pointer;

        option {
          background: #111726;
          color: #f1f5f9;
        }
      }
    }
  `]
})
export class NavbarComponent {
  readonly authService = inject(AuthService);

  getCurrentProfileKey(): string {
    const profile = this.authService.activeProfile();
    if (profile.role === 'ADMIN') return 'admin';
    if (profile.role === 'PARTNER') return 'partner';
    return profile.customerId === 2 ? 'customer2' : 'customer1';
  }

  onRoleChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.authService.selectProfile(target.value as any);
  }
}
