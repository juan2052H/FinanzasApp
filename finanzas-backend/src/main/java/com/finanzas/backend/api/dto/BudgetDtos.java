package com.finanzas.backend.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class BudgetDtos {
    private BudgetDtos() {
    }

    public record BudgetRequest(@NotNull UUID categoryId, @NotNull LocalDate periodMonth, @NotNull @PositiveOrZero BigDecimal amount) {
    }

    public record BudgetResponse(UUID id, UUID workspaceId, UUID categoryId, LocalDate periodMonth, BigDecimal amount) {
    }
}
