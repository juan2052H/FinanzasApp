package com.finanzas.backend.repo;

import com.finanzas.backend.domain.CategoryEntity;
import com.finanzas.backend.domain.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    List<CategoryEntity> findByWorkspaceIdOrderByNombre(UUID workspaceId);
    List<CategoryEntity> findByWorkspaceIdAndArchivedFalseOrderByNombre(UUID workspaceId);
    Optional<CategoryEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    Optional<CategoryEntity> findByWorkspaceIdAndNombreIgnoreCaseAndType(UUID workspaceId, String nombre, CategoryType type);
}
