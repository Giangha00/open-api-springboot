import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-swagger-viewer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="container">
      <div class="page-header">
        <div class="header-left">
          <h1>Tài Liệu Swagger UI & OpenAPI 3.1</h1>
          <p>
            Đặc tả API tự động sinh từ mã nguồn Spring Boot 4 + springdoc-openapi 3.1
          </p>
        </div>
        <div class="header-actions">
          <a
            href="http://localhost:8080/swagger-ui.html"
            target="_blank"
            class="btn btn-primary"
          >
            🚀 Mở Swagger UI Toàn Màn Hình &nearr;
          </a>
        </div>
      </div>

      <!-- 4 Spec Groups Cards (D1) -->
      <div class="spec-groups-grid mb-4">
        <div class="glass-card group-card">
          <div class="group-header">
            <span class="group-num">01</span>
            <span class="badge badge-deprecated">DEPRECATED</span>
          </div>
          <h3 class="group-title">Public API (v1)</h3>
          <p class="group-desc">
            Bao gồm danh mục sản phẩm (/api/v1/products) và chi tiết đơn hàng trả về totalAmount dạng số. Sẽ ngừng hỗ trợ 2026-12-31.
          </p>
          <div class="group-links">
            <a href="http://localhost:8080/v3/api-docs/public-v1" target="_blank" class="spec-link">JSON Spec &rarr;</a>
          </div>
        </div>

        <div class="glass-card group-card featured">
          <div class="group-header">
            <span class="group-num">02</span>
            <span class="badge badge-DELIVERED">STANDARD</span>
          </div>
          <h3 class="group-title">Public API (v2)</h3>
          <p class="group-desc">
            Chuẩn mới nhất: Tạo đơn hàng với Idempotency, thanh toán đa hình oneOf, chi tiết đơn với total {{ '{value, currency}' }}, ETag.
          </p>
          <div class="group-links">
            <a href="http://localhost:8080/v3/api-docs/public-v2" target="_blank" class="spec-link">JSON Spec &rarr;</a>
          </div>
        </div>

        <div class="glass-card group-card">
          <div class="group-header">
            <span class="group-num">03</span>
            <span class="badge badge-CONFIRMED">ADMIN</span>
          </div>
          <h3 class="group-title">Admin API</h3>
          <p class="group-desc">
            Duyệt đơn, xuất kho, hủy đơn với lý do bắt buộc, upload hóa đơn PDF dung lượng tối đa 5 MB, báo cáo doanh thu theo ngày.
          </p>
          <div class="group-links">
            <a href="http://localhost:8080/v3/api-docs/admin" target="_blank" class="spec-link">JSON Spec &rarr;</a>
          </div>
        </div>

        <div class="glass-card group-card">
          <div class="group-header">
            <span class="group-num">04</span>
            <span class="badge badge-SHIPPING">PARTNER</span>
          </div>
          <h3 class="group-title">Partner Webhooks</h3>
          <p class="group-desc">
            Dành cho đối tác logistics cập nhật SHIP, DELIVER, FAIL_DELIVERY với xác thực header X-Partner-Key.
          </p>
          <div class="group-links">
            <a href="http://localhost:8080/v3/api-docs/partner" target="_blank" class="spec-link">JSON Spec &rarr;</a>
          </div>
        </div>
      </div>

      <!-- Live Embed Frame -->
      <div class="glass-card embed-card">
        <div class="embed-header">
          <div class="embed-dots">
            <span class="dot red"></span>
            <span class="dot yellow"></span>
            <span class="dot green"></span>
          </div>
          <span class="embed-url font-mono">http://localhost:8080/swagger-ui.html</span>
          <a href="http://localhost:8080/swagger-ui.html" target="_blank" class="btn btn-secondary btn-sm">
            Mở tab mới
          </a>
        </div>
        <div class="iframe-container">
          <iframe
            src="http://localhost:8080/swagger-ui.html"
            class="swagger-iframe"
            title="Swagger UI"
          ></iframe>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .mb-4 { margin-bottom: 1.5rem; }
    .font-mono { font-family: var(--font-mono); }

    .spec-groups-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 1.25rem;
    }

    .group-card {
      display: flex;
      flex-direction: column;

      &.featured {
        border-color: rgba(99, 102, 241, 0.4);
        box-shadow: 0 0 20px rgba(99, 102, 241, 0.2);
      }

      .group-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 0.75rem;

        .group-num {
          font-family: var(--font-mono);
          font-size: 1.15rem;
          font-weight: 800;
          color: var(--text-dim);
        }
      }

      .group-title {
        font-size: 1.15rem;
        font-weight: 700;
        color: #ffffff;
        margin-bottom: 0.4rem;
      }

      .group-desc {
        font-size: 0.825rem;
        color: var(--text-muted);
        line-height: 1.5;
        flex: 1;
        margin-bottom: 1rem;
      }

      .group-links {
        border-top: 1px solid var(--border-color);
        padding-top: 0.75rem;

        .spec-link {
          font-size: 0.825rem;
          font-weight: 700;
          color: #60a5fa;
          &:hover { color: #93c5fd; }
        }
      }
    }

    .embed-card {
      padding: 0;
      overflow: hidden;
      border-radius: var(--radius-md);

      .embed-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.75rem 1rem;
        background: rgba(0, 0, 0, 0.4);
        border-bottom: 1px solid var(--border-color);

        .embed-dots {
          display: flex;
          gap: 0.4rem;
          .dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            &.red { background: #ef4444; }
            &.yellow { background: #f59e0b; }
            &.green { background: #10b981; }
          }
        }

        .embed-url {
          font-size: 0.8rem;
          color: var(--text-dim);
        }
      }

      .iframe-container {
        height: 750px;
        width: 100%;
        background: #ffffff;

        .swagger-iframe {
          width: 100%;
          height: 100%;
          border: none;
        }
      }
    }
  `]
})
export class SwaggerViewerComponent {}
