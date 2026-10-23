package com.sem4.openapispringboot.service;

import com.sem4.openapispringboot.dto.order.DailyRevenueReportResponse;
import com.sem4.openapispringboot.entity.Order;
import com.sem4.openapispringboot.entity.OrderStatus;
import com.sem4.openapispringboot.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orderRepository;

    public ReportService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<DailyRevenueReportResponse> getDailyRevenue(LocalDate from, LocalDate to) {
        LocalDate startDate = from != null ? from : LocalDate.now().minusDays(14);
        LocalDate endDate = to != null ? to : LocalDate.now();

        Instant fromInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant toInstant = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        List<Order> orders = orderRepository.findAll().stream()
            .filter(o -> !o.getCreatedAt().isBefore(fromInstant) && o.getCreatedAt().isBefore(toInstant))
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.RETURNED)
            .toList();

        Map<LocalDate, List<Order>> ordersByDate = orders.stream()
            .collect(Collectors.groupingBy(o -> o.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate()));

        List<DailyRevenueReportResponse> reports = new ArrayList<>();
        LocalDate curr = startDate;
        while (!curr.isAfter(endDate)) {
            List<Order> dayOrders = ordersByDate.getOrDefault(curr, Collections.emptyList());
            BigDecimal dayRevenue = dayOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            reports.add(new DailyRevenueReportResponse(
                curr,
                (long) dayOrders.size(),
                dayRevenue,
                "VND"
            ));
            curr = curr.plusDays(1);
        }

        return reports;
    }
}
