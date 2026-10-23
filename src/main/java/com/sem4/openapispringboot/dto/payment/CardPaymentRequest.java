package com.sem4.openapispringboot.dto.payment;

import com.sem4.openapispringboot.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Thanh toán qua thẻ quốc tế (VISA / MasterCard)")
public record CardPaymentRequest(
    @Schema(description = "Phương thức thanh toán", example = "CARD", allowableValues = {"CARD"})
    @NotNull
    PaymentMethod method,

    @Schema(description = "Tổ chức phát hành thẻ", example = "VISA")
    @NotBlank
    String provider,

    @Schema(description = "4 số cuối của thẻ", example = "4242")
    @NotBlank
    @Pattern(regexp = "^\\d{4}$", message = "last4 must be 4 digits")
    String last4
) implements PaymentRequest {
    public CardPaymentRequest(String provider, String last4) {
        this(PaymentMethod.CARD, provider, last4);
    }

    @Override
    public PaymentMethod getMethod() {
        return method;
    }
}
