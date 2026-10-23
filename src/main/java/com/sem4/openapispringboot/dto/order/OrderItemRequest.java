package com.sem4.openapispringboot.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Thông tin dòng sản phẩm đặt hàng")
public record OrderItemRequest(
    @Schema(description = "ID sản phẩm cần mua", example = "1")
    @NotNull
    Long productId,

    @Schema(description = "Số lượng mua (từ 1 đến 20)", example = "2", minimum = "1", maximum = "20")
    @NotNull
    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 20, message = "quantity must be at most 20")
    Integer quantity
) {
}
