package com.edulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteLessonRequest {

    @NotBlank(message = "Course ID is required")
    private String courseId;

    @NotBlank(message = "Lesson ID is required")
    private String lessonId;
}
