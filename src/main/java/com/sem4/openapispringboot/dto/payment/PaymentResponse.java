package com.sem4.openapispringboot.dto.payment;

import com.sem4.openapispringboot.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Thông tin thanh toán của đơn hàng")
public record PaymentResponse(
    @Schema(description = "Phương thức thanh toán", example = "CARD")
    PaymentMethod method,

    @Schema(description = "Số tiền thanh toán", example = "1590000")
    BigDecimal amount,

    @Schema(description = "Tổ chức thẻ (nếu là CARD)", example = "VISA")
    String provider,

    @Schema(description = "4 số cuối thẻ (nếu là CARD)", example = "4242")
    String last4,

    @Schema(description = "Mã ngân hàng (nếu là BANK_TRANSFER)", example = "VCB")
    String bankCode,

    @Schema(description = "Mã giao dịch (nếu là BANK_TRANSFER)", example = "TXN-2026-1023")
    String transferRef,

    @Schema(description = "Thời gian xác nhận thanh toán")
    Instant paidAt
) {
}
