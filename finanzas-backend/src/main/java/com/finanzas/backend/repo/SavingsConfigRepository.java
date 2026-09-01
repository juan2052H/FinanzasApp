package com.finanzas.backend.repo;

import com.finanzas.backend.domain.SavingsConfigEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SavingsConfigRepository extends JpaRepository<SavingsConfigEntity, UUID> {
    Optional<SavingsConfigEntity> findByWorkspaceId(UUID workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select config from SavingsConfigEntity config where config.workspaceId = :workspaceId")
    Optional<SavingsConfigEntity> findByWorkspaceIdForUpdate(@Param("workspaceId") UUID workspaceId);
}
