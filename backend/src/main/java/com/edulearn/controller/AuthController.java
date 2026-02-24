package com.edulearn.controller;

import com.edulearn.dto.request.ForgotPasswordRequest;
import com.edulearn.dto.request.LoginRequest;
import com.edulearn.dto.request.ChangePasswordRequest;
import com.edulearn.dto.request.RegisterRequest;
import com.edulearn.dto.request.ResetPasswordRequest;
import com.edulearn.dto.response.ApiResponse;
import com.edulearn.dto.response.AuthResponse;
import com.edulearn.entity.User;
import com.edulearn.security.UserPrincipal;
import com.edulearn.service.AuthService;
import com.edulearn.service.PasswordResetService;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final Environment environment;

    @Value("${auth.password-reset.debug-return-token:false}")
    private boolean debugReturnToken;

    @PostConstruct
    public void validateDebugTokenExposureSetting() {
        if (debugReturnToken && !environment.acceptsProfiles(Profiles.of("dev", "local", "test"))) {
            throw new IllegalStateException("Password reset token exposure is only allowed in dev/local/test profiles");
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse.UserDto>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        User user = authService.getCurrentUser(currentUser.getId());
        
        AuthResponse.UserDto userDto = AuthResponse.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        String resetToken = passwordResetService.requestPasswordReset(request.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "If an account exists, a password reset link has been sent");
        if (shouldExposeResetToken() && resetToken != null) {
            response.put("resetToken", resetToken);
        }

        return ResponseEntity.ok(ApiResponse.success("Password reset request accepted", response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully", null));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(currentUser.getId(), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    private boolean shouldExposeResetToken() {
        return debugReturnToken && environment.acceptsProfiles(Profiles.of("dev", "local", "test"));
    }
}
