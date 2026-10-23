package com.sem4.openapispringboot.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Chi tiết một dòng sản phẩm trong đơn hàng")
public record OrderItemResponse(
    @Schema(description = "ID dòng chi tiết", example = "101")
    Long id,

    @Schema(description = "ID sản phẩm", example = "1")
    Long productId,

    @Schema(description = "Tên sản phẩm chụp tại thời điểm đặt", example = "Bàn phím cơ K8 Pro")
    String productName,

    @Schema(description = "Đơn giá chụp tại thời điểm đặt", example = "1590000")
    BigDecimal unitPrice,

    @Schema(description = "Số lượng mua", example = "2")
    Integer quantity,

    @Schema(description = "Tổng tiền của dòng (unitPrice * quantity)", example = "3180000")
    BigDecimal lineTotal
) {
}
