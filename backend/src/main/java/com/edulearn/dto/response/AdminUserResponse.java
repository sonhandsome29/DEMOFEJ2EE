package com.edulearn.dto.response;

import com.edulearn.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponse {

    private String id;
    private String email;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private String role;
    private boolean enabled;
    private int enrolledCoursesCount;
    private LocalDateTime createdAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .enrolledCoursesCount(user.getEnrolledCourseIds() != null ? user.getEnrolledCourseIds().size() : 0)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
