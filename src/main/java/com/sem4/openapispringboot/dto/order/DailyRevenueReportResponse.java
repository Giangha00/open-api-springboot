package com.sem4.openapispringboot.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Báo cáo doanh thu theo ngày")
public record DailyRevenueReportResponse(
    @Schema(description = "Ngày thống kê (YYYY-MM-DD)", example = "2026-10-23")
    LocalDate date,

    @Schema(description = "Tổng số đơn hàng hoàn thành / hợp lệ trong ngày", example = "15")
    Long totalOrders,

    @Schema(description = "Tổng doanh thu ghi nhận trong ngày", example = "45800000.00")
    BigDecimal totalRevenue,

    @Schema(description = "Đơn vị tiền tệ", example = "VND")
    String currency
) {
}
