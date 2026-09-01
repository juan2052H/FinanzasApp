package com.finanzas.backend.api.dto;

import com.finanzas.backend.domain.CategoryType;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class CategoryDtos {
    private CategoryDtos() {
    }

    public record CategoryRequest(@NotBlank String nombre, CategoryType type, String icono, String color) {
    }

    public record CategoryResponse(UUID id, UUID workspaceId, String nombre, CategoryType type, String icono, String color, boolean archived) {
    }
}
