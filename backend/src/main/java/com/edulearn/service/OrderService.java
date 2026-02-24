package com.edulearn.service;

import com.edulearn.dto.request.CheckoutRequest;
import com.edulearn.entity.Course;
import com.edulearn.entity.Order;
import com.edulearn.entity.User;
import com.edulearn.exception.BadRequestException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.CourseRepository;
import com.edulearn.repository.OrderRepository;
import com.edulearn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private static final String SUPPORTED_PAYMENT_METHODS = "CARD, MOMO, BANK_TRANSFER";

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final ProgressService progressService;
    
    @Transactional
    public Order checkout(String userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        List<String> courseIds = request.getCourseIds().stream()
                .map(String::trim)
                .filter(courseId -> !courseId.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            throw new BadRequestException("Course IDs cannot be empty");
        }
        
        List<Course> courses = courseService.getPublishedCoursesByIds(courseIds);
        Map<String, Course> courseById = courses.stream()
                .collect(Collectors.toMap(Course::getId, course -> course));

        List<String> missingCourseIds = courseIds.stream()
                .filter(courseId -> !courseById.containsKey(courseId))
                .toList();

        if (!missingCourseIds.isEmpty()) {
            throw new BadRequestException(
                    "Some courses are unavailable or not published: " + String.join(", ", missingCourseIds)
            );
        }

        List<String> enrolledCourseIds = user.getEnrolledCourseIds() != null
                ? user.getEnrolledCourseIds()
                : List.of();

        List<String> alreadyEnrolledCourseIds = courseIds.stream()
                .filter(enrolledCourseIds::contains)
                .toList();

        if (!alreadyEnrolledCourseIds.isEmpty()) {
            throw new BadRequestException("You are already enrolled in: " + String.join(", ", alreadyEnrolledCourseIds));
        }
        
        List<Order.OrderItem> orderItems = courseIds.stream()
                .map(courseId -> {
                    Course course = courseById.get(courseId);
                    return Order.OrderItem.builder()
                            .courseId(courseId)
                            .courseTitle(course.getTitle())
                            .courseThumbnail(course.getThumbnail())
                            .price(course.getPrice() != null ? course.getPrice() : 0L)
                            .build();
                })
                .collect(Collectors.toList());

        long totalAmount = orderItems.stream()
                .mapToLong(Order.OrderItem::getPrice)
                .sum();

        Order order = Order.builder()
                .userId(userId)
                .userEmail(user.getEmail())
                .userName(user.getFullName())
                .items(orderItems)
                .totalAmount(totalAmount)
                .paymentMethod(parsePaymentMethod(request.getPaymentMethod()))
                .status(Order.OrderStatus.PENDING)
                .build();

        return orderRepository.save(order);
    }
    
    public List<Order> getUserOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order getUserOrderById(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access this order");
        }

        return order;
    }
    
    public Page<Order> getAllOrders(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Order.OrderStatus statusFilter = parseOrderStatusOrNull(status);
        if (statusFilter != null) {
            return orderRepository.findByStatusOrderByCreatedAtDesc(statusFilter, pageable);
        }
        
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    public Order updateOrderStatus(String orderId, String status) {
        return updateOrderStatus(orderId, parseOrderStatus(status));
    }

    @Transactional
    public Order updateOrderStatus(String orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == status) {
            return order;
        }

        if (status == Order.OrderStatus.COMPLETED && order.getStatus() != Order.OrderStatus.COMPLETED) {
            grantCourseAccess(order);
        }

        order.setStatus(status);
        return orderRepository.save(order);
    }
    
    public long getTotalRevenue() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == Order.OrderStatus.COMPLETED)
                .mapToLong(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0L)
                .sum();
    }
    
    public long countCompletedOrders() {
        return orderRepository.countByStatus(Order.OrderStatus.COMPLETED);
    }

    private void grantCourseAccess(Order order) {
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Set<String> enrolledCourseIds = new LinkedHashSet<>(
                user.getEnrolledCourseIds() != null ? user.getEnrolledCourseIds() : List.of()
        );
        List<String> newlyEnrolledCourseIds = new ArrayList<>();

        for (Order.OrderItem item : order.getItems()) {
            String courseId = item.getCourseId();
            if (courseId != null && enrolledCourseIds.add(courseId)) {
                newlyEnrolledCourseIds.add(courseId);
            }
        }

        if (newlyEnrolledCourseIds.isEmpty()) {
            return;
        }

        List<Course> courses = courseService.getCoursesByIds(newlyEnrolledCourseIds);
        Map<String, Course> courseById = courses.stream()
                .collect(Collectors.toMap(Course::getId, course -> course));

        List<String> missingCourseIds = newlyEnrolledCourseIds.stream()
                .filter(courseId -> !courseById.containsKey(courseId))
                .toList();

        if (!missingCourseIds.isEmpty()) {
            throw new BadRequestException(
                    "Cannot complete order because some courses no longer exist: "
                            + String.join(", ", missingCourseIds)
            );
        }

        courses.forEach(course -> course.setStudentsCount((course.getStudentsCount() != null
                ? course.getStudentsCount()
                : 0) + 1));
        courseRepository.saveAll(courses);

        user.setEnrolledCourseIds(new ArrayList<>(enrolledCourseIds));
        userRepository.save(user);

        for (String courseId : newlyEnrolledCourseIds) {
            progressService.createProgress(user.getId(), courseId);
        }
    }

    private Order.OrderStatus parseOrderStatusOrNull(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        return parseOrderStatus(status);
    }

    private Order.OrderStatus parseOrderStatus(String status) {
        try {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            return Order.OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid order status: " + status);
        }
    }

    private Order.PaymentMethod parsePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new BadRequestException("Payment method is required");
        }

        try {
            String normalized = paymentMethod.trim()
                    .toUpperCase(Locale.ROOT)
                    .replace('-', '_')
                    .replace(' ', '_');
            return Order.PaymentMethod.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Unsupported payment method. Supported values: " + SUPPORTED_PAYMENT_METHODS
            );
        }
    }
}
