package com.edulearn.repository;

import com.edulearn.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {
    
    Optional<Course> findBySlug(String slug);

    Optional<Course> findBySlugAndStatus(String slug, Course.Status status);
    
    boolean existsBySlug(String slug);
    
    // Find all published courses
    Page<Course> findByStatus(Course.Status status, Pageable pageable);

    List<Course> findByStatus(Course.Status status, Sort sort);
    
    // Find by category
    Page<Course> findByStatusAndCategoryIgnoreCase(Course.Status status, String category, Pageable pageable);

    Page<Course> findByCategoryIgnoreCase(String category, Pageable pageable);
    
    // Search by title
    Page<Course> findByStatusAndTitleContainingIgnoreCase(Course.Status status, String title, Pageable pageable);

    Page<Course> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Course> findByStatusAndCategoryIgnoreCaseAndTitleContainingIgnoreCase(
            Course.Status status,
            String category,
            String title,
            Pageable pageable
    );

    Page<Course> findByCategoryIgnoreCaseAndTitleContainingIgnoreCase(String category, String title, Pageable pageable);
    
    // Find featured (top rated)
    List<Course> findTop6ByStatusOrderByRatingDesc(Course.Status status);
    
    // Find by IDs
    List<Course> findByIdIn(List<String> ids);

    List<Course> findByIdInAndStatus(List<String> ids, Course.Status status);
    
    // Count by status
    long countByStatus(Course.Status status);
}
