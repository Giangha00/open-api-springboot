package com.sem4.openapispringboot.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Thông tin sản phẩm trả về")
public record ProductResponse(
    @Schema(description = "Mã định danh sản phẩm", example = "1")
    Long id,

    @Schema(description = "Tên sản phẩm", example = "Bàn phím cơ K8 Pro")
    String name,

    @Schema(description = "Mô tả sản phẩm", example = "Switch Gateron Brown, layout 87 phím")
    String description,

    @Schema(description = "Giá bán hiện tại", example = "1590000")
    BigDecimal price,

    @Schema(description = "Thời điểm tạo")
    Instant createdAt,

    @Schema(description = "Thời điểm cập nhật mới nhất")
    Instant updatedAt
) {
}
