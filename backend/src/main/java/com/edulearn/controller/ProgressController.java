package com.edulearn.controller;

import com.edulearn.dto.request.CompleteLessonRequest;
import com.edulearn.dto.request.VideoPositionRequest;
import com.edulearn.dto.response.ApiResponse;
import com.edulearn.entity.Course;
import com.edulearn.entity.Progress;
import com.edulearn.security.UserPrincipal;
import com.edulearn.service.CourseService;
import com.edulearn.service.ProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {
    
    private final ProgressService progressService;
    private final CourseService courseService;
    
    @GetMapping({"/courses", "/my-learning"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyCourses(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        List<Progress> progressList = progressService.getUserProgress(currentUser.getId());
        
        List<Map<String, Object>> courses = progressList.stream()
                .map(progress -> {
                    Course course = courseService.getCourseById(progress.getCourseId());
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", course.getId());
                    result.put("title", course.getTitle());
                    result.put("slug", course.getSlug());
                    result.put("thumbnail", course.getThumbnail());
                    result.put("category", course.getCategory());
                    result.put("instructor", course.getInstructor());
                    result.put("progress", progress.getProgressPercent());
                    result.put("currentLessonId", progress.getCurrentLessonId());
                    result.put("completedAt", progress.getCompletedAt());
                    return result;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(courses));
    }
    
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<Progress>> getCourseProgress(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String courseId) {
        
        Progress progress = progressService.getProgress(currentUser.getId(), courseId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }
    
    @PutMapping("/{courseId}/lesson/{lessonId}")
    public ResponseEntity<ApiResponse<Progress>> markLessonComplete(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String courseId,
            @PathVariable String lessonId) {
        
        Progress progress = progressService.markLessonComplete(
                currentUser.getId(), courseId, lessonId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Progress>> markLessonCompleteCompat(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CompleteLessonRequest request) {

        Progress progress = progressService.markLessonComplete(
                currentUser.getId(), request.getCourseId(), request.getLessonId());
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    @GetMapping("/position/{lessonId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWatchPosition(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String lessonId) {

        int position = progressService.getVideoPositionByLesson(currentUser.getId(), lessonId);

        Map<String, Object> response = new HashMap<>();
        response.put("lessonId", lessonId);
        response.put("position", position);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/position/{lessonId}")
    public ResponseEntity<ApiResponse<Progress>> saveWatchPosition(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String lessonId,
            @Valid @RequestBody VideoPositionRequest request) {

        Progress progress = progressService.updateVideoPositionByLesson(
                currentUser.getId(),
                lessonId,
                request.getPosition()
        );

        return ResponseEntity.ok(ApiResponse.success(progress));
    }
    
    @PutMapping("/{courseId}/video-position")
    public ResponseEntity<ApiResponse<Progress>> updateVideoPosition(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable String courseId,
            @RequestParam String lessonId,
            @RequestParam int position) {
        
        Progress progress = progressService.updateVideoPosition(
                currentUser.getId(), courseId, lessonId, position);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }
}
