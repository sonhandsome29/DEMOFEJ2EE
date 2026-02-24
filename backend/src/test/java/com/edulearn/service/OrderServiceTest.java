package com.edulearn.service;

import com.edulearn.dto.request.CheckoutRequest;
import com.edulearn.entity.Course;
import com.edulearn.entity.Order;
import com.edulearn.entity.User;
import com.edulearn.exception.BadRequestException;
import com.edulearn.repository.CourseRepository;
import com.edulearn.repository.OrderRepository;
import com.edulearn.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private ProgressService progressService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkoutRejectsRequestWhenAnyCourseIdIsUnknown() {
        User user = buildUser("u1");
        CheckoutRequest request = new CheckoutRequest();
        request.setCourseIds(List.of("c1", "c2"));
        request.setPaymentMethod("card");

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(courseService.getPublishedCoursesByIds(List.of("c1", "c2")))
                .thenReturn(List.of(buildCourse("c1", 100_000L, 5)));

        assertThrows(BadRequestException.class, () -> orderService.checkout("u1", request));
    }

    @Test
    void checkoutCreatesPendingOrderWithoutImmediateEnrollmentSideEffects() {
        User user = buildUser("u1");
        CheckoutRequest request = new CheckoutRequest();
        request.setCourseIds(List.of("c1", "c2"));
        request.setPaymentMethod("momo");

        List<Course> courses = List.of(
                buildCourse("c1", 100_000L, 10),
                buildCourse("c2", 200_000L, 20)
        );

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(courseService.getPublishedCoursesByIds(List.of("c1", "c2"))).thenReturn(courses);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId("o1");
            return order;
        });

        Order order = orderService.checkout("u1", request);

        assertEquals(Order.OrderStatus.PENDING, order.getStatus());
        assertEquals(Order.PaymentMethod.MOMO, order.getPaymentMethod());
        assertEquals(300_000L, order.getTotalAmount());
        verify(userRepository, never()).save(any(User.class));
        verify(progressService, never()).createProgress(any(), any());
        verify(courseRepository, never()).saveAll(anyList());
    }

    @Test
    void updateOrderStatusToCompletedEnrollsUserAndUpdatesCourseMetrics() {
        User user = buildUser("u1");
        Course course = buildCourse("c1", 150_000L, 4);

        Order order = Order.builder()
                .id("o1")
                .userId("u1")
                .status(Order.OrderStatus.PENDING)
                .items(List.of(Order.OrderItem.builder().courseId("c1").price(150_000L).build()))
                .build();

        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(courseService.getCoursesByIds(List.of("c1"))).thenReturn(List.of(course));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updated = orderService.updateOrderStatus("o1", "completed");

        assertEquals(Order.OrderStatus.COMPLETED, updated.getStatus());
        verify(progressService).createProgress("u1", "c1");
        verify(userRepository).save(any(User.class));
        verify(courseRepository).saveAll(anyList());
        assertEquals(5, course.getStudentsCount());
    }

    private User buildUser(String id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setFullName("Demo User");
        user.setEnrolledCourseIds(new ArrayList<>());
        return user;
    }

    private Course buildCourse(String id, long price, int studentsCount) {
        Course course = new Course();
        course.setId(id);
        course.setTitle("Course " + id);
        course.setThumbnail("thumb");
        course.setPrice(price);
        course.setStudentsCount(studentsCount);
        return course;
    }
}
