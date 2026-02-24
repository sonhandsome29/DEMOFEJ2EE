package com.edulearn.service;

import com.edulearn.dto.request.CourseRequest;
import com.edulearn.entity.Course;
import com.edulearn.exception.BadRequestException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.CourseRepository;
import com.edulearn.validation.InputSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    
    private final CourseRepository courseRepository;
    private final InputSanitizer inputSanitizer;
    
    // ===== PUBLIC METHODS =====
    
    public Page<Course> getPublishedCourses(String category, String search, int page, int size, String sortBy) {
        Sort sort = resolveSort(sortBy);
        
        Pageable pageable = PageRequest.of(page, size, sort);
        String searchFilter = normalize(search);
        String categoryFilter = normalize(category);
        boolean hasSearch = searchFilter != null;
        boolean hasCategory = categoryFilter != null && !"all".equalsIgnoreCase(categoryFilter);
        
        if (hasCategory && hasSearch) {
            return courseRepository.findByStatusAndCategoryIgnoreCaseAndTitleContainingIgnoreCase(
                    Course.Status.PUBLISHED,
                    categoryFilter,
                    searchFilter,
                    pageable
            );
        }
        
        if (hasSearch) {
            return courseRepository.findByStatusAndTitleContainingIgnoreCase(
                    Course.Status.PUBLISHED,
                    searchFilter,
                    pageable
            );
        }
        
        if (hasCategory) {
            return courseRepository.findByStatusAndCategoryIgnoreCase(
                    Course.Status.PUBLISHED,
                    categoryFilter,
                    pageable
            );
        }
        
        return courseRepository.findByStatus(Course.Status.PUBLISHED, pageable);
    }

    public List<String> getPublishedCategories() {
        return courseRepository.findByStatus(Course.Status.PUBLISHED, Sort.by(Sort.Direction.ASC, "category"))
                .stream()
                .map(Course::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<Course> getFeaturedCourses() {
        return courseRepository.findTop6ByStatusOrderByRatingDesc(Course.Status.PUBLISHED);
    }
    
    public Course getCourseBySlug(String slug) {
        return courseRepository.findBySlugAndStatus(slug, Course.Status.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }
    
    public Course getCourseById(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }
    
    public List<Course> getCoursesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return courseRepository.findByIdIn(ids);
    }

    public List<Course> getPublishedCoursesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return courseRepository.findByIdInAndStatus(ids, Course.Status.PUBLISHED);
    }
    
    // ===== ADMIN METHODS =====
    
    public Page<Course> getAllCourses(String status, String category, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Course.Status statusFilter = parseStatusOrNull(status);
        String categoryFilter = normalize(category);
        String searchFilter = normalize(search);
        boolean hasCategory = categoryFilter != null;
        boolean hasSearch = searchFilter != null;
        
        if (statusFilter != null) {
            if (hasCategory && hasSearch) {
                return courseRepository.findByStatusAndCategoryIgnoreCaseAndTitleContainingIgnoreCase(
                        statusFilter,
                        categoryFilter,
                        searchFilter,
                        pageable
                );
            }

            if (hasCategory) {
                return courseRepository.findByStatusAndCategoryIgnoreCase(statusFilter, categoryFilter, pageable);
            }

            if (hasSearch) {
                return courseRepository.findByStatusAndTitleContainingIgnoreCase(statusFilter, searchFilter, pageable);
            }

            return courseRepository.findByStatus(statusFilter, pageable);
        }

        if (hasCategory && hasSearch) {
            return courseRepository.findByCategoryIgnoreCaseAndTitleContainingIgnoreCase(
                    categoryFilter,
                    searchFilter,
                    pageable
            );
        }

        if (hasCategory) {
            return courseRepository.findByCategoryIgnoreCase(categoryFilter, pageable);
        }

        if (hasSearch) {
            return courseRepository.findByTitleContainingIgnoreCase(searchFilter, pageable);
        }
        
        return courseRepository.findAll(pageable);
    }
    
    public Course createCourse(CourseRequest request) {
        String sanitizedTitle = requiredText(request.getTitle(), "Title is required");

        // Generate slug from title
        String slug = generateSlug(sanitizedTitle);
        
        Course course = Course.builder()
                .title(sanitizedTitle)
                .slug(slug)
                .description(optionalText(request.getDescription()))
                .thumbnail(inputSanitizer.sanitizeHttpUrl(request.getThumbnail(), "thumbnail"))
                .category(optionalText(request.getCategory()))
                .level(resolveLevel(request.getLevel()))
                .instructor(optionalText(request.getInstructor()))
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .status(Boolean.TRUE.equals(request.getIsPublished()) 
                        ? Course.Status.PUBLISHED : Course.Status.DRAFT)
                .sections(mapSections(request.getSections()))
                .build();
        
        // Calculate total duration
        course.setDuration(calculateTotalDuration(course.getSections()));
        
        return courseRepository.save(course);
    }
    
    public Course updateCourse(String id, CourseRequest request) {
        Course course = getCourseById(id);

        course.setTitle(requiredText(request.getTitle(), "Title is required"));
        course.setDescription(optionalText(request.getDescription()));
        course.setThumbnail(inputSanitizer.sanitizeHttpUrl(request.getThumbnail(), "thumbnail"));
        course.setCategory(optionalText(request.getCategory()));
        course.setLevel(resolveLevel(request.getLevel()));
        course.setInstructor(optionalText(request.getInstructor()));
        course.setPrice(request.getPrice());
        course.setOriginalPrice(request.getOriginalPrice());
        course.setStatus(Boolean.TRUE.equals(request.getIsPublished()) 
                ? Course.Status.PUBLISHED : Course.Status.DRAFT);
        
        if (request.getSections() != null) {
            course.setSections(mapSections(request.getSections()));
            course.setDuration(calculateTotalDuration(course.getSections()));
        }
        
        return courseRepository.save(course);
    }
    
    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
    }
    
    public long countPublishedCourses() {
        return courseRepository.countByStatus(Course.Status.PUBLISHED);
    }
    
    // ===== HELPER METHODS =====
    
    private String generateSlug(String title) {
        String baseSlug = title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
        
        // Add UUID suffix to ensure uniqueness
        String slug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 8);
        
        return slug;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Course.Status parseStatusOrNull(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return null;
        }

        try {
            return Course.Status.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid course status: " + status);
        }
    }

    private Sort resolveSort(String sortBy) {
        if ("price_asc".equals(sortBy)) {
            return Sort.by(Sort.Direction.ASC, "price");
        }

        if ("price_desc".equals(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "price");
        }

        if ("rating".equals(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "rating");
        }

        if ("popular".equals(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "studentsCount");
        }

        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
    
    private List<Course.Section> mapSections(List<CourseRequest.SectionRequest> sections) {
        if (sections == null) return List.of();
        
        return sections.stream()
                .map(s -> Course.Section.builder()
                        .id(UUID.randomUUID().toString())
                        .title(requiredText(s.getTitle(), "Section title is required"))
                        .lessons(mapLessons(s.getLessons()))
                        .build())
                .collect(Collectors.toList());
    }
    
    private List<Course.Lesson> mapLessons(List<CourseRequest.LessonRequest> lessons) {
        if (lessons == null) return List.of();
        
        return lessons.stream()
                .map(l -> Course.Lesson.builder()
                        .id(UUID.randomUUID().toString())
                        .title(requiredText(l.getTitle(), "Lesson title is required"))
                        .videoUrl(inputSanitizer.sanitizeHttpUrl(l.getVideoUrl(), "videoUrl"))
                        .duration(safeDuration(l.getDuration()))
                        .isPreview(l.getIsPreview())
                        .build())
                .collect(Collectors.toList());
    }
    
    private int calculateTotalDuration(List<Course.Section> sections) {
        return sections.stream()
                .flatMap(section -> section.getLessons().stream())
                .mapToInt(lesson -> lesson.getDuration() != null ? lesson.getDuration() : 0)
                .sum();
    }

    private String requiredText(String value, String errorMessage) {
        String sanitized = inputSanitizer.sanitizePlainText(value);
        if (sanitized == null) {
            throw new BadRequestException(errorMessage);
        }

        return sanitized;
    }

    private String optionalText(String value) {
        return inputSanitizer.sanitizePlainText(value);
    }

    private int safeDuration(Integer duration) {
        if (duration == null) {
            return 0;
        }

        if (duration < 0) {
            throw new BadRequestException("Duration must be greater than or equal to 0");
        }

        return duration;
    }

    private String resolveLevel(String level) {
        String sanitized = optionalText(level);
        return sanitized != null ? sanitized : "beginner";
    }
}
