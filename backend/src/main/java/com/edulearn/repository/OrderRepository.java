package com.edulearn.repository;

import com.edulearn.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<Order> findByStatusOrderByCreatedAtDesc(Order.OrderStatus status, Pageable pageable);
    
    long countByStatus(Order.OrderStatus status);
}
