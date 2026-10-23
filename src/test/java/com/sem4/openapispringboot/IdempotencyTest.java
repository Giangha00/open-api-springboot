package com.sem4.openapispringboot;

import com.sem4.openapispringboot.dto.order.CreateOrderRequest;
import com.sem4.openapispringboot.dto.order.OrderItemRequest;
import com.sem4.openapispringboot.dto.order.OrderResponseV2;
import com.sem4.openapispringboot.dto.payment.CodPaymentRequest;
import com.sem4.openapispringboot.exception.BusinessValidationException;
import com.sem4.openapispringboot.exception.IdempotencyConflictException;
import com.sem4.openapispringboot.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IdempotencyTest {

    @Autowired
    OrderService orderService;

    private final Long customerId = 1L;

    @Test
    @DisplayName("Idempotency: Cùng Idempotency-Key và cùng body trong 24h -> trả lại đúng kết quả lần đầu")
    void testSameKeySameBodyReturnsCachedOrder() {
        String key = "IDEMP-" + UUID.randomUUID();
        CreateOrderRequest req = new CreateOrderRequest(
            "Hà Nội",
            List.of(new OrderItemRequest(1L, 2)),
            new CodPaymentRequest()
        );

        OrderResponseV2 res1 = orderService.createOrder(req, customerId, key);
        assertNotNull(res1);

        OrderResponseV2 res2 = orderService.createOrder(req, customerId, key);
        assertNotNull(res2);
        assertEquals(res1.code(), res2.code(), "Mã đơn hàng trả về phải trùng khớp với lần đầu");
        assertEquals(res1.total().value(), res2.total().value(), "Tổng tiền phải bằng nhau");
    }

    @Test
    @DisplayName("Idempotency: Cùng Idempotency-Key nhưng payload khác biệt -> ném lỗi 409 IDEMPOTENCY_CONFLICT")
    void testSameKeyDifferentBodyThrowsConflict() {
        String key = "IDEMP-CONFLICT-" + UUID.randomUUID();
        CreateOrderRequest req1 = new CreateOrderRequest(
            "Địa chỉ 1",
            List.of(new OrderItemRequest(1L, 1)),
            new CodPaymentRequest()
        );

        CreateOrderRequest req2 = new CreateOrderRequest(
            "Địa chỉ 2 hoàn toàn khác",
            List.of(new OrderItemRequest(2L, 2)),
            new CodPaymentRequest()
        );

        OrderResponseV2 res1 = orderService.createOrder(req1, customerId, key);
        assertNotNull(res1);

        assertThrows(IdempotencyConflictException.class, () -> {
            orderService.createOrder(req2, customerId, key);
        }, "Cùng key khác body phải ném IdempotencyConflictException (409 Conflict)");
    }

    @Test
    @DisplayName("Idempotency: Thiếu header Idempotency-Key -> ném lỗi hợp lệ")
    void testMissingKeyThrowsValidation() {
        CreateOrderRequest req = new CreateOrderRequest(
            "Địa chỉ",
            List.of(new OrderItemRequest(1L, 1)),
            new CodPaymentRequest()
        );

        assertThrows(BusinessValidationException.class, () -> {
            orderService.createOrder(req, customerId, null);
        });
    }
}
