package com.sem4.openapispringboot.dto.order;

import com.sem4.openapispringboot.dto.payment.PaymentRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Yêu cầu tạo đơn hàng mới")
public record CreateOrderRequest(
    @Schema(description = "Địa chỉ nhận hàng chi tiết", example = "Số 8 Tôn Thất Thuyết, Cầu Giấy, Hà Nội")
    @NotBlank
    @Size(max = 300)
    String shippingAddress,

    @Schema(description = "Danh sách sản phẩm trong đơn (1 - 50 dòng, không trùng productId)")
    @NotEmpty(message = "items list cannot be empty")
    @Size(max = 50, message = "cannot exceed 50 items per order")
    List<@Valid OrderItemRequest> items,

    @Schema(description = "Phương thức thanh toán đã chọn (đa hình COD/CARD/BANK_TRANSFER)")
    @NotNull
    @Valid
    PaymentRequest payment
) {
}
