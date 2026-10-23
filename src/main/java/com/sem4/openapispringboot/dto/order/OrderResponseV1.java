package com.sem4.openapispringboot.dto.order;

import com.sem4.openapispringboot.dto.payment.PaymentResponse;
import com.sem4.openapispringboot.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Deprecated
@Schema(
    description = "Thông tin chi tiết đơn hàng theo đặc tả v1 (Deprecated: Sẽ ngừng hỗ trợ từ 2026-12-31, vui lòng chuyển sang v2)",
    deprecated = true
)
public record OrderResponseV1(
    @Schema(description = "Mã định danh đơn hàng duy nhất", example = "ORD-2026-000123")
    String code,

    @Schema(description = "ID khách hàng", example = "1")
    Long customerId,

    @Schema(description = "Trạng thái hiện tại của đơn hàng", example = "PENDING")
    OrderStatus status,

    @Schema(description = "Địa chỉ nhận hàng", example = "Số 8 Tôn Thất Thuyết, Cầu Giấy, Hà Nội")
    String shippingAddress,

    @Schema(description = "Tổng tiền dạng số đơn giản (Deprecated ở v2)", example = "3180000")
    BigDecimal totalAmount,

    @Schema(description = "Đơn vị tiền tệ", example = "VND")
    String currency,

    @Schema(description = "Phiên bản dữ liệu (sử dụng cho ETag / If-Match Optimistic Locking)", example = "0")
    Long version,

    @Schema(description = "Thời điểm tạo đơn")
    Instant createdAt,

    @Schema(description = "Thời điểm đơn hàng được giao thành công")
    Instant deliveredAt,

    @Schema(description = "Danh sách sản phẩm trong đơn")
    List<OrderItemResponse> items,

    @Schema(description = "Thông tin thanh toán")
    PaymentResponse payment,

    @Schema(description = "Đơn hàng đã được xuất hóa đơn PDF chưa", example = "false")
    boolean hasInvoice
) {
}
