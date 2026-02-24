package com.edulearn.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "orders")
public class Order {
    
    @Id
    private String id;
    
    private String userId;
    
    private String userEmail;
    
    private String userName;
    
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
    
    @Builder.Default
    private Long totalAmount = 0L;
    
    private PaymentMethod paymentMethod;
    
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    public enum OrderStatus {
        PENDING, COMPLETED, REFUNDED, CANCELLED
    }

    public enum PaymentMethod {
        CARD, MOMO, BANK_TRANSFER
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItem {
        private String courseId;
        private String courseTitle;
        private String courseThumbnail;
        private Long price;
    }
}
