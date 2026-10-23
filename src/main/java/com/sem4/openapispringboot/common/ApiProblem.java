package com.sem4.openapispringboot.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Schema(description = "Mô hình lỗi chuẩn RFC 9457 mở rộng với mã lỗi nghiệp vụ và chi tiết trường")
public record ApiProblem(
    @Schema(description = "URI định danh kiểu lỗi", example = "https://api.example.com/errors/invalid-transition")
    URI type,

    @Schema(description = "Tiêu đề ngắn gọn về lỗi", example = "Invalid order state transition")
    String title,

    @Schema(description = "Mã trạng thái HTTP", example = "409")
    int status,

    @Schema(description = "Mô tả chi tiết nguyên nhân lỗi", example = "Cannot transition order from DELIVERED to CANCELLED")
    String detail,

    @Schema(description = "URI tài nguyên xảy ra lỗi", example = "/api/orders/ORD-2026-000123/transitions")
    String instance,

    @Schema(description = "Mã định danh lỗi nghiệp vụ duy nhất", example = "ORDER_INVALID_TRANSITION")
    String code,

    @Schema(description = "Thời điểm xảy ra lỗi", example = "2026-10-23T12:00:00Z")
    Instant timestamp,

    @Schema(description = "Danh sách chi tiết lỗi validate theo trường (nếu có)")
    List<FieldErrorItem> errors
) {
    @Schema(description = "Chi tiết lỗi của từng trường dữ liệu")
    public record FieldErrorItem(
        @Schema(description = "Tên trường vi phạm", example = "quantity")
        String field,

        @Schema(description = "Nội dung thông báo lỗi", example = "must be less than or equal to 20")
        String message
    ) {}
}
