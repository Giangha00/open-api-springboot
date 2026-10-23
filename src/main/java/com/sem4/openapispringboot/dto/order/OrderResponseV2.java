package com.sem4.openapispringboot.dto.order;

import com.sem4.openapispringboot.dto.payment.PaymentResponse;
import com.sem4.openapispringboot.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Thông tin chi tiết đơn hàng chuẩn phiên bản v2")
public record OrderResponseV2(
    @Schema(description = "Mã định danh đơn hàng duy nhất", example = "ORD-2026-000123")
    String code,

    @Schema(description = "ID khách hàng", example = "1")
    Long customerId,

    @Schema(description = "Trạng thái hiện tại của đơn hàng", example = "PENDING")
    OrderStatus status,

    @Schema(description = "Địa chỉ nhận hàng", example = "Số 8 Tôn Thất Thuyết, Cầu Giấy, Hà Nội")
    String shippingAddress,

    @Schema(description = "Tổng giá trị đơn hàng dạng cấu trúc {value, currency}")
    TotalAmountDto total,

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
