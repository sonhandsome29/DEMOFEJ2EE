package com.edulearn.service;

import com.edulearn.entity.Course;
import com.edulearn.entity.Progress;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.ProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProgressService {
    
    private final ProgressRepository progressRepository;
    private final CourseService courseService;
    
    public Progress createProgress(String userId, String courseId) {
        // Check if already exists
        if (progressRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return getProgress(userId, courseId);
        }
        
        Progress progress = Progress.builder()
                .userId(userId)
                .courseId(courseId)
                .progressPercent(0)
                .build();
        
        return progressRepository.save(progress);
    }
    
    public Progress getProgress(String userId, String courseId) {
        return progressRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Progress not found"));
    }
    
    public List<Progress> getUserProgress(String userId) {
        return progressRepository.findByUserId(userId);
    }
    
    public Progress markLessonComplete(String userId, String courseId, String lessonId) {
        Progress progress = getProgress(userId, courseId);
        
        // Add lesson to completed list if not already there
        if (!progress.getCompletedLessonIds().contains(lessonId)) {
            progress.getCompletedLessonIds().add(lessonId);
        }
        
        progress.setCurrentLessonId(lessonId);
        
        // Calculate progress percentage
        Course course = courseService.getCourseById(courseId);
        int totalLessons = course.getLessonsCount();
        int completedLessons = progress.getCompletedLessonIds().size();
        int progressPercent = totalLessons > 0 
                ? (completedLessons * 100) / totalLessons 
                : 0;
        
        progress.setProgressPercent(progressPercent);
        
        // Check if completed
        if (progressPercent == 100 && progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }
        
        return progressRepository.save(progress);
    }
    
    public Progress updateVideoPosition(String userId, String courseId, String lessonId, int position) {
        Progress progress = getProgress(userId, courseId);
        progress.setCurrentLessonId(lessonId);
        progress.setLastVideoPosition(position);
        return progressRepository.save(progress);
    }

    public int getVideoPositionByLesson(String userId, String lessonId) {
        return findProgressByLesson(userId, lessonId)
                .map(Progress::getLastVideoPosition)
                .orElse(0);
    }

    public Progress updateVideoPositionByLesson(String userId, String lessonId, int position) {
        Progress progress = findProgressByLesson(userId, lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Progress not found for lesson"));

        progress.setCurrentLessonId(lessonId);
        progress.setLastVideoPosition(position);
        return progressRepository.save(progress);
    }
    
    public boolean hasAccess(String userId, String courseId) {
        return progressRepository.existsByUserIdAndCourseId(userId, courseId);
    }

    private Optional<Progress> findProgressByLesson(String userId, String lessonId) {
        Optional<Progress> currentLessonProgress = progressRepository.findByUserIdAndCurrentLessonId(userId, lessonId);
        if (currentLessonProgress.isPresent()) {
            return currentLessonProgress;
        }

        List<Progress> completedLessonProgress = progressRepository
                .findByUserIdAndCompletedLessonIdsContaining(userId, lessonId);

        if (!completedLessonProgress.isEmpty()) {
            return Optional.of(completedLessonProgress.get(0));
        }

        return Optional.empty();
    }
}
