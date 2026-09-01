package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.AnalyticsDtos;
import com.finanzas.backend.api.dto.SavingsDtos;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.repo.CategoryRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final WorkspaceAccessService access;
    private final SavingsService savings;

    public AnalyticsService(TransactionRepository transactions, CategoryRepository categories, WorkspaceAccessService access, SavingsService savings) {
        this.transactions = transactions;
        this.categories = categories;
        this.access = access;
        this.savings = savings;
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.SummaryResponse summary(UUID userId, UUID workspaceId) {
        access.requireMember(userId, workspaceId);
        BigDecimal income = BigDecimal.ZERO.setScale(2);
        BigDecimal expenses = BigDecimal.ZERO.setScale(2);
        Map<UUID, String> categoryNames = new LinkedHashMap<>();
        categories.findByWorkspaceIdAndArchivedFalseOrderByNombre(workspaceId)
                .forEach(category -> categoryNames.put(category.getId(), category.getNombre()));
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();

        for (TransactionEntity transaction : transactions.findByWorkspaceIdOrderByTransactionDateDesc(workspaceId)) {
            if (transaction.getType() == TransactionType.INCOME) {
                income = income.add(transaction.getMonto());
            } else {
                expenses = expenses.add(transaction.getMonto());
                String category = categoryNames.getOrDefault(transaction.getCategoryId(), "Sin categoria");
                expenseByCategory.merge(category, transaction.getMonto(), BigDecimal::add);
            }
        }

        BigDecimal balance = income.subtract(expenses).setScale(2, RoundingMode.HALF_UP);
        SavingsDtos.SavingsSummaryResponse savingsSummary = savings.summary(userId, workspaceId);
        return new AnalyticsDtos.SummaryResponse(
                income,
                expenses,
                balance,
                savingsSummary.tasaAhorro(),
                savingsSummary.ahorroTotal(),
                savingsSummary.saldoDisponibleNoAhorrado(),
                expenseByCategory);
    }
}
