package com.finanzas.backend.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class SavingsGoalDtos {
    private SavingsGoalDtos() {
    }

    public record SavingsGoalRequest(
            @NotBlank String name,
            @PositiveOrZero BigDecimal currentAmount,
            @NotNull @Positive BigDecimal targetAmount,
            String color,
            String icono,
            LocalDate dueDate) {
    }

    public record ContributionRequest(@NotNull @Positive BigDecimal amount) {
    }

    public record SavingsGoalResponse(UUID id, UUID workspaceId, String name, BigDecimal currentAmount,
                                      BigDecimal targetAmount, String color, String icono, LocalDate dueDate, String status) {
    }
}
