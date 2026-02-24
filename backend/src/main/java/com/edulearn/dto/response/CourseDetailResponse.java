package com.edulearn.dto.response;

import com.edulearn.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
public class CourseDetailResponse {

    private String id;
    private String title;
    private String slug;
    private String description;
    private String thumbnail;
    private String category;
    private String level;
    private String instructor;
    private Long price;
    private Long originalPrice;
    private Integer duration;
    private Integer studentsCount;
    private Double rating;
    private Integer reviewsCount;
    private List<SectionResponse> sections;

    public static CourseDetailResponse fromPublicCourse(Course course) {
        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .slug(course.getSlug())
                .description(course.getDescription())
                .thumbnail(course.getThumbnail())
                .category(course.getCategory())
                .level(course.getLevel())
                .instructor(course.getInstructor())
                .price(course.getPrice())
                .originalPrice(course.getOriginalPrice())
                .duration(course.getDuration())
                .studentsCount(course.getStudentsCount())
                .rating(course.getRating())
                .reviewsCount(course.getReviewsCount())
                .sections(toSectionResponses(course.getSections()))
                .build();
    }

    private static List<SectionResponse> toSectionResponses(List<Course.Section> sections) {
        if (sections == null) {
            return List.of();
        }

        return sections.stream()
                .map(section -> SectionResponse.builder()
                        .id(section.getId())
                        .title(section.getTitle())
                        .lessons(toLessonResponses(section.getLessons()))
                        .build())
                .toList();
    }

    private static List<LessonResponse> toLessonResponses(List<Course.Lesson> lessons) {
        if (lessons == null) {
            return List.of();
        }

        return lessons.stream()
                .map(lesson -> LessonResponse.builder()
                        .id(lesson.getId())
                        .title(lesson.getTitle())
                        .duration(lesson.getDuration())
                        .isPreview(Boolean.TRUE.equals(lesson.getIsPreview()))
                        .videoUrl(Boolean.TRUE.equals(lesson.getIsPreview()) ? lesson.getVideoUrl() : null)
                        .build())
                .toList();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectionResponse {
        private String id;
        private String title;
        private List<LessonResponse> lessons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonResponse {
        private String id;
        private String title;
        private String videoUrl;
        private Integer duration;
        private Boolean isPreview;
    }
}
