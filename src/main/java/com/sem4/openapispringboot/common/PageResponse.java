package com.sem4.openapispringboot.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Cấu trúc phản hồi phân trang chuẩn ổn định")
public record PageResponse<T>(
    @Schema(description = "Danh sách dữ liệu trong trang hiện tại")
    List<T> content,

    @Schema(description = "Số thứ tự trang (0-indexed)", example = "0")
    int pageNumber,

    @Schema(description = "Kích thước mỗi trang", example = "10")
    int pageSize,

    @Schema(description = "Tổng số phần tử thỏa mãn điều kiện", example = "42")
    long totalElements,

    @Schema(description = "Tổng số trang", example = "5")
    int totalPages,

    @Schema(description = "Đã là trang cuối cùng hay chưa", example = "false")
    boolean isLast
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}
