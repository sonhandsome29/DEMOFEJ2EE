package com.edulearn.service;

import com.edulearn.dto.request.LoginRequest;
import com.edulearn.dto.request.RegisterRequest;
import com.edulearn.dto.response.AuthResponse;
import com.edulearn.entity.User;
import com.edulearn.exception.BadRequestException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.UserRepository;
import com.edulearn.security.JwtTokenProvider;
import com.edulearn.validation.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;
    
    public AuthResponse register(RegisterRequest request) {
        validatePasswordStrength(request.getPassword());

        String email = request.getEmail().trim().toLowerCase();

        // Check if email exists
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }
        
        // Create new user
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(User.Role.USER)
                .build();
        
        user = userRepository.save(user);
        
        // Generate token
        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getTokenVersion());
        
        return AuthResponse.of(token, user);
    }
    
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );
        
        // Get user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        // Generate token
        String token = tokenProvider.generateToken(authentication);
        
        return AuthResponse.of(token, user);
    }
    
    public User getCurrentUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validatePasswordStrength(newPassword);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        passwordResetService.invalidateTokensForUser(userId);
    }

    private void validatePasswordStrength(String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new BadRequestException(PasswordPolicy.MESSAGE);
        }
    }
}
