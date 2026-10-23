package com.sem4.openapispringboot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.api.version:1.0.0}")
    private String version;

    @Value("${app.api.server-url:http://localhost:8080}")
    private String serverUrl;

    // D1: Dropdown 4 nhóm spec: public-v1, public-v2, admin, partner
    @Bean
    public GroupedOpenApi publicV1Group() {
        return GroupedOpenApi.builder()
            .group("public-v1")
            .displayName("1. Public API (v1 - Deprecated)")
            .pathsToMatch("/api/v1/**", "/api/orders/**")
            .pathsToExclude("/api/orders/{code}/invoice", "/api/v2/**", "/api/admin/**", "/api/webhooks/**")
            .addOperationCustomizer(globalHeaderCustomizer())
            .build();
    }

    @Bean
    public GroupedOpenApi publicV2Group() {
        return GroupedOpenApi.builder()
            .group("public-v2")
            .displayName("2. Public API (v2 - Standard)")
            .pathsToMatch("/api/v2/**", "/api/orders", "/api/orders/*/transitions", "/api/orders/*/invoice")
            .pathsToExclude("/api/admin/**", "/api/webhooks/**")
            .addOperationCustomizer(globalHeaderCustomizer())
            .build();
    }

    @Bean
    public GroupedOpenApi adminGroup() {
        return GroupedOpenApi.builder()
            .group("admin")
            .displayName("3. Admin API")
            .pathsToMatch("/api/admin/**", "/api/orders/*/invoice", "/api/orders/*/transitions")
            .pathsToExclude("/api/webhooks/**")
            .addOperationCustomizer(globalHeaderCustomizer())
            .build();
    }

    @Bean
    public GroupedOpenApi partnerGroup() {
        return GroupedOpenApi.builder()
            .group("partner")
            .displayName("4. Partner API (Shipping Webhooks)")
            .pathsToMatch("/api/webhooks/**", "/api/partner/**")
            .addOperationCustomizer(globalHeaderCustomizer())
            .build();
    }

    // D2 & Metadata
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TechShop Order & Catalog API")
                .version(version)
                .description("Hệ thống REST API quản lý Sản phẩm và Dịch vụ Đặt hàng cho TechShop theo chuẩn OpenAPI 3.1 & Spring Boot 4.")
                .contact(new Contact().name("TechShop Backend Team").email("support@techshop.vn"))
                .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server().url(serverUrl).description("Môi trường hiện tại"),
                new Server().url("https://api.techshop.vn").description("Production Gateway")
            ))
            .externalDocs(new ExternalDocumentation()
                .description("Tài liệu hướng dẫn tích hợp chi tiết")
                .url("https://example.com/docs/techshop-api"))
            .components(new Components()
                // D2: Bearer JWT security scheme
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .name("bearerAuth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Nhập Bearer Token (Ví dụ: dev-admin-token hoặc dev-customer-token)"))
                // D2: Partner API Key header scheme
                .addSecuritySchemes("partnerKey", new SecurityScheme()
                    .name("X-Partner-Key")
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .description("API Key dành cho đối tác vận chuyển (Mặc định dev: partner-secret-key-2026)"))
            );
    }

    // D5: Customizer thêm header Idempotency-Key và X-Request-Id tự động
    @Bean
    public OperationCustomizer globalHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            // Thêm X-Request-Id vào mọi response
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((code, response) -> {
                    response.addHeaderObject("X-Request-Id", new Header()
                        .description("Mã theo dõi duy nhất cho mỗi yêu cầu HTTP (UUID)")
                        .schema(new StringSchema().example("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")));
                });
            }

            // Thêm Idempotency-Key cho POST /api/orders
            String path = handlerMethod.getMethod().getName();
            if (path.toLowerCase().contains("createorder") || path.toLowerCase().contains("create")) {
                boolean hasIdemp = operation.getParameters() != null && operation.getParameters().stream()
                    .anyMatch(p -> "Idempotency-Key".equalsIgnoreCase(p.getName()));
                if (!hasIdemp) {
                    operation.addParametersItem(new Parameter()
                        .in("header")
                        .name("Idempotency-Key")
                        .required(true)
                        .description("Khóa chống trùng lặp tạo đơn hàng (Idempotency Key)")
                        .schema(new StringSchema().example("REQ-" + System.currentTimeMillis())));
                }
            }

            return operation;
        };
    }
}
