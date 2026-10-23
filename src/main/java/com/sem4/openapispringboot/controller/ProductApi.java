package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.common.ApiProblem;
import com.sem4.openapispringboot.dto.product.ProductRequest;
import com.sem4.openapispringboot.dto.product.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Products", description = "Quản lý danh mục sản phẩm TechShop")
@RequestMapping("/api/v1/products")
public interface ProductApi {

    @Operation(summary = "Lấy danh sách tất cả sản phẩm")
    @ApiResponse(responseCode = "200", description = "Danh sách sản phẩm (có thể rỗng)")
    @GetMapping
    List<ProductResponse> findAll();

    @Operation(summary = "Lấy thông tin chi tiết một sản phẩm theo ID")
    @ApiResponse(responseCode = "200", description = "Tìm thấy thông tin sản phẩm")
    @ApiResponse(
        responseCode = "404",
        description = "Sản phẩm không tồn tại",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    )
    @GetMapping("/{id}")
    ProductResponse findById(
        @Parameter(description = "ID sản phẩm cần truy vấn", example = "1")
        @PathVariable Long id
    );

    @Operation(summary = "Tạo mới sản phẩm")
    @ApiResponse(
        responseCode = "201",
        description = "Sản phẩm đã được tạo thành công",
        headers = @Header(name = "Location", description = "URI của sản phẩm vừa tạo")
    )
    @ApiResponse(
        responseCode = "400",
        description = "Dữ liệu yêu cầu không hợp lệ",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    )
    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request);

    @Operation(summary = "Cập nhật toàn bộ thông tin sản phẩm")
    @ApiResponse(responseCode = "200", description = "Đã cập nhật sản phẩm thành công")
    @ApiResponse(
        responseCode = "400",
        description = "Dữ liệu cập nhật không hợp lệ",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Sản phẩm cần cập nhật không tồn tại",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    )
    @PutMapping("/{id}")
    ProductResponse update(
        @Parameter(description = "ID sản phẩm cần cập nhật", example = "1")
        @PathVariable Long id,
        @Valid @RequestBody ProductRequest request
    );

    @Operation(summary = "Xóa sản phẩm theo ID")
    @ApiResponse(responseCode = "204", description = "Đã xóa sản phẩm thành công")
    @ApiResponse(
        responseCode = "404",
        description = "Sản phẩm cần xóa không tồn tại",
        content = @Content(
            mediaType = "application/problem+json",
            schema = @Schema(implementation = ProblemDetail.class)
        )
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
        @Parameter(description = "ID sản phẩm cần xóa", example = "1")
        @PathVariable Long id
    );
}
