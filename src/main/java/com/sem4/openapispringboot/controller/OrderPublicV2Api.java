package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.common.ApiProblem;
import com.sem4.openapispringboot.common.PageResponse;
import com.sem4.openapispringboot.dto.order.*;
import com.sem4.openapispringboot.entity.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Orders (Public)", description = "API quản lý Đơn hàng cho Khách hàng TechShop (v2)")
@RequestMapping("/api")
public interface OrderPublicV2Api {

    @Operation(
        summary = "Tạo đơn hàng mới",
        description = "Tạo đơn hàng với cơ chế chống trùng lặp qua header Idempotency-Key. Tính tổng tiền từ giá server và lưu phương thức thanh toán đa hình.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
        responseCode = "201",
        description = "Đơn hàng đã được tạo thành công",
        headers = @Header(name = "Location", description = "URI của đơn hàng vừa tạo")
    )
    @ApiResponse(
        responseCode = "400",
        description = "Dữ liệu không hợp lệ (số lượng vượt quá, sản phẩm trùng lặp)",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "Xung đột Idempotency (Cùng Idempotency-Key nhưng payload khác biệt)",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @PostMapping("/orders")
    ResponseEntity<OrderResponseV2> createOrder(
        @Parameter(description = "Khóa chống trùng lặp yêu cầu tạo đơn", required = true, example = "REQ-1729683900000")
        @RequestHeader("Idempotency-Key") String idempotencyKey,

        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Thông tin đơn hàng (Chọn mẫu request theo phương thức thanh toán)",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateOrderRequest.class),
                examples = {
                    @ExampleObject(
                        name = "Đơn thanh toán khi nhận hàng (COD)",
                        summary = "Phương thức COD không cần thông tin thẻ/ngân hàng",
                        value = """
                        {
                          "shippingAddress": "Số 8 Tôn Thất Thuyết, Cầu Giấy, Hà Nội",
                          "items": [
                            { "productId": 1, "quantity": 1 },
                            { "productId": 2, "quantity": 2 }
                          ],
                          "payment": {
                            "method": "COD"
                          }
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Đơn thanh toán thẻ tín dụng (CARD)",
                        summary = "Thanh toán thẻ với provider và last4",
                        value = """
                        {
                          "shippingAddress": "Tòa nhà Keangnam Landmark 72, Nam Từ Liêm, Hà Nội",
                          "items": [
                            { "productId": 3, "quantity": 1 }
                          ],
                          "payment": {
                            "method": "CARD",
                            "provider": "VISA",
                            "last4": "4242"
                          }
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Đơn chuyển khoản ngân hàng (BANK_TRANSFER)",
                        summary = "Thanh toán chuyển khoản với bankCode và mã giao dịch",
                        value = """
                        {
                          "shippingAddress": "Phố Huế, Hoàn Kiếm, Hà Nội",
                          "items": [
                            { "productId": 4, "quantity": 1 }
                          ],
                          "payment": {
                            "method": "BANK_TRANSFER",
                            "bankCode": "VCB",
                            "transferRef": "TXN-2026-102399"
                          }
                        }
                        """
                    )
                }
            )
        )
        @org.springframework.web.bind.annotation.RequestBody @Valid CreateOrderRequest request
    );

