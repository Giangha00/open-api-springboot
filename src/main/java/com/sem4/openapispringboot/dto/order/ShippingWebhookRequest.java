package com.sem4.openapispringboot.dto.order;

import com.sem4.openapispringboot.entity.OrderAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Webhook yêu cầu cập nhật giao hàng từ đối tác vận chuyển")
public record ShippingWebhookRequest(
    @Schema(description = "Mã đơn hàng cần cập nhật", example = "ORD-2026-000123")
    @NotBlank
    String code,

    @Schema(
        description = "Hành động vận chuyển từ đối tác (SHIP, DELIVER, hoặc FAIL_DELIVERY)",
        example = "DELIVER",
        allowableValues = {"SHIP", "DELIVER", "FAIL_DELIVERY"}
    )
    @NotNull
    OrderAction action,

    @Schema(description = "Ghi chú giao hàng hoặc lý do giao thất bại (nếu có)", example = "Đã giao thành công cho khách hàng tại nhà")
    String notes
) {
}
