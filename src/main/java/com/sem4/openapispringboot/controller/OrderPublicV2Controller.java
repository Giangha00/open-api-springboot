package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.config.AuthContext;
import com.sem4.openapispringboot.common.PageResponse;
import com.sem4.openapispringboot.dto.order.*;
import com.sem4.openapispringboot.entity.OrderStatus;
import com.sem4.openapispringboot.service.InvoiceService;
import com.sem4.openapispringboot.service.OrderService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
public class OrderPublicV2Controller implements OrderPublicV2Api {

    private final OrderService orderService;
    private final InvoiceService invoiceService;

    public OrderPublicV2Controller(OrderService orderService, InvoiceService invoiceService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
    }

    @Override
    public ResponseEntity<OrderResponseV2> createOrder(String idempotencyKey, CreateOrderRequest request) {
        AuthContext.UserPrincipal user = AuthContext.get();
        Long customerId = (user != null && user.customerId() != null) ? user.customerId() : 1L;

        OrderResponseV2 response = orderService.createOrder(request, customerId, idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/orders/{code}").buildAndExpand(response.code()).toUri();

        return ResponseEntity.created(location)
            .eTag("\"" + response.version() + "\"")
            .body(response);
    }

    @Override
    public PageResponse<OrderResponseV2> getOrders(
        OrderStatus status,
        LocalDate from,
        LocalDate to,
        BigDecimal minTotal,
        int page,
        int size,
        String sort
    ) {
        AuthContext.UserPrincipal user = AuthContext.get();
        boolean isAdmin = user != null && "ROLE_ADMIN".equalsIgnoreCase(user.role());
        Long customerId = user != null ? user.customerId() : 1L;

        Sort sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            String prop = parts[0].trim();
            Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
            sortObj = Sort.by(dir, prop);
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        return orderService.getOrders(customerId, isAdmin, status, from, to, minTotal, pageable);
    }

    @Override
    public ResponseEntity<OrderResponseV2> getOrderByCodeV2(String code) {
        AuthContext.UserPrincipal user = AuthContext.get();
        boolean isAdmin = user != null && "ROLE_ADMIN".equalsIgnoreCase(user.role());
        Long customerId = user != null ? user.customerId() : null;

        OrderResponseV2 response = orderService.getOrderByCodeV2(code, customerId, isAdmin);
        return ResponseEntity.ok()
            .eTag("\"" + response.version() + "\"")
            .body(response);
    }

    @Override
    public ResponseEntity<OrderTransitionResponse> transitionOrder(
        String code,
        String ifMatch,
        OrderTransitionRequest request
    ) {
        AuthContext.UserPrincipal user = AuthContext.get();
        String role = user != null ? user.role() : "ROLE_CUSTOMER";
        Long customerId = user != null ? user.customerId() : 1L;

        OrderTransitionResponse response = orderService.transitionOrder(code, request, ifMatch, customerId, role);
        return ResponseEntity.ok()
            .eTag("\"" + response.version() + "\"")
            .body(response);
    }

    @Override
    public ResponseEntity<Resource> getInvoice(String code) {
        AuthContext.UserPrincipal user = AuthContext.get();
        boolean isAdmin = user != null && "ROLE_ADMIN".equalsIgnoreCase(user.role());
        Long customerId = user != null ? user.customerId() : null;

        InvoiceService.InvoiceDownload invoice = invoiceService.getInvoice(code, customerId, isAdmin);
        ByteArrayResource resource = new ByteArrayResource(invoice.data());

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + invoice.fileName() + "\"")
            .body(resource);
    }

    @Override
    public List<OrderEventResponse> getOrderEvents(String code) {
        return orderService.getOrderEvents(code);
    }
}
