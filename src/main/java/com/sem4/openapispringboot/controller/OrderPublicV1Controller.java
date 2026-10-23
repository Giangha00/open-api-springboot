package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.config.AuthContext;
import com.sem4.openapispringboot.dto.order.OrderResponseV1;
import com.sem4.openapispringboot.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderPublicV1Controller implements OrderPublicV1Api {

    private final OrderService orderService;

    public OrderPublicV1Controller(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ResponseEntity<OrderResponseV1> getOrderByCodeV1(String code) {
        AuthContext.UserPrincipal user = AuthContext.get();
        boolean isAdmin = user != null && "ROLE_ADMIN".equalsIgnoreCase(user.role());
        Long customerId = user != null ? user.customerId() : null;

        OrderResponseV1 response = orderService.getOrderByCodeV1(code, customerId, isAdmin);
        return ResponseEntity.ok()
            .eTag("\"" + response.version() + "\"")
            .body(response);
    }
}
