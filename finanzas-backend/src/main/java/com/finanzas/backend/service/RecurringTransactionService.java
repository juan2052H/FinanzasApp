package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.RecurringDtos;
import com.finanzas.backend.api.dto.TransactionDtos;
import com.finanzas.backend.domain.CategoryEntity;
import com.finanzas.backend.domain.CategoryType;
import com.finanzas.backend.domain.RecurringTransactionEntity;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.CategoryRepository;
import com.finanzas.backend.repo.RecurringTransactionRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RecurringTransactionService {
    private final RecurringTransactionRepository recurringTransactions;
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;

    public RecurringTransactionService(RecurringTransactionRepository recurringTransactions,
                                       TransactionRepository transactions,
                                       CategoryRepository categories,
                                       WorkspaceAccessService access,
                                       AuditLogService auditLogs) {
        this.recurringTransactions = recurringTransactions;
        this.transactions = transactions;
        this.categories = categories;
        this.access = access;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<RecurringDtos.RecurringTransactionResponse> list(UUID userId, UUID workspaceId, boolean activeOnly) {
        access.requireMember(userId, workspaceId);
        List<RecurringTransactionEntity> result = activeOnly
                ? recurringTransactions.findByWorkspaceIdAndActiveTrueOrderByNextRunDateAsc(workspaceId)
                : recurringTransactions.findByWorkspaceIdOrderByNextRunDateAsc(workspaceId);
        return result.stream().map(this::toResponse).toList();
    }

    @Transactional
    public RecurringDtos.RecurringTransactionResponse create(UUID userId, UUID workspaceId, RecurringDtos.RecurringTransactionRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        CategoryEntity category = categories.findByIdAndWorkspaceId(request.categoryId(), workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria no pertenece al workspace."));
        requireCompatibleCategory(category, request.type());
        RecurringTransactionEntity recurring = recurringTransactions.save(new RecurringTransactionEntity(
                workspaceId,
                category.getId(),
                request.type(),
                request.description(),
                request.amount(),
                request.frequency(),
                request.customIntervalDays(),
                request.nextRunDate()));
        auditLogs.record(workspaceId, userId, "RECURRING_TRANSACTION_CREATED", "RecurringTransaction", recurring.getId(), Map.of(
                "type", recurring.getType().name(),
                "amount", recurring.getAmount().toPlainString(),
                "frequency", recurring.getFrequency().name(),
                "nextRunDate", recurring.getNextRunDate().toString()));
        return toResponse(recurring);
    }

    @Transactional
    public void deactivate(UUID userId, UUID workspaceId, UUID recurringTransactionId) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        RecurringTransactionEntity recurring = recurringTransactions.findByIdAndWorkspaceId(recurringTransactionId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaccion recurrente no encontrada."));
        recurring.deactivate();
        auditLogs.record(workspaceId, userId, "RECURRING_TRANSACTION_DEACTIVATED", "RecurringTransaction", recurringTransactionId, Map.of());
    }

    @Transactional
    public TransactionDtos.TransactionResponse run(UUID userId, UUID workspaceId, UUID recurringTransactionId) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        RecurringTransactionEntity recurring = recurringTransactions.findByIdAndWorkspaceId(recurringTransactionId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaccion recurrente no encontrada."));
        if (!recurring.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La recurrencia esta inactiva.");
        }
        TransactionEntity transaction = transactions.save(new TransactionEntity(
                workspaceId,
                recurring.getCategoryId(),
                userId,
                recurring.getType(),
                recurring.getDescription(),
                recurring.getAmount(),
                recurring.getNextRunDate()));
        recurring.advanceNextRunDate();
        auditLogs.record(workspaceId, userId, "RECURRING_TRANSACTION_RUN", "RecurringTransaction", recurringTransactionId, Map.of(
                "transactionId", transaction.getId().toString(),
                "nextRunDate", recurring.getNextRunDate().toString()));
        return new TransactionDtos.TransactionResponse(
                transaction.getId(),
                transaction.getWorkspaceId(),
                transaction.getCategoryId(),
                transaction.getCreatedByUserId(),
                transaction.getType(),
                transaction.getDescripcion(),
                transaction.getMonto(),
                transaction.getTransactionDate());
    }

    RecurringDtos.RecurringTransactionResponse toResponse(RecurringTransactionEntity recurring) {
        return new RecurringDtos.RecurringTransactionResponse(
                recurring.getId(),
                recurring.getWorkspaceId(),
                recurring.getCategoryId(),
                recurring.getType(),
                recurring.getDescription(),
                recurring.getAmount(),
                recurring.getFrequency(),
                recurring.getCustomIntervalDays(),
                recurring.getNextRunDate(),
                recurring.isActive());
    }

    private void requireCompatibleCategory(CategoryEntity category, TransactionType transactionType) {
        TransactionType type = transactionType == null ? TransactionType.EXPENSE : transactionType;
        boolean validIncome = type == TransactionType.INCOME && category.getType() == CategoryType.INCOME;
        boolean validExpense = type == TransactionType.EXPENSE && category.getType() == CategoryType.EXPENSE;
        if (!validIncome && !validExpense) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoria no coincide con el tipo de recurrencia.");
        }
    }
}
