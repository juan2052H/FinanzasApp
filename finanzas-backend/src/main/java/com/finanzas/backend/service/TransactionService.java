package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.TransactionDtos;
import com.finanzas.backend.domain.CategoryEntity;
import com.finanzas.backend.domain.CategoryType;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.CategoryRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;
    private final SavingsService savings;

    public TransactionService(TransactionRepository transactions,
                              CategoryRepository categories,
                              WorkspaceAccessService access,
                              AuditLogService auditLogs,
                              SavingsService savings) {
        this.transactions = transactions;
        this.categories = categories;
        this.access = access;
        this.auditLogs = auditLogs;
        this.savings = savings;
    }

    @Transactional(readOnly = true)
    public List<TransactionDtos.TransactionResponse> list(UUID userId, UUID workspaceId, TransactionType type, LocalDate from, LocalDate to) {
        access.requireMember(userId, workspaceId);
        LocalDate start = from == null ? LocalDate.of(1970, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.now().plusYears(20) : to;
        List<TransactionEntity> result = type == null
                ? transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(workspaceId, start, end)
                : transactions.findByWorkspaceIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDesc(workspaceId, type, start, end);
        return result.stream().map(this::toResponse).toList();
    }

    @Transactional
    public TransactionDtos.TransactionResponse create(UUID userId, UUID workspaceId, TransactionDtos.TransactionRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        CategoryEntity category = categories.findByIdAndWorkspaceId(request.categoryId(), workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria no pertenece al workspace."));
        requireCompatibleCategory(category, request.type());
        TransactionEntity transaction = transactions.save(new TransactionEntity(
                workspaceId,
                category.getId(),
                userId,
                request.type(),
                request.description(),
                request.amount(),
                request.date()));
        savings.recordAutomaticAllocationForCreatedIncome(userId, workspaceId, transaction);
        auditLogs.record(workspaceId, userId, "TRANSACTION_CREATED", "Transaction", transaction.getId(), Map.of(
                "type", transaction.getType().name(),
                "amount", transaction.getMonto().toPlainString(),
                "categoryId", category.getId().toString()));
        return toResponse(transaction);
    }

    @Transactional
    public TransactionDtos.TransactionResponse update(UUID userId, UUID workspaceId, UUID transactionId, TransactionDtos.TransactionRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        TransactionEntity transaction = transactions.findByIdAndWorkspaceId(transactionId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaccion no encontrada."));
        CategoryEntity category = categories.findByIdAndWorkspaceId(request.categoryId(), workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria no pertenece al workspace."));
        requireCompatibleCategory(category, request.type());
        transaction.update(category.getId(), request.type(), request.description(), request.amount(), request.date());
        savings.reconcileAutomaticAllocationForUpdatedTransaction(
                userId,
                workspaceId,
                transaction.getId(),
                transaction.getType(),
                transaction.getMonto(),
                transaction.getTransactionDate());
        auditLogs.record(workspaceId, userId, "TRANSACTION_UPDATED", "Transaction", transactionId, Map.of(
                "type", transaction.getType().name(),
                "amount", transaction.getMonto().toPlainString(),
                "categoryId", category.getId().toString()));
        return toResponse(transaction);
    }

    @Transactional
    public void delete(UUID userId, UUID workspaceId, UUID transactionId) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        TransactionEntity transaction = transactions.findByIdAndWorkspaceId(transactionId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaccion no encontrada."));
        savings.reverseAutomaticAllocationForDeletedTransaction(userId, workspaceId, transactionId);
        transactions.delete(transaction);
        auditLogs.record(workspaceId, userId, "TRANSACTION_DELETED", "Transaction", transactionId, Map.of());
    }

    TransactionDtos.TransactionResponse toResponse(TransactionEntity transaction) {
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

    private void requireCompatibleCategory(CategoryEntity category, TransactionType transactionType) {
        TransactionType type = transactionType == null ? TransactionType.EXPENSE : transactionType;
        boolean validIncome = type == TransactionType.INCOME && category.getType() == CategoryType.INCOME;
        boolean validExpense = type == TransactionType.EXPENSE && category.getType() == CategoryType.EXPENSE;
        if (!validIncome && !validExpense) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoria no coincide con el tipo de transaccion.");
        }
    }
}
