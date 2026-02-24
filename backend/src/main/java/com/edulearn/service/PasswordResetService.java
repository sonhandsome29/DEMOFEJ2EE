package com.edulearn.service;

import com.edulearn.entity.PasswordResetToken;
import com.edulearn.entity.User;
import com.edulearn.exception.BadRequestException;
import com.edulearn.exception.ResourceNotFoundException;
import com.edulearn.repository.PasswordResetTokenRepository;
import com.edulearn.repository.UserRepository;
import com.edulearn.validation.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 48;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.password-reset.token-expiration-minutes:30}")
    private long tokenExpirationMinutes;

    @Transactional
    public String requestPasswordReset(String email) {
        cleanupExpiredTokens();

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);
        if (userOptional.isEmpty()) {
            return null;
        }

        User user = userOptional.get();
        invalidateActiveTokens(user.getId(), null);

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(resolveExpirationMinutes());

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset requested for email {}", normalizedEmail);

        return rawToken;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        cleanupExpiredTokens();

        if (!PasswordPolicy.isValid(newPassword)) {
            throw new BadRequestException(PasswordPolicy.MESSAGE);
        }

        String tokenHash = hashToken(token);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.getExpiresAt() == null || !resetToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        invalidateActiveTokens(user.getId(), resetToken.getId());
    }

    @Transactional
    public void invalidateTokensForUser(String userId) {
        invalidateActiveTokens(userId, null);
    }

    private void invalidateActiveTokens(String userId, String keepTokenId) {
        List<PasswordResetToken> activeTokens = passwordResetTokenRepository.findByUserIdAndUsedFalse(userId);
        List<PasswordResetToken> tokensToInvalidate = activeTokens.stream()
                .filter(token -> keepTokenId == null || !token.getId().equals(keepTokenId))
                .toList();

        if (tokensToInvalidate.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        tokensToInvalidate.forEach(token -> {
            token.setUsed(true);
            token.setUsedAt(now);
        });
        passwordResetTokenRepository.saveAll(tokensToInvalidate);
    }

    private void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
    }

    private long resolveExpirationMinutes() {
        return tokenExpirationMinutes > 0 ? tokenExpirationMinutes : 30;
    }

    private String generateToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
