package com.sem4.openapispringboot.dto.order;

import com.sem4.openapispringboot.entity.OrderAction;
import com.sem4.openapispringboot.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Sự kiện trong nhật ký chuyển trạng thái của đơn hàng")
public record OrderEventResponse(
    @Schema(description = "ID bản ghi sự kiện", example = "1")
    Long id,

    @Schema(description = "Mã đơn hàng", example = "ORD-2026-000123")
    String orderCode,

    @Schema(description = "Trạng thái trước", example = "PENDING")
    OrderStatus fromStatus,

    @Schema(description = "Trạng thái sau", example = "CONFIRMED")
    OrderStatus toStatus,

    @Schema(description = "Hành động thực hiện", example = "CONFIRM")
    OrderAction action,

    @Schema(description = "Chủ thể thực hiện (CUSTOMER / ADMIN / PARTNER)", example = "ADMIN")
    String actor,

    @Schema(description = "Lý do ghi nhận", example = "Xác nhận kho sẵn sàng giao hàng")
    String reason,

    @Schema(description = "Thời điểm ghi nhận sự kiện")
    Instant at
) {
}
