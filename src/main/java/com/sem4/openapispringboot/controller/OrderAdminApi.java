package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.common.ApiProblem;
import com.sem4.openapispringboot.dto.order.DailyRevenueReportResponse;
import com.sem4.openapispringboot.dto.order.OrderTransitionRequest;
import com.sem4.openapispringboot.dto.order.OrderTransitionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "Admin", description = "API Quản trị dành cho Quản lý / ADMIN TechShop")
@RequestMapping("/api")
public interface OrderAdminApi {

    @Operation(
        summary = "Quản trị viên chuyển trạng thái đơn hàng",
        description = "ADMIN có quyền thực hiện mọi hành động (CONFIRM, SHIP, CANCEL với reason khi CONFIRMED, ACCEPT_RETURN). Bắt buộc If-Match.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
        responseCode = "200",
        description = "Chuyển trạng thái đơn hàng thành công",
        headers = @Header(name = "ETag", description = "Phiên bản mới của đơn hàng")
    )
    @ApiResponse(
        responseCode = "409",
        description = "Chuyển trạng thái không hợp lệ (ORDER_INVALID_TRANSITION)",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @ApiResponse(
        responseCode = "412",
        description = "If-Match lệch version (PRECONDITION_FAILED)",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @PostMapping("/admin/orders/{code}/transitions")
    ResponseEntity<OrderTransitionResponse> adminTransitionOrder(
        @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000123")
        @PathVariable String code,

        @Parameter(description = "Phiên bản hiện tại của đơn hàng (bắt buộc)", required = true, example = "\"0\"")
        @RequestHeader("If-Match") String ifMatch,

        @Valid @RequestBody OrderTransitionRequest request
    );

    @Operation(
        summary = "Tải lên hóa đơn PDF cho đơn hàng",
        description = "ADMIN tải lên hóa đơn điện tử cho đơn hàng. Định dạng bắt buộc PDF, kích thước tối đa 5 MB.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Đã tải lên hóa đơn thành công")
    @ApiResponse(
        responseCode = "400",
        description = "File không hợp lệ hoặc không phải PDF",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @PostMapping(value = "/orders/{code}/invoice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<Map<String, String>> uploadInvoice(
        @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000123")
        @PathVariable String code,

        @Parameter(description = "File hóa đơn điện tử PDF (tối đa 5 MB)")
        @RequestPart("file") MultipartFile file
    );

    @Operation(
        summary = "Báo cáo doanh thu theo ngày",
        description = "Thống kê tổng số đơn hàng hợp lệ và tổng doanh thu theo từng ngày trong khoảng thời gian chỉ định.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Dữ liệu báo cáo doanh thu theo ngày")
    @GetMapping("/admin/reports/daily-revenue")
    List<DailyRevenueReportResponse> getDailyRevenueReport(
        @Parameter(description = "Ngày bắt đầu (YYYY-MM-DD)", example = "2026-10-01")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

        @Parameter(description = "Ngày kết thúc (YYYY-MM-DD)", example = "2026-10-31")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    );
}
