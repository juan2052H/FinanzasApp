package com.finanzas.backend.repo;

import com.finanzas.backend.domain.AccountTokenEntity;
import com.finanzas.backend.domain.AccountTokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountTokenRepository extends JpaRepository<AccountTokenEntity, UUID> {
    Optional<AccountTokenEntity> findByTokenHashAndTypeAndConsumedAtIsNull(String tokenHash, AccountTokenType type);
    List<AccountTokenEntity> findByUserIdAndTypeAndConsumedAtIsNull(UUID userId, AccountTokenType type);
    long deleteByExpiresAtBefore(Instant instant);
}
