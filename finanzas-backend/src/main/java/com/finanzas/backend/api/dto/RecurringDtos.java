package com.finanzas.backend.api.dto;

import com.finanzas.backend.domain.RecurringFrequency;
import com.finanzas.backend.domain.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class RecurringDtos {
    private RecurringDtos() {
    }

    public record RecurringTransactionRequest(
            @NotNull UUID categoryId,
            @NotNull TransactionType type,
            String description,
            @NotNull @Positive BigDecimal amount,
            @NotNull RecurringFrequency frequency,
            Integer customIntervalDays,
            @NotNull LocalDate nextRunDate) {
    }

    public record RecurringTransactionResponse(
            UUID id,
            UUID workspaceId,
            UUID categoryId,
            TransactionType type,
            String description,
            BigDecimal amount,
            RecurringFrequency frequency,
            int customIntervalDays,
            LocalDate nextRunDate,
            boolean active) {
    }
}
