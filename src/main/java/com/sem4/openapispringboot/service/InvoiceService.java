package com.sem4.openapispringboot.service;

import com.sem4.openapispringboot.entity.Order;
import com.sem4.openapispringboot.exception.BusinessValidationException;
import com.sem4.openapispringboot.exception.OrderNotFoundException;
import com.sem4.openapispringboot.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Transactional(readOnly = true)
public class InvoiceService {

    private final OrderRepository orderRepository;

    public InvoiceService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public void uploadInvoice(String code, MultipartFile file) {
        Order order = orderRepository.findByCode(code)
            .orElseThrow(() -> new OrderNotFoundException(code));

        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("EMPTY_FILE", "File hóa đơn không được để trống.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new BusinessValidationException("INVALID_FILE_TYPE", "Chỉ chấp nhận file định dạng PDF.");
        }

        long maxSize = 5L * 1024 * 1024; // 5 MB
        if (file.getSize() > maxSize) {
            throw new BusinessValidationException("FILE_TOO_LARGE", "Kích thước file hóa đơn không được vượt quá 5 MB.");
        }

        try {
            order.setInvoiceFileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : (code + "-invoice.pdf"));
            order.setInvoiceContentType("application/pdf");
            order.setInvoiceData(file.getBytes());
            orderRepository.save(order);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi đọc dữ liệu file tải lên: " + e.getMessage(), e);
        }
    }

    public record InvoiceDownload(byte[] data, String fileName) {}

    public InvoiceDownload getInvoice(String code, Long customerId, boolean isAdmin) {
        Order order = orderRepository.findByCode(code)
            .orElseThrow(() -> new OrderNotFoundException(code));

        if (!isAdmin && customerId != null && !order.getCustomerId().equals(customerId)) {
            throw new SecurityException("Bạn không có quyền truy cập hóa đơn của đơn hàng này.");
        }

        if (order.getInvoiceData() == null || order.getInvoiceData().length == 0) {
            throw new BusinessValidationException("INVOICE_NOT_FOUND", "Đơn hàng " + code + " hiện chưa có hóa đơn PDF.");
        }

        return new InvoiceDownload(
            order.getInvoiceData(),
            order.getInvoiceFileName() != null ? order.getInvoiceFileName() : (code + "-invoice.pdf")
        );
    }
}
