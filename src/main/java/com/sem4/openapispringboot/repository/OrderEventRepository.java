package com.sem4.openapispringboot.repository;

import com.sem4.openapispringboot.entity.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    List<OrderEvent> findByOrderIdOrderByAtAsc(Long orderId);
    List<OrderEvent> findByOrderCodeOrderByAtAsc(String orderCode);
}
