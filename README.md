# TechShop Order & Catalog Service (Spring Boot 4 + OpenAPI 3.1 + Angular)

Dự án hoàn chỉnh triển khai theo yêu cầu đặc tả đề bài:
- **Backend:** Spring Boot 4.1.2, Java 17, Spring Data JPA, H2/MySQL, MapStruct 1.6.3, JJWT 0.12.6, Bean Validation.
- **Tài liệu hóa API:** springdoc-openapi 3.1.0 theo chuẩn **OpenAPI 3.1**, phân tách 4 nhóm spec (`public-v1`, `public-v2`, `admin`, `partner`), xử lý lỗi RFC 9457 `ProblemDetail`.
- **Frontend View:** Ứng dụng **Angular Standalone Components** với kiến trúc phân trang độc lập (`/products`, `/orders/create`, `/orders`, `/orders/:code`, `/admin/orders`, `/admin/reports`, `/partner/shipping`, `/swagger`).

---

## 1. Khởi Động Nhanh Bằng Một Lệnh

Chỉ cần chạy lệnh sau từ thư mục gốc của dự án:

```bash
./mvnw spring-boot:run
```

Sau khi ứng dụng khởi động thành công:
- **Giao diện Web Angular (Tất cả các Page):** [http://localhost:8080](http://localhost:8080)
- **Tài liệu tương tác Swagger UI (OpenAPI 3.1):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Trang quản trị cơ sở dữ liệu H2 Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:catalogdb`, Username: `sa`, Password: để trống)

*(Nếu muốn chạy frontend độc lập ở chế độ Live-Reload trong khi phát triển: vào thư mục `frontend/` và gõ `npm start` để mở trên `http://localhost:4200`)*.

---

## 2. Kiến Trúc Phân Trang Giao Diện Angular

Giao diện được thiết kế độc lập theo từng Page với hệ thống Angular Router chuyên biệt:

| Đường dẫn (Route) | Tên Page / Chức năng | Mô tả chi tiết |
| :--- | :--- | :--- |
| `/products` | **Danh mục sản phẩm** | Danh sách sản phẩm dạng lưới & thẻ interactive, thêm sản phẩm mới, sửa, xóa, tìm kiếm thời gian thực. |
| `/orders/create` | **Đặt hàng mới** | Chọn sản phẩm, số lượng (1-20), chọn phương thức thanh toán đa hình (`COD`, `CARD`, `BANK_TRANSFER`), cấu hình và kiểm thử khóa `Idempotency-Key`. |
| `/orders` | **Lịch sử đơn hàng** | Danh sách đơn hàng cá nhân, phân trang ổn định `PageResponse`, bộ lọc trạng thái, khoảng ngày, giá trị tối thiểu, sắp xếp. |
| `/orders/:code` | **Chi tiết đơn hàng** | Chi tiết đơn, toggle so sánh đặc tả v1 (Deprecated) vs v2 (Standard), ETag/version, timeline sự kiện `OrderEvent`, nút Hủy đơn (PENDING), Yêu cầu trả hàng (DELIVERED &le; 7 ngày), tải hóa đơn PDF. |
| `/admin/orders` | **Quản lý đơn Admin** | Quản trị viên duyệt đơn (`CONFIRM`), xuất kho (`SHIP`), hủy đơn có bắt buộc lý do (`CANCEL`), chấp nhận trả hàng (`ACCEPT_RETURN`), tải lên hóa đơn PDF (&le; 5 MB). |
| `/admin/reports` | **Báo cáo doanh thu** | Báo cáo doanh thu theo ngày, bộ lọc khoảng ngày, thẻ chỉ số KPI (Tổng doanh thu, Tổng đơn, AOV), biểu đồ cột trực quan. |
| `/partner/shipping` | **Mô phỏng Giao vận** | Mô phỏng đối tác logistics gọi Webhook `POST /api/webhooks/shipping` với header `X-Partner-Key` để cập nhật `SHIP`, `DELIVER`, `FAIL_DELIVERY`. |
| `/swagger` | **Swagger UI Explorer** | Tổng hợp 4 nhóm spec OpenAPI 3.1, liên kết tải file JSON/YAML và khung nhúng tương tác trực tiếp. |

---

## 3. Hệ Thống 4 Nhóm Spec OpenAPI 3.1 (Tiêu chí D1)

Swagger UI được phân chia thành 4 nhóm dropdown chuyên biệt:

1. **`public-v1` (Public API v1 - Deprecated):**
   - Endpoints: `GET /api/v1/products`, `GET /api/orders/{code}` (v1: `totalAmount` dạng số).
   - Đánh dấu `deprecated: true` với ngày ngừng hỗ trợ `2026-12-31`.
2. **`public-v2` (Public API v2 - Standard):**
   - Endpoints: `POST /api/orders`, `GET /api/orders`, `GET /api/v2/orders/{code}`, `POST /api/orders/{code}/transitions`, `GET /api/orders/{code}/invoice`.
   - Chuẩn hóa cấu trúc tiền tệ `total: { value, currency }`, header `Idempotency-Key`, `If-Match`.
3. **`admin` (Admin API):**
   - Endpoints: `POST /api/admin/orders/{code}/transitions`, `POST /api/orders/{code}/invoice`, `GET /api/admin/reports/daily-revenue`.
4. **`partner` (Partner Webhooks):**
   - Endpoints: `POST /api/webhooks/shipping` với xác thực `X-Partner-Key`.

---

## 4. Máy Trạng Thái Đơn Hàng (State Machine)

Hệ thống quản lý vòng đời đơn hàng qua **7 trạng thái** và **8 hành động**:

```
[ PENDING ] ──(CONFIRM - ADMIN)──> [ CONFIRMED ] ──(SHIP - ADMIN/PARTNER)──> [ SHIPPING ]
     │                                    │                                      │
 (CANCEL)                             (CANCEL + reason)                     (FAIL_DELIVERY)
     │                                    │                                      │
     ▼                                    ▼                                      ▼
[ CANCELLED ]                        [ CANCELLED ]                         [ CONFIRMED ]

                                      [ DELIVERED ] <──(DELIVER - PARTNER)───────┘
                                           │
                                    (REQUEST_RETURN - CUSTOMER <= 7 ngày)
                                           │
                                           ▼
                                  [ RETURN_REQUESTED ] ──(ACCEPT_RETURN - ADMIN)──> [ RETURNED ]
```

---

## 5. Thư Mục Tài Liệu OpenAPI Artifacts & ADR

- **4 File Spec YAML (Tiêu chí D9):**
  - `docs/api/openapi-public-v1.yaml`
  - `docs/api/openapi-public-v2.yaml`
  - `docs/api/openapi-admin.yaml`
  - `docs/api/openapi-partner.yaml`
- **Tài liệu Quyết định Kiến trúc (ADR):**
  - [ADR-001: Chiến lược Versioning REST API (URI Path vs Header)](file:///Users/macbook/T2502E_SEM4/open-api-springboot/docs/ADR/ADR-001-api-versioning-strategy.md)

---

## 6. Chạy Kiểm Thử Tự Động (Automated Tests)

Chạy toàn bộ bộ kiểm thử hợp đồng spec, máy trạng thái, và idempotency bằng lệnh:

```bash
./mvnw test
```

Bộ test bao gồm:
1. `OpenApiSpecTest`: Kiểm tra cấu trúc OpenAPI 3.1 cho cả 4 nhóm và tự động xuất file spec YAML vào `docs/api/`.
2. `StateMachineTest`: Kiểm tra toàn bộ luồng chuyển trạng thái hợp lệ và không hợp lệ, phân quyền người dùng và quy tắc 7 ngày đổi trả.
3. `IdempotencyTest`: Kiểm tra cơ chế chống trùng lặp tạo đơn hàng trong 24 giờ với cùng key và xung đột khi khác payload (`409 IDEMPOTENCY_CONFLICT`).
