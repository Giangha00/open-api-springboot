package com.sem4.openapispringboot.dto.payment;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.sem4.openapispringboot.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "method",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CodPaymentRequest.class, name = "COD"),
    @JsonSubTypes.Type(value = CardPaymentRequest.class, name = "CARD"),
    @JsonSubTypes.Type(value = BankTransferPaymentRequest.class, name = "BANK_TRANSFER")
})
@Schema(
    description = "Thông tin thanh toán đa hình theo phương thức lựa chọn",
    discriminatorProperty = "method",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "COD", schema = CodPaymentRequest.class),
        @DiscriminatorMapping(value = "CARD", schema = CardPaymentRequest.class),
        @DiscriminatorMapping(value = "BANK_TRANSFER", schema = BankTransferPaymentRequest.class)
    },
    oneOf = {
        CodPaymentRequest.class,
        CardPaymentRequest.class,
        BankTransferPaymentRequest.class
    }
)
public sealed interface PaymentRequest permits CodPaymentRequest, CardPaymentRequest, BankTransferPaymentRequest {
    PaymentMethod getMethod();
}
