package com.edulearn.service;

import com.edulearn.entity.PasswordResetToken;
import com.edulearn.entity.User;
import com.edulearn.exception.BadRequestException;
import com.edulearn.repository.PasswordResetTokenRepository;
import com.edulearn.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "tokenExpirationMinutes", 30L);
    }

    @Test
    void requestPasswordResetReturnsTokenForExistingUser() {
        User user = new User();
        user.setId("u1");
        user.setEmail("user@example.com");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUserIdAndUsedFalse("u1")).thenReturn(List.of());

        String token = passwordResetService.requestPasswordReset("user@example.com");

        assertNotNull(token);
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertEquals("u1", tokenCaptor.getValue().getUserId());
        assertEquals(hash(token), tokenCaptor.getValue().getTokenHash());
    }

    @Test
    void requestPasswordResetReturnsNullForUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        String token = passwordResetService.requestPasswordReset("missing@example.com");

        assertNull(token);
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
    }

    @Test
    void resetPasswordUpdatesPasswordAndMarksTokenUsed() {
        User user = new User();
        user.setId("u1");
        user.setEmail("user@example.com");

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id("t1")
                .userId("u1")
                .tokenHash(hash("valid-token"))
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(hash("valid-token")))
                .thenReturn(Optional.of(resetToken));
        when(passwordResetTokenRepository.findByUserIdAndUsedFalse("u1")).thenReturn(List.of(resetToken));
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Newpass123!")).thenReturn("encoded-pass");

        passwordResetService.resetPassword("valid-token", "Newpass123!");

        assertEquals("encoded-pass", user.getPassword());
        assertEquals(1L, user.getTokenVersion());
        assertEquals(true, resetToken.isUsed());
        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    void resetPasswordThrowsWhenTokenInvalid() {
        when(passwordResetTokenRepository.findByTokenHashAndUsedFalse(hash("invalid-token")))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword("invalid-token", "Newpass123!"));
    }

    @Test
    void resetPasswordThrowsWhenNewPasswordIsWeak() {
        assertThrows(BadRequestException.class,
                () -> passwordResetService.resetPassword("any-token", "weakpass"));

        verify(passwordResetTokenRepository, never()).findByTokenHashAndUsedFalse(any());
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
