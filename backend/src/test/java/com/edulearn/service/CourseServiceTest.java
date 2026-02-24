package com.edulearn.service;

import com.edulearn.entity.Course;
import com.edulearn.exception.BadRequestException;
import com.edulearn.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void getAllCoursesWithSearchAndEmptyStatusUsesSearchFilterWithoutStatusParsingError() {
        Page<Course> emptyPage = new PageImpl<>(List.of());
        when(courseRepository.findByTitleContainingIgnoreCase(eq("spring"), any(Pageable.class)))
                .thenReturn(emptyPage);

        courseService.getAllCourses("", "", "spring", 0, 10);

        verify(courseRepository).findByTitleContainingIgnoreCase(eq("spring"), any(Pageable.class));
    }

    @Test
    void getAllCoursesThrowsBadRequestWhenStatusInvalid() {
        assertThrows(BadRequestException.class,
                () -> courseService.getAllCourses("not-a-valid-status", "", "", 0, 10));
    }
}
