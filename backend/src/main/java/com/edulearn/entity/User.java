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
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email;
    
    private String password;
    
    private String fullName;
    
    private String phone;
    
    private String avatarUrl;
    
    @Builder.Default
    private Role role = Role.USER;
    
    @Builder.Default
    private List<String> enrolledCourseIds = new ArrayList<>();
    
    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private long tokenVersion = 0L;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    public enum Role {
        USER, ADMIN
    }
}
