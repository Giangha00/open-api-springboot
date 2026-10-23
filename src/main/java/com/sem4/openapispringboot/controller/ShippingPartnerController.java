package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.dto.order.OrderTransitionRequest;
import com.sem4.openapispringboot.dto.order.OrderTransitionResponse;
import com.sem4.openapispringboot.dto.order.ShippingWebhookRequest;
import com.sem4.openapispringboot.entity.Order;
import com.sem4.openapispringboot.exception.OrderNotFoundException;
import com.sem4.openapispringboot.repository.OrderRepository;
import com.sem4.openapispringboot.service.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShippingPartnerController implements ShippingPartnerApi {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final String expectedPartnerKey;

    public ShippingPartnerController(
        OrderService orderService,
        OrderRepository orderRepository,
        @Value("${app.partner.api-key:partner-secret-key-2026}") String expectedPartnerKey
    ) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.expectedPartnerKey = expectedPartnerKey;
    }

    @Override
    public ResponseEntity<OrderTransitionResponse> handleShippingWebhook(String partnerKey, ShippingWebhookRequest request) {
        if (partnerKey == null || !partnerKey.equals(expectedPartnerKey)) {
            throw new SecurityException("X-Partner-Key không chính xác hoặc không có quyền truy cập");
        }

        Order order = orderRepository.findByCode(request.code())
            .orElseThrow(() -> new OrderNotFoundException(request.code()));

        // Webhook handles optimistic locking internally using current order version
        String ifMatch = String.valueOf(order.getVersion());
        OrderTransitionRequest transitionRequest = new OrderTransitionRequest(request.action(), request.notes());

        OrderTransitionResponse response = orderService.transitionOrder(
            request.code(),
            transitionRequest,
            ifMatch,
            null,
            "ROLE_PARTNER"
        );

        return ResponseEntity.ok(response);
    }
}
