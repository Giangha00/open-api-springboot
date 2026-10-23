package com.sem4.openapispringboot.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thông tin phản hồi đăng nhập / cấp phát JWT Token cho môi trường dev")
public record AuthTokenResponse(
    @Schema(description = "JWT Token mang quyền Bearer", example = "eyJhbGciOiJIUzI1NiJ9...")
    String accessToken,

    @Schema(description = "Loại token", example = "Bearer")
    String tokenType,

    @Schema(description = "Vai trò người dùng (ROLE_CUSTOMER hoặc ROLE_ADMIN)", example = "ROLE_CUSTOMER")
    String role,

    @Schema(description = "ID khách hàng gắn với token (nếu là khách hàng)", example = "1")
    Long customerId,

    @Schema(description = "Tên người dùng / email", example = "customer@techshop.vn")
    String email,

    @Schema(description = "Thời gian hết hạn tính bằng giây", example = "86400")
    long expiresIn
) {
}
