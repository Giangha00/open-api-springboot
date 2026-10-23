package com.sem4.openapispringboot;

import com.sem4.openapispringboot.dto.order.CreateOrderRequest;
import com.sem4.openapispringboot.dto.order.OrderItemRequest;
import com.sem4.openapispringboot.dto.order.OrderResponseV2;
import com.sem4.openapispringboot.dto.order.OrderTransitionRequest;
import com.sem4.openapispringboot.dto.order.OrderTransitionResponse;
import com.sem4.openapispringboot.dto.payment.CodPaymentRequest;
import com.sem4.openapispringboot.entity.OrderAction;
import com.sem4.openapispringboot.entity.OrderStatus;
import com.sem4.openapispringboot.exception.BusinessValidationException;
import com.sem4.openapispringboot.exception.InvalidTransitionException;
import com.sem4.openapispringboot.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StateMachineTest {

    @Autowired
    OrderService orderService;

    private Long customerId = 1L;

    private OrderResponseV2 createTestOrder() {
        String key = "TEST-SM-" + UUID.randomUUID();
        CreateOrderRequest req = new CreateOrderRequest(
            "Hà Nội",
            List.of(new OrderItemRequest(1L, 1)),
            new CodPaymentRequest()
        );
        return orderService.createOrder(req, customerId, key);
    }

    @Test
    @DisplayName("Hợp lệ: Luồng đơn hàng đầy đủ từ PENDING -> CONFIRMED -> SHIPPING -> DELIVERED -> RETURN_REQUESTED -> RETURNED")
    void testFullHappyPathWorkflow() {
        OrderResponseV2 order = createTestOrder();
        assertEquals(OrderStatus.PENDING, order.status());

        // 1. CONFIRM by ADMIN
        OrderTransitionResponse conf = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.CONFIRM, "Admin duyệt đơn"),
            String.valueOf(order.version()),
            null,
            "ROLE_ADMIN"
        );
        assertEquals(OrderStatus.CONFIRMED, conf.toStatus());

        // 2. SHIP by ADMIN
        OrderTransitionResponse ship = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.SHIP, "Giao cho bưu tá"),
            String.valueOf(conf.version()),
            null,
            "ROLE_ADMIN"
        );
        assertEquals(OrderStatus.SHIPPING, ship.toStatus());

        // 3. DELIVER by PARTNER
        OrderTransitionResponse deliver = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.DELIVER, "Đã giao tận tay"),
            String.valueOf(ship.version()),
            null,
            "ROLE_PARTNER"
        );
        assertEquals(OrderStatus.DELIVERED, deliver.toStatus());

        // 4. REQUEST_RETURN by CUSTOMER (trong vòng 7 ngày)
        OrderTransitionResponse retReq = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.REQUEST_RETURN, "Sản phẩm không vừa ý"),
            String.valueOf(deliver.version()),
            customerId,
            "ROLE_CUSTOMER"
        );
        assertEquals(OrderStatus.RETURN_REQUESTED, retReq.toStatus());

        // 5. ACCEPT_RETURN by ADMIN
        OrderTransitionResponse returned = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.ACCEPT_RETURN, "Kho đã nhận lại hàng"),
            String.valueOf(retReq.version()),
            null,
            "ROLE_ADMIN"
        );
        assertEquals(OrderStatus.RETURNED, returned.toStatus());
    }

    @Test
    @DisplayName("Hợp lệ: Giao thất bại FAIL_DELIVERY chuyển SHIPPING -> CONFIRMED")
    void testFailDeliveryRetry() {
        OrderResponseV2 order = createTestOrder();

        OrderTransitionResponse conf = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.CONFIRM, "Duyệt"),
            String.valueOf(order.version()),
            null,
            "ROLE_ADMIN"
        );

        OrderTransitionResponse ship = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.SHIP, "Xuất kho"),
            String.valueOf(conf.version()),
            null,
            "ROLE_ADMIN"
        );

        OrderTransitionResponse fail = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.FAIL_DELIVERY, "Không liên lạc được khách"),
            String.valueOf(ship.version()),
            null,
            "ROLE_PARTNER"
        );
        assertEquals(OrderStatus.CONFIRMED, fail.toStatus());
    }

    @Test
    @DisplayName("Hợp lệ: Hủy đơn PENDING bởi Khách hàng")
    void testCustomerCancelPending() {
        OrderResponseV2 order = createTestOrder();
        OrderTransitionResponse cancel = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.CANCEL, "Khách đổi ý"),
            String.valueOf(order.version()),
            customerId,
            "ROLE_CUSTOMER"
        );
        assertEquals(OrderStatus.CANCELLED, cancel.toStatus());
    }

    @Test
    @DisplayName("Hợp lệ: Hủy đơn CONFIRMED bởi ADMIN (kèm reason bắt buộc)")
    void testAdminCancelConfirmedWithReason() {
        OrderResponseV2 order = createTestOrder();

        OrderTransitionResponse conf = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.CONFIRM, "Duyệt"),
            String.valueOf(order.version()),
            null,
            "ROLE_ADMIN"
        );

        OrderTransitionResponse cancel = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.CANCEL, "Hết hàng trong kho"),
            String.valueOf(conf.version()),
            null,
            "ROLE_ADMIN"
        );
        assertEquals(OrderStatus.CANCELLED, cancel.toStatus());
    }

    @Test
    @DisplayName("Không hợp lệ: Hủy đơn CONFIRMED nhưng thiếu reason -> Lỗi")
    void testAdminCancelConfirmedWithoutReasonThrows() {
        OrderResponseV2 order = createTestOrder();

        OrderTransitionResponse conf = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.CONFIRM, "Duyệt"),
            String.valueOf(order.version()),
            null,
            "ROLE_ADMIN"
        );

        assertThrows(BusinessValidationException.class, () -> {
            orderService.transitionOrder(
                order.code(),
                new OrderTransitionRequest(OrderAction.CANCEL, ""),
                String.valueOf(conf.version()),
                null,
                "ROLE_ADMIN"
            );
        });
    }

    @Test
    @DisplayName("Không hợp lệ: Đơn đang SHIPPING không thể hủy")
    void testCannotCancelShipping() {
        OrderResponseV2 order = createTestOrder();

        OrderTransitionResponse conf = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.CONFIRM, "Duyệt"),
            String.valueOf(order.version()),
            null,
            "ROLE_ADMIN"
        );

        OrderTransitionResponse ship = orderService.transitionOrder(
            order.code(),
            new OrderTransitionRequest(OrderAction.SHIP, "Giao"),
            String.valueOf(conf.version()),
            null,
            "ROLE_ADMIN"
        );

        assertThrows(InvalidTransitionException.class, () -> {
            orderService.transitionOrder(
                order.code(),
                new OrderTransitionRequest(OrderAction.CANCEL, "Muốn hủy"),
                String.valueOf(ship.version()),
                null,
                "ROLE_ADMIN"
            );
        });
    }

    @Test
    @DisplayName("Không hợp lệ: Khách hàng không có quyền CONFIRM đơn hàng")
    void testCustomerCannotConfirm() {
        OrderResponseV2 order = createTestOrder();

        assertThrows(SecurityException.class, () -> {
            orderService.transitionOrder(
                order.code(),
                new OrderTransitionRequest(OrderAction.CONFIRM, "Khách tự duyệt"),
                String.valueOf(order.version()),
                customerId,
                "ROLE_CUSTOMER"
            );
        });
    }
}
