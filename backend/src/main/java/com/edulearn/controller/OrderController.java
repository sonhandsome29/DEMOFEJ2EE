package com.edulearn.controller;

import com.edulearn.dto.request.CheckoutRequest;
import com.edulearn.dto.response.ApiResponse;
import com.edulearn.entity.Order;
import com.edulearn.security.UserPrincipal;
import com.edulearn.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Order>> checkout(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CheckoutRequest request) {
        Order order = orderService.checkout(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", order));
    }
    
    @GetMapping({"", "/me"})
    public ResponseEntity<ApiResponse<List<Order>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<Order> orders = orderService.getUserOrders(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String orderId) {
        Order order = orderService.getUserOrderById(currentUser.getId(), orderId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
