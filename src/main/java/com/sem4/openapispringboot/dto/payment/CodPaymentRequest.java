package com.sem4.openapispringboot.dto.payment;

import com.sem4.openapispringboot.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Thanh toán khi nhận hàng (COD)")
public record CodPaymentRequest(
    @Schema(description = "Phương thức thanh toán", example = "COD", allowableValues = {"COD"})
    @NotNull
    PaymentMethod method
) implements PaymentRequest {
    public CodPaymentRequest() {
        this(PaymentMethod.COD);
    }

    @Override
    public PaymentMethod getMethod() {
        return method;
    }
}