    @Operation(
        summary = "Lấy danh sách đơn hàng có phân trang và bộ lọc",
        description = "Khách hàng chỉ xem được danh sách đơn hàng của mình. Cho phép lọc theo trạng thái, khoảng ngày, giá trị tối thiểu và sắp xếp.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Danh sách đơn hàng phân trang theo cấu trúc ổn định")
    @GetMapping("/orders")
    PageResponse<OrderResponseV2> getOrders(
        @Parameter(description = "Lọc theo trạng thái đơn hàng")
        @RequestParam(required = false) OrderStatus status,

        @Parameter(description = "Từ ngày (YYYY-MM-DD)", example = "2026-10-01")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

        @Parameter(description = "Đến ngày (YYYY-MM-DD)", example = "2026-10-31")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

        @Parameter(description = "Giá trị đơn hàng tối thiểu (VND)", example = "1000000")
        @RequestParam(required = false) BigDecimal minTotal,

        @Parameter(description = "Số trang (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "Số phần tử mỗi trang", example = "10")
        @RequestParam(defaultValue = "10") int size,

        @Parameter(
            description = "Trường sắp xếp và thứ tự",
            example = "createdAt,desc",
            schema = @Schema(allowableValues = {"createdAt,desc", "createdAt,asc", "totalAmount,desc", "totalAmount,asc"})
        )
        @RequestParam(defaultValue = "createdAt,desc") String sort
    );

    @Operation(
        summary = "Lấy chi tiết đơn hàng (v2 - Chuẩn hóa)",
        description = "Trả về thông tin chi tiết đơn hàng phiên bản v2 với tổng tiền dạng {value, currency} và header ETag.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
        responseCode = "200",
        description = "Tìm thấy thông tin đơn hàng v2",
        headers = @Header(name = "ETag", description = "Phiên bản hiện tại của đơn hàng (dùng cho If-Match)")
    )
    @ApiResponse(
        responseCode = "404",
        description = "Không tìm thấy mã đơn hàng",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @GetMapping("/v2/orders/{code}")
    ResponseEntity<OrderResponseV2> getOrderByCodeV2(
        @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000123")
        @PathVariable String code
    );

    @Operation(
        summary = "Chuyển trạng thái đơn hàng (Khách hàng)",
        description = "Khách hàng thực hiện các thao tác: CANCEL (chỉ khi đơn còn PENDING), REQUEST_RETURN (chỉ khi đơn DELIVERED trong vòng 7 ngày). Bắt buộc truyền header If-Match.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
        responseCode = "200",
        description = "Chuyển trạng thái thành công",
        headers = @Header(name = "ETag", description = "Phiên bản mới của đơn hàng")
    )
    @ApiResponse(
        responseCode = "409",
        description = "Chuyển trạng thái không hợp lệ (ORDER_INVALID_TRANSITION)",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @ApiResponse(
        responseCode = "412",
        description = "Phiên bản If-Match không khớp (PRECONDITION_FAILED)",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @PostMapping("/orders/{code}/transitions")
    ResponseEntity<OrderTransitionResponse> transitionOrder(
        @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000123")
        @PathVariable String code,

        @Parameter(description = "Phiên bản hiện tại của đơn hàng (bắt buộc)", required = true, example = "\"0\"")
        @RequestHeader("If-Match") String ifMatch,

        @Valid @RequestBody OrderTransitionRequest request
    );

    @Operation(
        summary = "Tải hóa đơn PDF của đơn hàng",
        description = "Khách hàng tải hóa đơn điện tử PDF của đơn hàng (nếu quản trị viên đã xuất hóa đơn).",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
        responseCode = "200",
        description = "Trả về file hóa đơn PDF",
        content = @Content(mediaType = "application/pdf")
    )
    @ApiResponse(
        responseCode = "404",
        description = "Chưa có hóa đơn hoặc đơn hàng không tồn tại",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblem.class))
    )
    @GetMapping(value = "/orders/{code}/invoice", produces = "application/pdf")
    ResponseEntity<Resource> getInvoice(
        @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000123")
        @PathVariable String code
    );

    @Operation(
        summary = "Xem lịch sử sự kiện chuyển trạng thái đơn hàng",
        description = "Xem nhật ký chi tiết các lần chuyển trạng thái, thời gian và lý do.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(responseCode = "200", description = "Danh sách sự kiện chuyển trạng thái")
    @GetMapping("/orders/{code}/events")
    List<OrderEventResponse> getOrderEvents(
        @Parameter(description = "Mã đơn hàng", example = "ORD-2026-000123")
        @PathVariable String code
    );
}
