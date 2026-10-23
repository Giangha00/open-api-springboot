package com.sem4.openapispringboot.config;

import com.sem4.openapispringboot.service.JwtTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthFilter implements Filter {

    private final JwtTokenService jwtTokenService;
    private final String partnerKey;

    public AuthFilter(
        JwtTokenService jwtTokenService,
        @Value("${app.partner.api-key:partner-secret-key-2026}") String partnerKey
    ) {
        this.jwtTokenService = jwtTokenService;
        this.partnerKey = partnerKey;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // D5: X-Request-Id for all responses
        String requestId = httpRequest.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        httpResponse.setHeader("X-Request-Id", requestId);

        // Check Partner Key header
        String reqPartnerKey = httpRequest.getHeader("X-Partner-Key");
        if (reqPartnerKey != null && reqPartnerKey.equals(partnerKey)) {
            AuthContext.set(new AuthContext.UserPrincipal("partner@shipping.vn", null, "ROLE_PARTNER"));
        } else {
            // Check Bearer Token
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if ("dev-admin-token".equals(token)) {
                    AuthContext.set(new AuthContext.UserPrincipal("admin@techshop.vn", null, "ROLE_ADMIN"));
                } else if ("dev-customer-token".equals(token)) {
                    AuthContext.set(new AuthContext.UserPrincipal("customer@techshop.vn", 1L, "ROLE_CUSTOMER"));
                } else {
                    try {
                        Claims claims = jwtTokenService.parseToken(token);
                        String email = claims.getSubject();
                        String role = claims.get("role", String.class);
                        Long customerId = null;
                        Object cidObj = claims.get("customerId");
                        if (cidObj instanceof Number num) {
                            customerId = num.longValue();
                        }
                        AuthContext.set(new AuthContext.UserPrincipal(email, customerId, role));
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }
}
