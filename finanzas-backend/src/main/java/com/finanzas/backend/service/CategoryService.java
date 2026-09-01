package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.CategoryDtos;
import com.finanzas.backend.domain.CategoryEntity;
import com.finanzas.backend.domain.CategoryType;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;

    public CategoryService(CategoryRepository categories, WorkspaceAccessService access, AuditLogService auditLogs) {
        this.categories = categories;
        this.access = access;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.CategoryResponse> list(UUID userId, UUID workspaceId, CategoryType type, boolean includeArchived, String query) {
        access.requireMember(userId, workspaceId);
        String normalizedQuery = normalizeSearch(query);
        return categories.findByWorkspaceIdOrderByNombre(workspaceId).stream()
                .filter(category -> type == null || category.getType() == type)
                .filter(category -> includeArchived || !category.isArchived())
                .filter(category -> normalizedQuery.isEmpty()
                        || normalizeSearch(category.getNombre()).contains(normalizedQuery)
                        || normalizeSearch(category.getIcono()).contains(normalizedQuery))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoryDtos.CategoryResponse create(UUID userId, UUID workspaceId, CategoryDtos.CategoryRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        CategoryType type = request.type() == null ? CategoryType.EXPENSE : request.type();
        CategoryEntity category = categories.findByWorkspaceIdAndNombreIgnoreCaseAndType(workspaceId, request.nombre(), type)
                .map(existing -> {
                    if (!existing.isArchived()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoria activa con ese nombre y tipo.");
                    }
                    existing.update(request.nombre(), request.icono(), request.color());
                    existing.restore();
                    return existing;
                })
                .orElseGet(() -> categories.save(new CategoryEntity(workspaceId, request.nombre(), type, request.icono(), request.color())));
        auditLogs.record(workspaceId, userId, "CATEGORY_CREATED", "Category", category.getId(), Map.of(
                "name", category.getNombre(),
                "type", category.getType().name()));
        return toResponse(category);
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(UUID userId, UUID workspaceId, UUID categoryId, CategoryDtos.CategoryRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        CategoryEntity category = categories.findByIdAndWorkspaceId(categoryId, workspaceId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Categoria no encontrada."));
        categories.findByWorkspaceIdAndNombreIgnoreCaseAndType(workspaceId, request.nombre(), category.getType())
                .filter(existing -> !existing.getId().equals(categoryId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoria con ese nombre y tipo.");
                });
        category.update(request.nombre(), request.icono(), request.color());
        auditLogs.record(workspaceId, userId, "CATEGORY_UPDATED", "Category", categoryId, Map.of(
                "name", category.getNombre(),
                "type", category.getType().name()));
        return toResponse(category);
    }

    @Transactional
    public void archive(UUID userId, UUID workspaceId, UUID categoryId) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        CategoryEntity category = categories.findByIdAndWorkspaceId(categoryId, workspaceId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Categoria no encontrada."));
        category.archive();
        auditLogs.record(workspaceId, userId, "CATEGORY_ARCHIVED", "Category", categoryId, Map.of(
                "name", category.getNombre()));
    }

    @Transactional
    public CategoryDtos.CategoryResponse restore(UUID userId, UUID workspaceId, UUID categoryId) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        CategoryEntity category = categories.findByIdAndWorkspaceId(categoryId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria no encontrada."));
        categories.findByWorkspaceIdAndNombreIgnoreCaseAndType(workspaceId, category.getNombre(), category.getType())
                .filter(existing -> !existing.getId().equals(categoryId))
                .filter(existing -> !existing.isArchived())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoria activa con ese nombre y tipo.");
                });
        category.restore();
        auditLogs.record(workspaceId, userId, "CATEGORY_RESTORED", "Category", categoryId, Map.of(
                "name", category.getNombre()));
        return toResponse(category);
    }

    private CategoryDtos.CategoryResponse toResponse(CategoryEntity category) {
        return new CategoryDtos.CategoryResponse(
                category.getId(),
                category.getWorkspaceId(),
                category.getNombre(),
                category.getType(),
                category.getIcono(),
                category.getColor(),
                category.isArchived());
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
    }
}
