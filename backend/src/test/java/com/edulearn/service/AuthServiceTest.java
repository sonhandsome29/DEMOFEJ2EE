package com.edulearn.service;

import com.edulearn.dto.request.RegisterRequest;
import com.edulearn.dto.response.AuthResponse;
import com.edulearn.entity.User;
import com.edulearn.exception.BadRequestException;
import com.edulearn.repository.UserRepository;
import com.edulearn.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerAlwaysCreatesRegularUserRole() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Alice Admin");
        request.setEmail("Admin@Example.com");
        request.setPassword("Password123!");

        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded-password");
        when(userRepository.save(argThat(matchesRegularUser("admin@example.com")))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("u-1");
            return user;
        });
        when(tokenProvider.generateToken("u-1", "admin@example.com", 0L)).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("USER", response.getUser().getRole());
        assertEquals("admin@example.com", response.getUser().getEmail());
    }

    private ArgumentMatcher<User> matchesRegularUser(String email) {
        return user -> user != null
                && email.equals(user.getEmail())
                && user.getRole() == User.Role.USER;
    }

    @Test
    void changePasswordUpdatesPasswordWhenCurrentPasswordIsValid() {
        User user = new User();
        user.setId("u-1");
        user.setPassword("encoded-old-password");

        when(userRepository.findById("u-1")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("Oldpass123!", "encoded-old-password")).thenReturn(true);
        when(passwordEncoder.matches("Newpass123!", "encoded-old-password")).thenReturn(false);
        when(passwordEncoder.encode("Newpass123!")).thenReturn("encoded-new-password");

        authService.changePassword("u-1", "Oldpass123!", "Newpass123!");

        assertEquals("encoded-new-password", user.getPassword());
        assertEquals(1L, user.getTokenVersion());
        verify(userRepository).save(user);
        verify(passwordResetService).invalidateTokensForUser("u-1");
    }

    @Test
    void changePasswordThrowsWhenCurrentPasswordIsInvalid() {
        User user = new User();
        user.setId("u-1");
        user.setPassword("encoded-old-password");

        when(userRepository.findById("u-1")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("Wrongpass123!", "encoded-old-password")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> authService.changePassword("u-1", "Wrongpass123!", "Newpass123!"));
    }

    @Test
    void changePasswordThrowsWhenNewPasswordMatchesCurrentPassword() {
        User user = new User();
        user.setId("u-1");
        user.setPassword("encoded-old-password");

        when(userRepository.findById("u-1")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("Oldpass123!", "encoded-old-password")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> authService.changePassword("u-1", "Oldpass123!", "Oldpass123!"));
    }

    @Test
    void registerThrowsWhenPasswordIsWeak() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("weakpass");

        assertThrows(BadRequestException.class, () -> authService.register(request));
    }
}
