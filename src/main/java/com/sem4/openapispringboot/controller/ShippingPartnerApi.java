package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.common.ApiProblem;
import com.sem4.openapispringboot.dto.order.OrderTransitionResponse;
import com.sem4.openapispringboot.dto.order.ShippingWebhookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Partner (Shipping)", description = "Webhook tiếp nhận cập nhật vận chuyển từ Đơn vị giao vận")
@RequestMapping("/api/webhooks")
public interface ShippingPartnerApi {

    @Operation(
        summary = "Webhook cập nhật trạng thái vận chuyển từ đối tác",
        description = "Đối tác giao vận gọi webhook để cập nhật: SHIP (bắt đầu giao hàng), DELIVER (đã giao thành công), hoặc FAIL_DELIVERY (giao thất bại, chờ giao lại). Bắt buộc xác thực bằng header X-Partner-Key.",
        security = @SecurityRequirement(name = "partnerKey")
    )
    @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái vận chuyển thành công")
    @ApiResponse(
        responseCode = "403",
        description = "X-Partner-Key không hợp lệ hoặc thiếu",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "Trạng thái đơn hàng không hợp lệ cho cập nhật này",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @PostMapping("/shipping")
    ResponseEntity<OrderTransitionResponse> handleShippingWebhook(
        @Parameter(description = "Mã API Key của đối tác giao vận", required = true, example = "partner-secret-key-2026")
        @RequestHeader("X-Partner-Key") String partnerKey,

        @Valid @RequestBody ShippingWebhookRequest request
    );
}
