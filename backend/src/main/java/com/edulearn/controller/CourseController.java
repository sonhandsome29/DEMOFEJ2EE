package com.edulearn.controller;

import com.edulearn.dto.response.ApiResponse;
import com.edulearn.dto.response.CourseDetailResponse;
import com.edulearn.dto.response.CourseSummaryResponse;
import com.edulearn.entity.Course;
import com.edulearn.service.CourseService;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {
    
    private final CourseService courseService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourses(
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "newest") String sort) {
        
        Page<Course> coursePage = courseService.getPublishedCourses(category, search, page, size, sort);
        List<CourseSummaryResponse> courses = coursePage.getContent().stream()
                .map(CourseSummaryResponse::from)
                .toList();
        
        Map<String, Object> response = new HashMap<>();
        response.put("courses", courses);
        response.put("currentPage", coursePage.getNumber());
        response.put("totalPages", coursePage.getTotalPages());
        response.put("totalItems", coursePage.getTotalElements());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<CourseSummaryResponse>>> getFeaturedCourses() {
        List<CourseSummaryResponse> courses = courseService.getFeaturedCourses().stream()
                .map(CourseSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        List<String> categories = courseService.getPublishedCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
    
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> getCourseBySlug(@PathVariable String slug) {
        Course course = courseService.getCourseBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(CourseDetailResponse.fromPublicCourse(course)));
    }
    
    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable String id) {
        Course course = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.success(course));
    }
}
