package com.finanzas.backend.api.dto;

import com.finanzas.backend.domain.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class TransactionDtos {
    private TransactionDtos() {
    }

    public record TransactionRequest(
            @NotNull UUID categoryId,
            @NotNull TransactionType type,
            String description,
            @NotNull @Positive BigDecimal amount,
            @NotNull LocalDate date) {
    }

    public record TransactionResponse(
            UUID id,
            UUID workspaceId,
            UUID categoryId,
            UUID createdByUserId,
            TransactionType type,
            String description,
            BigDecimal amount,
            LocalDate date) {
    }
}
