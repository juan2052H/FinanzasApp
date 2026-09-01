package com.finanzas.backend.repo;

import com.finanzas.backend.domain.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByUserIdAndWorkspaceIdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId, UUID workspaceId);
    List<NotificationEntity> findByUserIdAndWorkspaceIdOrderByCreatedAtDesc(UUID userId, UUID workspaceId);
    Optional<NotificationEntity> findByIdAndUserId(UUID id, UUID userId);
    Optional<NotificationEntity> findByIdAndUserIdAndWorkspaceId(UUID id, UUID userId, UUID workspaceId);
    boolean existsByUserIdAndWorkspaceIdAndTypeAndTitleAndReadAtIsNull(UUID userId, UUID workspaceId, String type, String title);
}
