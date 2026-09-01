package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.BudgetDtos;
import com.finanzas.backend.domain.BudgetEntity;
import com.finanzas.backend.domain.CategoryEntity;
import com.finanzas.backend.domain.CategoryType;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.CategoryRepository;
import com.finanzas.backend.repo.BudgetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BudgetService {
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;

    public BudgetService(BudgetRepository budgets,
                         CategoryRepository categories,
                         WorkspaceAccessService access,
                         AuditLogService auditLogs) {
        this.budgets = budgets;
        this.categories = categories;
        this.access = access;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<BudgetDtos.BudgetResponse> list(UUID userId, UUID workspaceId) {
        access.requireMember(userId, workspaceId);
        return budgets.findByWorkspaceIdOrderByPeriodMonthDesc(workspaceId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public BudgetDtos.BudgetResponse create(UUID userId, UUID workspaceId, BudgetDtos.BudgetRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        CategoryEntity category = requireExpenseCategory(request.categoryId(), workspaceId);
        BudgetEntity budget = budgets.save(new BudgetEntity(workspaceId, request.categoryId(), request.periodMonth(), request.amount()));
        auditLogs.record(workspaceId, userId, "BUDGET_CREATED", "Budget", budget.getId(), Map.of(
                "amount", budget.getAmount().toPlainString(),
                "categoryId", category.getId().toString(),
                "periodMonth", budget.getPeriodMonth().toString()));
        return toResponse(budget);
    }

    @Transactional
    public BudgetDtos.BudgetResponse update(UUID userId, UUID workspaceId, UUID budgetId, BudgetDtos.BudgetRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        BudgetEntity budget = budgets.findByIdAndWorkspaceId(budgetId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado."));
        CategoryEntity category = requireExpenseCategory(request.categoryId(), workspaceId);
        budget.update(category.getId(), request.periodMonth(), request.amount());
        auditLogs.record(workspaceId, userId, "BUDGET_UPDATED", "Budget", budgetId, Map.of(
                "amount", budget.getAmount().toPlainString(),
                "categoryId", category.getId().toString(),
                "periodMonth", budget.getPeriodMonth().toString()));
        return toResponse(budget);
    }

    @Transactional
    public void delete(UUID userId, UUID workspaceId, UUID budgetId) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        BudgetEntity budget = budgets.findByIdAndWorkspaceId(budgetId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presupuesto no encontrado."));
        budgets.delete(budget);
        auditLogs.record(workspaceId, userId, "BUDGET_DELETED", "Budget", budgetId, Map.of());
    }

    private BudgetDtos.BudgetResponse toResponse(BudgetEntity budget) {
        return new BudgetDtos.BudgetResponse(budget.getId(), budget.getWorkspaceId(), budget.getCategoryId(), budget.getPeriodMonth(), budget.getAmount());
    }

    private CategoryEntity requireExpenseCategory(UUID categoryId, UUID workspaceId) {
        CategoryEntity category = categories.findByIdAndWorkspaceId(categoryId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria no pertenece al workspace."));
        if (category.getType() != CategoryType.EXPENSE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El presupuesto debe usar una categoria de gasto.");
        }
        return category;
    }
}
