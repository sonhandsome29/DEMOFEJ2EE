package com.edulearn.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "courses")
public class Course {
    
    @Id
    private String id;
    
    private String title;
    
    @Indexed(unique = true)
    private String slug;
    
    private String description;
    
    private String thumbnail;
    
    private String category;
    
    @Builder.Default
    private String level = "beginner"; // beginner, intermediate, advanced
    
    private String instructor;
    
    @Builder.Default
    private Long price = 0L; // VND
    
    private Long originalPrice;
    
    @Builder.Default
    private Integer duration = 0; // total minutes
    
    @Builder.Default
    private Integer studentsCount = 0;
    
    @Builder.Default
    private Double rating = 0.0;
    
    @Builder.Default
    private Integer reviewsCount = 0;
    
    @Builder.Default
    private Status status = Status.DRAFT;
    
    @Builder.Default
    private List<Section> sections = new ArrayList<>();
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum Status {
        DRAFT, PUBLISHED
    }
    
    // Embedded Section class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Section {
        private String id;
        private String title;
        @Builder.Default
        private List<Lesson> lessons = new ArrayList<>();
    }
    
    // Embedded Lesson class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Lesson {
        private String id;
        private String title;
        private String videoUrl;
        @Builder.Default
        private Integer duration = 0; // minutes
        @Builder.Default
        private Boolean isPreview = false;
    }
    
    // Helper method to count total lessons
    public int getLessonsCount() {
        return sections.stream()
                .mapToInt(section -> section.getLessons().size())
                .sum();
    }
}
