package com.edulearn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CourseRequest {
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    private String thumbnail;
    
    private String category;
    
    private String level = "beginner";
    
    private String instructor;
    
    @NotNull(message = "Price is required")
    private Long price = 0L;
    
    private Long originalPrice;
    
    private Boolean isPublished = false;
    
    private List<SectionRequest> sections;
    
    @Data
    public static class SectionRequest {
        private String title;
        private List<LessonRequest> lessons;
    }
    
    @Data
    public static class LessonRequest {
        private String title;
        private String videoUrl;
        private Integer duration = 0;
        private Boolean isPreview = false;
    }
}
