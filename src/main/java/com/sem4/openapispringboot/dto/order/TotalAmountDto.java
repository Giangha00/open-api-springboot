package com.sem4.openapispringboot.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Tổng giá trị đơn hàng kèm đơn vị tiền tệ")
public record TotalAmountDto(
    @Schema(description = "Giá trị số tiền", example = "3180000")
    BigDecimal value,

    @Schema(description = "Mã tiền tệ", example = "VND")
    String currency
) {
}
