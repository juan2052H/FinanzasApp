package com.finanzas.backend.repo;

import com.finanzas.backend.domain.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {
    Optional<RefreshTokenEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);
    List<RefreshTokenEntity> findByUserIdAndRevokedAtIsNull(UUID userId);

    long deleteByExpiresAtBefore(Instant instant);
}
