package com.sem4.openapispringboot.dto.order;

import com.sem4.openapispringboot.entity.OrderAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu thực hiện chuyển trạng thái đơn hàng")
public record OrderTransitionRequest(
    @Schema(
        description = "Hành động chuyển trạng thái được phép:\n" +
            "- CONFIRM: ADMIN (PENDING -> CONFIRMED)\n" +
            "- SHIP: ADMIN / PARTNER (CONFIRMED -> SHIPPING)\n" +
            "- DELIVER: PARTNER (SHIPPING -> DELIVERED)\n" +
            "- CANCEL: CUSTOMER (khi PENDING) | ADMIN (khi PENDING hoặc CONFIRMED kèm reason)\n" +
            "- FAIL_DELIVERY: PARTNER (SHIPPING -> CONFIRMED)\n" +
            "- REQUEST_RETURN: CUSTOMER (DELIVERED -> RETURN_REQUESTED, trong vòng 7 ngày)\n" +
            "- ACCEPT_RETURN: ADMIN (RETURN_REQUESTED -> RETURNED)",
        example = "CANCEL"
    )
    @NotNull
    OrderAction action,

    @Schema(
        description = "Lý do chuyển trạng thái (bắt buộc khi ADMIN hủy đơn ở trạng thái CONFIRMED hoặc khi trả hàng)",
        example = "Khách hàng đổi ý muốn đổi sang mẫu mã khác"
    )
    String reason
) {
}
