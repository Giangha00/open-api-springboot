package com.sem4.openapispringboot.dto.order;

import com.sem4.openapispringboot.entity.OrderAction;
import com.sem4.openapispringboot.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kết quả chuyển trạng thái đơn hàng thành công")
public record OrderTransitionResponse(
    @Schema(description = "Mã đơn hàng", example = "ORD-2026-000123")
    String code,

    @Schema(description = "Trạng thái trước khi chuyển", example = "PENDING")
    OrderStatus fromStatus,

    @Schema(description = "Trạng thái sau khi chuyển thành công", example = "CONFIRMED")
    OrderStatus toStatus,

    @Schema(description = "Hành động đã thực hiện", example = "CONFIRM")
    OrderAction action,

    @Schema(description = "Phiên bản mới của đơn hàng (ETag)", example = "1")
    Long version,

    @Schema(description = "Thông báo kết quả", example = "Đơn hàng ORD-2026-000123 đã được chuyển sang trạng thái CONFIRMED thành công.")
    String message
) {
}
