package com.edulearn.dto.response;

import com.edulearn.entity.Course;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CourseSummaryResponse {

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

    public static CourseSummaryResponse from(Course course) {
        return CourseSummaryResponse.builder()
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
                .build();
    }
}
