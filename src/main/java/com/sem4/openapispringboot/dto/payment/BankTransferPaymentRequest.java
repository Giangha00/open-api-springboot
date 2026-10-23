package com.sem4.openapispringboot.dto.payment;

import com.sem4.openapispringboot.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Thanh toán bằng chuyển khoản ngân hàng")
public record BankTransferPaymentRequest(
    @Schema(description = "Phương thức thanh toán", example = "BANK_TRANSFER", allowableValues = {"BANK_TRANSFER"})
    @NotNull
    PaymentMethod method,

    @Schema(description = "Mã ngân hàng thụ hưởng", example = "VCB")
    @NotBlank
    String bankCode,

    @Schema(description = "Mã tham chiếu giao dịch chuyển khoản", example = "TXN-2026-1023")
    @NotBlank
    String transferRef
) implements PaymentRequest {
    public BankTransferPaymentRequest(String bankCode, String transferRef) {
        this(PaymentMethod.BANK_TRANSFER, bankCode, transferRef);
    }

    @Override
    public PaymentMethod getMethod() {
        return method;
    }
}
