package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.dto.auth.AuthTokenResponse;
import com.sem4.openapispringboot.entity.Customer;
import com.sem4.openapispringboot.repository.CustomerRepository;
import com.sem4.openapispringboot.service.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication (Dev)", description = "Cấp phát token thử nghiệm phục vụ kiểm thử Swagger UI & Frontend")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenService jwtTokenService;
    private final CustomerRepository customerRepository;

    public AuthController(JwtTokenService jwtTokenService, CustomerRepository customerRepository) {
        this.jwtTokenService = jwtTokenService;
        this.customerRepository = customerRepository;
    }

    @Operation(summary = "Lấy JWT Token nhanh cho vai trò CUSTOMER hoặc ADMIN để dùng trên Swagger Authorize / View")
    @PostMapping("/dev-token")
    public AuthTokenResponse getDevToken(
        @Parameter(description = "Chọn vai trò: 'CUSTOMER' hoặc 'ADMIN'", example = "CUSTOMER")
        @RequestParam(defaultValue = "CUSTOMER") String role,

        @Parameter(description = "Email khách hàng (nếu là CUSTOMER)", example = "customer@techshop.vn")
        @RequestParam(defaultValue = "customer@techshop.vn") String email
    ) {
        Long customerId = null;
        String formattedRole = "ADMIN".equalsIgnoreCase(role) ? "ROLE_ADMIN" : "ROLE_CUSTOMER";

        if ("ROLE_CUSTOMER".equals(formattedRole)) {
            Customer customer = customerRepository.findByEmail(email).orElseGet(() -> {
                Customer newC = new Customer(null, "Khách hàng mẫu", email, "0901234567");
                return customerRepository.save(newC);
            });
            customerId = customer.getId();
        }

        String token = jwtTokenService.generateToken(email, customerId, formattedRole);

        return new AuthTokenResponse(
            token,
            "Bearer",
            formattedRole,
            customerId,
            email,
            86400L
        );
    }
}
