package com.edulearn.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "progress")
@CompoundIndex(name = "user_course_idx", def = "{'userId': 1, 'courseId': 1}", unique = true)
public class Progress {
    
    @Id
    private String id;
    
    private String userId;
    
    private String courseId;
    
    @Builder.Default
    private List<String> completedLessonIds = new ArrayList<>();
    
    private String currentLessonId;
    
    @Builder.Default
    private Integer lastVideoPosition = 0; // seconds
    
    @Builder.Default
    private Integer progressPercent = 0;
    
    private LocalDateTime completedAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
