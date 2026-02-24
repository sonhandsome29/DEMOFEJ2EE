package com.edulearn.controller;

import com.edulearn.dto.request.CourseRequest;
import com.edulearn.dto.request.UpdateOrderStatusRequest;
import com.edulearn.dto.response.ApiResponse;
import com.edulearn.dto.response.AdminUserResponse;
import com.edulearn.entity.Course;
import com.edulearn.entity.Order;
import com.edulearn.entity.User;
import com.edulearn.service.CourseService;
import com.edulearn.service.OrderService;
import com.edulearn.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final CourseService courseService;
    private final OrderService orderService;
    private final UserService userService;
    
    // ===== DASHBOARD =====
    
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRevenue", orderService.getTotalRevenue());
        stats.put("totalStudents", userService.countUsers());
        stats.put("totalCourses", courseService.countPublishedCourses());
        stats.put("totalOrders", orderService.countCompletedOrders());
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    // ===== COURSES =====
    
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourses(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        Page<Course> coursePage = courseService.getAllCourses(status, category, search, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("courses", coursePage.getContent());
        response.put("currentPage", coursePage.getNumber());
        response.put("totalPages", coursePage.getTotalPages());
        response.put("totalItems", coursePage.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<Course>> createCourse(
            @Valid @RequestBody CourseRequest request) {
        Course course = courseService.createCourse(request);
        return ResponseEntity.ok(ApiResponse.success("Course created successfully", course));
    }
    
    @PutMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable String id,
            @Valid @RequestBody CourseRequest request) {
        Course course = courseService.updateCourse(id, request);
        return ResponseEntity.ok(ApiResponse.success("Course updated successfully", course));
    }
    
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully", null));
    }
    
    // ===== ORDERS =====
    
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOrders(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        Page<Order> orderPage = orderService.getAllOrders(status, page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("orders", orderPage.getContent());
        response.put("currentPage", orderPage.getNumber());
        response.put("totalPages", orderPage.getTotalPages());
        response.put("totalItems", orderPage.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Order order = orderService.updateOrderStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatusLegacy(
            @PathVariable String id,
            @RequestParam String status) {
        Order order = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", order));
    }
    
    // ===== USERS =====
    
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        
        Page<User> userPage = userService.getAllUsers(page, size);
        List<AdminUserResponse> users = userPage.getContent().stream()
                .map(AdminUserResponse::from)
                .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("users", users);
        response.put("currentPage", userPage.getNumber());
        response.put("totalPages", userPage.getTotalPages());
        response.put("totalItems", userPage.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
