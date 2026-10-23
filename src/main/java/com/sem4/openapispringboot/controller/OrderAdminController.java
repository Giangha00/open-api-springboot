package com.sem4.openapispringboot.controller;

import com.sem4.openapispringboot.config.AuthContext;
import com.sem4.openapispringboot.dto.order.DailyRevenueReportResponse;
import com.sem4.openapispringboot.dto.order.OrderTransitionRequest;
import com.sem4.openapispringboot.dto.order.OrderTransitionResponse;
import com.sem4.openapispringboot.service.InvoiceService;
import com.sem4.openapispringboot.service.OrderService;
import com.sem4.openapispringboot.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class OrderAdminController implements OrderAdminApi {

    private final OrderService orderService;
    private final InvoiceService invoiceService;
    private final ReportService reportService;

    public OrderAdminController(OrderService orderService, InvoiceService invoiceService, ReportService reportService) {
        this.orderService = orderService;
        this.invoiceService = invoiceService;
        this.reportService = reportService;
    }

    @Override
    public ResponseEntity<OrderTransitionResponse> adminTransitionOrder(String code, String ifMatch, OrderTransitionRequest request) {
        AuthContext.UserPrincipal user = AuthContext.get();
        // ADMIN can perform transition
        OrderTransitionResponse response = orderService.transitionOrder(code, request, ifMatch, null, "ROLE_ADMIN");
        return ResponseEntity.ok()
            .eTag("\"" + response.version() + "\"")
            .body(response);
    }

    @Override
    public ResponseEntity<Map<String, String>> uploadInvoice(String code, MultipartFile file) {
        invoiceService.uploadInvoice(code, file);
        return ResponseEntity.ok(Map.of(
            "code", code,
            "message", "Hóa đơn PDF đã được tải lên thành công cho đơn hàng " + code
        ));
    }

    @Override
    public List<DailyRevenueReportResponse> getDailyRevenueReport(LocalDate from, LocalDate to) {
        return reportService.getDailyRevenue(from, to);
    }
}
