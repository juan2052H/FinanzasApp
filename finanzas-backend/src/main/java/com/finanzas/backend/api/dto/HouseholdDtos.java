package com.finanzas.backend.api.dto;

import com.finanzas.backend.domain.InvitationStatus;
import com.finanzas.backend.domain.SharedExpenseStatus;
import com.finanzas.backend.domain.SplitMethod;
import com.finanzas.backend.domain.WorkspaceRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class HouseholdDtos {
    private HouseholdDtos() {
    }

    public record InvitationRequest(
            @Email @NotBlank String email,
            @NotNull WorkspaceRole role) {
    }

    public record InvitationResponse(
            UUID id,
            UUID workspaceId,
            String workspaceName,
            String invitedEmail,
            UUID invitedByUserId,
            WorkspaceRole role,
            InvitationStatus status,
            Instant expiresAt,
            Instant createdAt) {
    }

    public record MemberResponse(
            UUID userId,
            String nombre,
            String apellido,
            String email,
            WorkspaceRole role,
            Instant joinedAt) {
    }

    public record MemberRoleRequest(@NotNull WorkspaceRole role) {
    }

    public record TransferOwnerRequest(@NotNull UUID newOwnerUserId) {
    }

    public record SplitParticipantRequest(
            @NotNull UUID userId,
            @PositiveOrZero BigDecimal amount,
            @Positive BigDecimal percentage) {
    }

    public record SharedExpenseRequest(
            @NotBlank String description,
            @NotNull @Positive BigDecimal amount,
            LocalDate date,
            UUID paidByUserId,
            UUID categoryId,
            @NotNull SplitMethod splitMethod,
            @Valid List<SplitParticipantRequest> participants) {
    }

    public record ExpenseSplitResponse(
            UUID id,
            UUID userId,
            String nombre,
            String email,
            BigDecimal amount,
            BigDecimal percentage,
            BigDecimal settledAmount,
            BigDecimal outstandingAmount) {
    }

    public record SharedExpenseResponse(
            UUID id,
            UUID workspaceId,
            UUID paidByUserId,
            String paidByName,
            UUID categoryId,
            String description,
            BigDecimal amount,
            LocalDate date,
            SplitMethod splitMethod,
            SharedExpenseStatus status,
            List<ExpenseSplitResponse> splits) {
    }

    public record SettlementRequest(
            @NotNull UUID fromUserId,
            @NotNull UUID toUserId,
            @NotNull @Positive BigDecimal amount,
            LocalDate date,
            String note) {
    }

    public record SettlementResponse(
            UUID id,
            UUID workspaceId,
            UUID fromUserId,
            String fromName,
            UUID toUserId,
            String toName,
            BigDecimal amount,
            LocalDate date,
            String note) {
    }

    public record MemberBalanceResponse(
            UUID userId,
            String nombre,
            String email,
            BigDecimal owes,
            BigDecimal owed,
            BigDecimal netBalance) {
    }
}
