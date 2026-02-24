package com.edulearn.repository;

import com.edulearn.entity.Progress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgressRepository extends MongoRepository<Progress, String> {
    
    Optional<Progress> findByUserIdAndCourseId(String userId, String courseId);

    Optional<Progress> findByUserIdAndCurrentLessonId(String userId, String currentLessonId);

    List<Progress> findByUserIdAndCompletedLessonIdsContaining(String userId, String lessonId);
    
    List<Progress> findByUserId(String userId);
    
    boolean existsByUserIdAndCourseId(String userId, String courseId);
}
