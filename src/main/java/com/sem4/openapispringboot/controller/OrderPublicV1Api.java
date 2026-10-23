package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.common.ApiProblem;
import com.sem4.openapispringboot.dto.order.OrderResponseV1;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Orders (v1)", description = "API Đơn hàng phiên bản 1 (Đã deprecated, thay thế bởi v2)")
@RequestMapping("/api/orders")
public interface OrderPublicV1Api {

    @Operation(
        summary = "Lấy chi tiết đơn hàng (v1 - Deprecated)",
        description = "Trả về thông tin đơn hàng theo đặc tả v1 với trường totalAmount dạng số. (Lưu ý: Endpoint này sẽ ngừng hoạt động từ ngày 2026-12-31, vui lòng chuyển sang /api/v2/orders/{code}).",
        deprecated = true,
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
        responseCode = "200",
        description = "Tìm thấy thông tin đơn hàng",
        headers = @Header(name = "ETag", description = "Phiên bản hiện tại của đơn hàng (dùng cho optimistic locking)")
    )
    @ApiResponse(
        responseCode = "404",
        description = "Không tìm thấy mã đơn hàng",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @GetMapping("/{code}")
    ResponseEntity<OrderResponseV1> getOrderByCodeV1(
        @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000123")
        @PathVariable String code
    );
}
