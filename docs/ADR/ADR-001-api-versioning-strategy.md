# ADR-001: Chiến Lược Versioning REST API (URI Path vs HTTP Header)

- **Trạng thái:** Được chấp thuận (Accepted)
- **Ngày quyết định:** 2026-10-23
- **Tác giả:** Đội ngũ Kỹ sư Phần mềm TechShop (com.sem4)
- **Phạm vi:** Toàn bộ hệ sinh thái REST API TechShop (Product Catalog, Order Service)

---

## 1. Ngữ cảnh (Context)

Hệ thống TechShop cung cấp REST API cho 3 nhóm đối tượng: Khách hàng (Web/Mobile), Đối tác vận chuyển (Logistics), và Nhân sự quản trị nội bộ (Admin).

Khi nâng cấp mô hình dữ liệu đơn hàng từ v1 (trả về `totalAmount` kiểu số `BigDecimal`) sang v2 (chuẩn hóa đa tiền tệ với cấu trúc `{ value, currency }`), hệ thống cần một chiến lược đánh phiên bản (API Versioning) rõ ràng nhằm:
1. Đảm bảo tính tương thích ngược (Backward Compatibility), không làm gãy các ứng dụng client cũ đang chạy trên thị trường.
2. Dễ dàng tài liệu hóa thành các nhóm đặc tả OpenAPI 3.1 riêng biệt (`public-v1`, `public-v2`).
3. Trực quan cho các lập trình viên Frontend, đối tác bên thứ ba và hệ thống caching/proxy (CDN, API Gateway).

Hai phương án chính được cân nhắc:
- **Phương án A: URI Path Versioning** (ví dụ: `/api/v1/orders/{code}` và `/api/v2/orders/{code}`).
- **Phương án B: Header Versioning** (Accept Header / Content Negotiation: `Accept: application/vnd.techshop.v2+json` hoặc Custom Header: `X-API-Version: 2`).

---

## 2. Quyết định (Decision)

Chúng tôi quyết định áp dụng **URI Path Versioning kết hợp Resource URI chung**:

1. **Path-based Versioning:**
   - Phiên bản v1: Đánh dấu `@Deprecated`, tài liệu hóa rõ ràng ngày ngừng hỗ trợ (End of Life: 2026-12-31).
   - Phiên bản v2: Đường dẫn chuẩn hóa `/api/v2/orders/{code}` và các API mới theo namespace v2.
2. **Resource URI gốc:**
   - Các thao tác chuẩn (`POST /api/orders`, `GET /api/orders`) mặc định phục vụ đặc tả mới nhất (v2).

---

## 3. Lý do lựa chọn (Rationale)

| Tiêu chí | URI Path Versioning (Được chọn) | Header / Content Negotiation |
| :--- | :--- | :--- |
| **Khả năng sinh tài liệu OpenAPI / Swagger** | **Xuất sắc:** SpringDoc & Swagger UI quét và phân tách group OpenAPI (`public-v1`, `public-v2`) tự động và trực quan 100%. | **Hạn chế:** Nhiều tool và Swagger UI gặp khó khăn khi hiển thị nhiều schema khác nhau trên cùng một Path. |
| **Sinh Code Client tự động (SDK Generation)** | **Dễ dàng:** OpenAPI Generator tạo ra các client TypeScript, Java SDK rõ ràng mà không cần cấu hình header phức tạp. | Dễ gây nhầm lẫn hoặc lỗi khi sinh mã tự động cho FE/Mobile. |
| **Khả năng Caching & Proxy (CDN / Gateway)** | **Rất cao:** Cache key dựa trên URI (URL). Không cần cấu hình phức tạp `Vary: Accept` hay `Vary: X-API-Version`. | Dễ bị lỗi cache stale nếu Gateway hoặc CDN không cấu hình đúng `Vary`. |
| **Trải nghiệm kiểm thử & Debug** | **Thuận tiện:** Có thể dán URL lên trình duyệt, gửi link qua chat, kiểm tra bằng cURL không cần nhớ cú pháp header tùy biến. | Bắt buộc phải thêm header tùy biến ở mọi request; khó test nhanh. |

---

## 4. Hệ quả & Giải pháp (Consequences & Mitigations)

- **Hệ quả:** Có thể làm gia tăng số lượng endpoint trong Controller nếu duy trì nhiều phiên bản song song.
- **Biện pháp giảm thiểu:**
  - Áp dụng nguyên tắc chỉ hỗ trợ tối đa 2 phiên bản (phiên bản hiện tại và phiên bản liền kề có gắn nhãn Deprecated).
  - Tách contract interface `OrderPublicV1Api` và `OrderPublicV2Api` độc lập, delegate chung về `OrderService` giúp tái sử dụng mã nguồn và tuân thủ nguyên tắc DRY (Don't Repeat Yourself).
