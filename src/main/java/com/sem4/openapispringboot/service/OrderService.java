package com.sem4.openapispringboot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sem4.openapispringboot.common.PageResponse;
import com.sem4.openapispringboot.dto.order.*;
import com.sem4.openapispringboot.dto.payment.*;
import com.sem4.openapispringboot.entity.*;
import com.sem4.openapispringboot.exception.*;
import com.sem4.openapispringboot.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderEventRepository orderEventRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        PaymentRepository paymentRepository,
        OrderEventRepository orderEventRepository,
        IdempotencyRecordRepository idempotencyRecordRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.orderEventRepository = orderEventRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponseV2 createOrder(CreateOrderRequest request, Long customerId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessValidationException("MISSING_IDEMPOTENCY_KEY", "Idempotency-Key header is required for creating an order.");
        }

        String requestHash = computeSha256(serializeForHash(request));

        Optional<IdempotencyRecord> existingOpt = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOpt.isPresent()) {
            IdempotencyRecord record = existingOpt.get();
            if (Instant.now().isBefore(record.getExpiresAt())) {
                if (record.getRequestHash().equals(requestHash)) {
                    try {
                        return objectMapper.readValue(record.getResponseBody(), OrderResponseV2.class);
                    } catch (Exception e) {
                        // fallback to querying fresh order from code
                    }
                } else {
                    throw new IdempotencyConflictException(
                        idempotencyKey,
                        "Idempotency conflict: A request with key '" + idempotencyKey + "' was already processed with different payload."
                    );
                }
            }
        }

        // Validate items: max 50 items, no duplicate productId
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessValidationException("EMPTY_ITEMS", "Order items list cannot be empty.");
        }
        if (request.items().size() > 50) {
            throw new BusinessValidationException("TOO_MANY_ITEMS", "An order can contain at most 50 items.");
        }

        Set<Long> seenProductIds = new HashSet<>();
        for (OrderItemRequest item : request.items()) {
            if (!seenProductIds.add(item.productId())) {
                throw new BusinessValidationException("DUPLICATE_PRODUCT", "Duplicate productId " + item.productId() + " in order items.");
            }
            if (item.quantity() < 1 || item.quantity() > 20) {
                throw new BusinessValidationException("INVALID_QUANTITY", "Quantity for productId " + item.productId() + " must be between 1 and 20.");
            }
        }

        // Calculate totals on server from DB product prices
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItemsToSave = new ArrayList<>();

        for (OrderItemRequest itemReq : request.items()) {
            Product product = productRepository.findById(itemReq.productId())
                .orElseThrow(() -> new ProductNotFoundException(itemReq.productId()));

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.quantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = new OrderItem(
                0L, // will set orderId after order is saved
                product.getId(),
                product.getName(),
                product.getPrice(),
                itemReq.quantity(),
                lineTotal
            );
            orderItemsToSave.add(orderItem);
        }

        String code = generateOrderCode();

        Order order = new Order();
        order.setCode(code);
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.shippingAddress());
        order.setTotalAmount(totalAmount);
        order.setCurrency("VND");
        order.setVersion(0L);

        Order savedOrder = orderRepository.saveAndFlush(order);

        for (OrderItem item : orderItemsToSave) {
            item.setOrderId(savedOrder.getId());
            orderItemRepository.save(item);
        }

        // Save polymorphic payment
        Payment payment = new Payment(savedOrder.getId(), request.payment().getMethod(), totalAmount);
        if (request.payment() instanceof CardPaymentRequest card) {
            payment.setProvider(card.provider());
            payment.setLast4(card.last4());
        } else if (request.payment() instanceof BankTransferPaymentRequest bank) {
            payment.setBankCode(bank.bankCode());
            payment.setTransferRef(bank.transferRef());
        }
        paymentRepository.save(payment);

        // Record Initial Event
        OrderEvent initialEvent = new OrderEvent(
            savedOrder.getId(),
            savedOrder.getCode(),
            null,
            OrderStatus.PENDING,
            OrderAction.CONFIRM,
            "CUSTOMER",
            "Đơn hàng được khởi tạo thành công"
        );
        orderEventRepository.save(initialEvent);

        OrderResponseV2 response = buildOrderResponseV2(savedOrder);

        // Save Idempotency record with 24 hours expiry
        try {
            String serialized = objectMapper.writeValueAsString(response);
            IdempotencyRecord record = new IdempotencyRecord(
                idempotencyKey,
                requestHash,
                serialized,
                201,
                Instant.now().plus(24, ChronoUnit.HOURS)
            );
            idempotencyRecordRepository.save(record);
        } catch (Exception ignored) {
        }

        return response;
    }

    public PageResponse<OrderResponseV2> getOrders(
        Long customerId,
        boolean isAdmin,
        OrderStatus status,
        LocalDate from,
        LocalDate to,
        BigDecimal minTotal,
        Pageable pageable
    ) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isAdmin && customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (from != null) {
                Instant fromInstant = from.atStartOfDay(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }

            if (to != null) {
                Instant toInstant = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.lessThan(root.get("createdAt"), toInstant));
            }

            if (minTotal != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), minTotal));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        List<OrderResponseV2> dtos = orderPage.getContent().stream()
            .map(this::buildOrderResponseV2)
            .toList();

        return new PageResponse<>(
            dtos,
            orderPage.getNumber(),
            orderPage.getSize(),
            orderPage.getTotalElements(),
            orderPage.getTotalPages(),
            orderPage.isLast()
        );
    }

    public OrderResponseV1 getOrderByCodeV1(String code, Long customerId, boolean isAdmin) {
        Order order = findOrderByCodeAndCheckPermission(code, customerId, isAdmin);
        return buildOrderResponseV1(order);
    }

    public OrderResponseV2 getOrderByCodeV2(String code, Long customerId, boolean isAdmin) {
        Order order = findOrderByCodeAndCheckPermission(code, customerId, isAdmin);
        return buildOrderResponseV2(order);
    }

    @Transactional
    public OrderTransitionResponse transitionOrder(
        String code,
        OrderTransitionRequest request,
        String ifMatchHeader,
        Long customerId,
        String role
    ) {
        Order order = orderRepository.findByCode(code)
            .orElseThrow(() -> new OrderNotFoundException(code));

        // Validate If-Match header (precondition check)
        validateIfMatch(ifMatchHeader, order.getVersion());

        OrderStatus currentStatus = order.getStatus();
        OrderAction action = request.action();
        OrderStatus newStatus;

        boolean isAdmin = "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
        boolean isPartner = "ROLE_PARTNER".equalsIgnoreCase(role) || "PARTNER".equalsIgnoreCase(role);
        boolean isCustomer = !isAdmin && !isPartner;

        if (isCustomer && !order.getCustomerId().equals(customerId)) {
            throw new SecurityException("Bạn không có quyền thao tác trên đơn hàng của người khác");
        }

        switch (action) {
            case CONFIRM -> {
                if (!isAdmin) {
                    throw new SecurityException("Chỉ ADMIN mới có quyền xác nhận đơn hàng");
                }
                if (currentStatus != OrderStatus.PENDING) {
                    throw new InvalidTransitionException(code, currentStatus.name(), action.name(),
                        "Chỉ có thể CONFIRM khi đơn ở trạng thái PENDING (hiện tại: " + currentStatus + ")");
                }
                newStatus = OrderStatus.CONFIRMED;
            }
            case SHIP -> {
                if (!isAdmin && !isPartner) {
                    throw new SecurityException("Chỉ ADMIN hoặc đối tác vận chuyển mới có quyền chuyển sang SHIPPING");
                }
                if (currentStatus != OrderStatus.CONFIRMED) {
                    throw new InvalidTransitionException(code, currentStatus.name(), action.name(),
                        "Chỉ có thể SHIP khi đơn ở trạng thái CONFIRMED (hiện tại: " + currentStatus + ")");
                }
                newStatus = OrderStatus.SHIPPING;
            }
            case DELIVER -> {
                if (!isAdmin && !isPartner) {
                    throw new SecurityException("Chỉ đối tác vận chuyển hoặc ADMIN mới có quyền xác nhận DELIVER");
                }
                if (currentStatus != OrderStatus.SHIPPING) {
                    throw new InvalidTransitionException(code, currentStatus.name(), action.name(),
                        "Chỉ có thể DELIVER khi đơn đang ở trạng thái SHIPPING (hiện tại: " + currentStatus + ")");
                }
                newStatus = OrderStatus.DELIVERED;
                order.setDeliveredAt(Instant.now());
            }
            case FAIL_DELIVERY -> {
                if (!isAdmin && !isPartner) {
                    throw new SecurityException("Chỉ đối tác vận chuyển hoặc ADMIN mới có quyền báo giao hàng thất bại");
                }
                if (currentStatus != OrderStatus.SHIPPING) {
                    throw new InvalidTransitionException(code, currentStatus.name(), action.name(),
                        "Chỉ có thể báo FAIL_DELIVERY khi đơn đang SHIPPING (hiện tại: " + currentStatus + ")");
                }
                newStatus = OrderStatus.CONFIRMED; // quay về CONFIRMED để giao lại theo máy trạng thái
            }
            case CANCEL -> {
                if (currentStatus == OrderStatus.PENDING) {
                    // Cả khách và ADMIN đều được hủy khi PENDING
                    newStatus = OrderStatus.CANCELLED;
                } else if (currentStatus == OrderStatus.CONFIRMED) {
                    if (!isAdmin) {
                        throw new InvalidTransitionException(code, currentStatus.name(), action.name(),
                            "Khách hàng chỉ được phép hủy đơn khi đơn còn PENDING.");
                    }
                    if (request.reason() == null || request.reason().trim().isEmpty()) {
                        throw new BusinessValidationException("REASON_REQUIRED",
                            "Hủy đơn ở trạng thái CONFIRMED bắt buộc phải có lý do hủy.");
                    }
                    newStatus = OrderStatus.CANCELLED;
                } else {
                    throw new InvalidTransitionException(code, currentStatus.name(), action.name(),
                        "Đơn hàng chỉ được hủy trước khi giao (PENDING hoặc CONFIRMED). Không thể hủy ở trạng thái: " + currentStatus);
                }
            }
            case REQUEST_RETURN -> {
                if (currentStatus != OrderStatus.DELIVERED) {
                    throw new InvalidTransitionException(code, currentStatus.name(), action.name(),
                        "Chỉ có thể yêu cầu trả hàng khi đơn đã DELIVERED.");
                }
                if (order.getDeliveredAt() != null) {
                    long daysSinceDelivery = ChronoUnit.DAYS.between(order.getDeliveredAt(), Instant.now());
                    if (daysSinceDelivery > 7) {
                        throw new InvalidTransitionException(code, currentStatus.name(), action.name(),
                            "Yêu cầu trả hàng chỉ hợp lệ trong vòng 7 ngày kể từ khi đơn được giao thành công.");
                    }
                }
                newStatus = OrderStatus.RETURN_REQUESTED;
            }
            case ACCEPT_RETURN -> {
                if (!isAdmin) {
                    throw new SecurityException("Chỉ ADMIN mới có quyền chấp nhận trả hàng");
                }
                if (currentStatus != OrderStatus.RETURN_REQUESTED) {
                    throw new InvalidTransitionException(code, currentStatus.name(), action.name(),
                        "Chỉ có thể ACCEPT_RETURN khi đơn đang ở trạng thái RETURN_REQUESTED (hiện tại: " + currentStatus + ")");
                }
                newStatus = OrderStatus.RETURNED;
            }
            default -> throw new InvalidTransitionException(code, currentStatus.name(), action.name(), "Hành động không hợp lệ");
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.saveAndFlush(order);

        String actorName = isAdmin ? "ADMIN" : (isPartner ? "PARTNER" : "CUSTOMER");
        OrderEvent event = new OrderEvent(
            updatedOrder.getId(),
            updatedOrder.getCode(),
            currentStatus,
            newStatus,
            action,
            actorName,
            request.reason() != null && !request.reason().isBlank() ? request.reason() : ("Thực hiện " + action)
        );
        orderEventRepository.save(event);

        return new OrderTransitionResponse(
            updatedOrder.getCode(),
            currentStatus,
            newStatus,
            action,
            updatedOrder.getVersion(),
            "Chuyển trạng thái đơn hàng " + code + " sang " + newStatus + " thành công."
        );
    }

    public List<OrderEventResponse> getOrderEvents(String code) {
        List<OrderEvent> events = orderEventRepository.findByOrderCodeOrderByAtAsc(code);
        return events.stream().map(e -> new OrderEventResponse(
            e.getId(),
            e.getOrderCode(),
            e.getFromStatus(),
            e.getToStatus(),
            e.getAction(),
            e.getActor(),
            e.getReason(),
            e.getAt()
        )).toList();
    }

    private Order findOrderByCodeAndCheckPermission(String code, Long customerId, boolean isAdmin) {
        Order order = orderRepository.findByCode(code)
            .orElseThrow(() -> new OrderNotFoundException(code));

        if (!isAdmin && customerId != null && !order.getCustomerId().equals(customerId)) {
            throw new SecurityException("Bạn không có quyền xem thông tin đơn hàng này");
        }
        return order;
    }

    public OrderResponseV1 buildOrderResponseV1(Order order) {
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
            .map(this::toItemResponse)
            .toList();

        PaymentResponse paymentResponse = paymentRepository.findByOrderId(order.getId())
            .map(this::toPaymentResponse)
            .orElse(null);

        return new OrderResponseV1(
            order.getCode(),
            order.getCustomerId(),
            order.getStatus(),
            order.getShippingAddress(),
            order.getTotalAmount(),
            order.getCurrency(),
            order.getVersion(),
            order.getCreatedAt(),
            order.getDeliveredAt(),
            items,
            paymentResponse,
            order.getInvoiceData() != null
        );
    }

    public OrderResponseV2 buildOrderResponseV2(Order order) {
        List<OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
            .map(this::toItemResponse)
            .toList();

        PaymentResponse paymentResponse = paymentRepository.findByOrderId(order.getId())
            .map(this::toPaymentResponse)
            .orElse(null);

        return new OrderResponseV2(
            order.getCode(),
            order.getCustomerId(),
            order.getStatus(),
            order.getShippingAddress(),
            new TotalAmountDto(order.getTotalAmount(), order.getCurrency()),
            order.getVersion(),
            order.getCreatedAt(),
            order.getDeliveredAt(),
            items,
            paymentResponse,
            order.getInvoiceData() != null
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getProductId(),
            item.getProductName(),
            item.getUnitPrice(),
            item.getQuantity(),
            item.getLineTotal()
        );
    }

    private PaymentResponse toPaymentResponse(Payment p) {
        return new PaymentResponse(
            p.getMethod(),
            p.getAmount(),
            p.getProvider(),
            p.getLast4(),
            p.getBankCode(),
            p.getTransferRef(),
            p.getPaidAt()
        );
    }

    private void validateIfMatch(String ifMatchHeader, Long currentVersion) {
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            throw new BusinessValidationException("MISSING_IF_MATCH", "If-Match header is required for state transition.");
        }
        String cleanHeader = ifMatchHeader.trim();
        if (cleanHeader.startsWith("\"") && cleanHeader.endsWith("\"") && cleanHeader.length() >= 2) {
            cleanHeader = cleanHeader.substring(1, cleanHeader.length() - 1);
        }
        String currentVersionStr = String.valueOf(currentVersion);
        if (!cleanHeader.equals(currentVersionStr)) {
            throw new PreconditionFailedException(cleanHeader, currentVersionStr);
        }
    }

    private String generateOrderCode() {
        long timestampSuffix = System.currentTimeMillis() % 1000000;
        int random = new Random().nextInt(900) + 100;
        return String.format("ORD-2026-%03d%03d", random, timestampSuffix % 1000);
    }

    private String serializeForHash(CreateOrderRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            return request.toString();
        }
    }

    private String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
