package com.sem4.openapispringboot.config;

import com.sem4.openapispringboot.dto.order.CreateOrderRequest;
import com.sem4.openapispringboot.dto.order.OrderItemRequest;
import com.sem4.openapispringboot.dto.payment.CardPaymentRequest;
import com.sem4.openapispringboot.entity.Customer;
import com.sem4.openapispringboot.entity.Product;
import com.sem4.openapispringboot.repository.CustomerRepository;
import com.sem4.openapispringboot.repository.ProductRepository;
import com.sem4.openapispringboot.service.OrderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderService orderService;

    public DataInitializer(ProductRepository productRepository, CustomerRepository customerRepository, OrderService orderService) {
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.orderService = orderService;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() == 0) {
            Product p1 = new Product(null, "Bàn phím cơ K8 Pro", "Switch Gateron Brown, layout 87 phím, Bluetooth 5.1 & Type-C", new BigDecimal("1590000"), null, null);
            Product p2 = new Product(null, "Chuột không dây MX Master 3S", "Cảm biến 8K DPI Darkfield, cuộn MagSpeed siêu tốc, sạc Type-C", new BigDecimal("2250000"), null, null);
            Product p3 = new Product(null, "Màn hình Dell UltraSharp 27 inch 4K", "Tấm nền IPS Black, độ phân giải 3840x2160, 100% sRGB, Type-C 90W", new BigDecimal("12500000"), null, null);
            Product p4 = new Product(null, "Tai nghe chống ồn Sony WH-1000XM5", "Chống ồn đỉnh cao ANC, pin 30 giờ, âm thanh Hi-Res Audio Wireless", new BigDecimal("6990000"), null, null);
            Product p5 = new Product(null, "Ghế công thái học Ergonomic Chair", "Khung nhôm nguyên khối, tựa lưng 3D điều chỉnh linh hoạt, lưới nhập khẩu", new BigDecimal("8500000"), null, null);

            productRepository.saveAll(List.of(p1, p2, p3, p4, p5));
        }

        Customer c1 = customerRepository.findByEmail("customer@techshop.vn").orElseGet(() -> {
            Customer customer = new Customer(null, "Nguyễn Văn Tuấn", "customer@techshop.vn", "0901234567");
            return customerRepository.save(customer);
        });

        customerRepository.findByEmail("mai.tran@techshop.vn").orElseGet(() -> {
            Customer customer = new Customer(null, "Trần Thị Mai", "mai.tran@techshop.vn", "0987654321");
            return customerRepository.save(customer);
        });

        // Seed a sample order if no orders exist yet
        try {
            orderService.createOrder(
                new CreateOrderRequest(
                    "Số 8 Tôn Thất Thuyết, Dịch Vọng Hậu, Cầu Giấy, Hà Nội",
                    List.of(
                        new OrderItemRequest(1L, 1),
                        new OrderItemRequest(2L, 1)
                    ),
                    new CardPaymentRequest("VISA", "4242")
                ),
                c1.getId(),
                "INIT-IDEMPOTENCY-KEY-001"
            );
        } catch (Exception ignored) {
        }
    }
}
