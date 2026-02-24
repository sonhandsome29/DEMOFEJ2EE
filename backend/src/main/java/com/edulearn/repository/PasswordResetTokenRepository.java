package com.edulearn.repository;

import com.edulearn.entity.PasswordResetToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    List<PasswordResetToken> findByUserIdAndUsedFalse(String userId);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
