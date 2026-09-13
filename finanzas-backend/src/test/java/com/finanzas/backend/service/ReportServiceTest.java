package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.ReportDtos;
import com.finanzas.backend.api.dto.SavingsDtos;
import com.finanzas.backend.domain.BudgetEntity;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.repo.BudgetRepository;
import com.finanzas.backend.repo.CategoryRepository;
import com.finanzas.backend.repo.RecurringTransactionRepository;
import com.finanzas.backend.repo.SavingsGoalRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {
    private TransactionRepository transactions;
    private CategoryRepository categories;
    private BudgetRepository budgets;
    private SavingsGoalRepository goals;
    private RecurringTransactionRepository recurringTransactions;
    private WorkspaceAccessService access;
    private AuditLogService auditLogs;
    private SavingsService savings;
    private ReportService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        transactions = mock(TransactionRepository.class);
        categories = mock(CategoryRepository.class);
        budgets = mock(BudgetRepository.class);
        goals = mock(SavingsGoalRepository.class);
        recurringTransactions = mock(RecurringTransactionRepository.class);
        access = mock(WorkspaceAccessService.class);
        auditLogs = mock(AuditLogService.class);
        savings = mock(SavingsService.class);
        service = new ReportService(transactions, categories, budgets, goals, recurringTransactions, access, auditLogs, savings);

        when(categories.findByWorkspaceIdAndArchivedFalseOrderByNombre(workspaceId)).thenReturn(List.of());
        when(goals.findByWorkspaceIdAndStatusNotOrderByDueDateAsc(eq(workspaceId), any())).thenReturn(List.of());
        when(recurringTransactions.findByWorkspaceIdAndActiveTrueOrderByNextRunDateAsc(workspaceId)).thenReturn(List.of());
        when(savings.summary(eq(userId), eq(workspaceId), any(), any())).thenReturn(zeroSavingsSummary());
    }

    @Test
    void overspendingByOneCentReportsOverOneHundredPercentAndNegativeAvailable() {
        UUID categoryId = UUID.randomUUID();
        LocalDate month = LocalDate.of(2026, 3, 1);
        BudgetEntity budget = new BudgetEntity(workspaceId, categoryId, month, new BigDecimal("100.00"));
        TransactionEntity expense = new TransactionEntity(workspaceId, categoryId, userId, TransactionType.EXPENSE,
                "Mercado", new BigDecimal("100.01"), LocalDate.of(2026, 3, 15));

        when(budgets.findByWorkspaceIdOrderByPeriodMonthDesc(workspaceId)).thenReturn(List.of(budget));
        when(transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(eq(workspaceId), any(), any()))
                .thenReturn(List.of(expense));

        ReportDtos.ReportResponse report = service.summary(userId, workspaceId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertEquals(1, report.presupuestos().size());
        ReportDtos.BudgetUsage usage = report.presupuestos().get(0);
        assertEquals(new BigDecimal("100.01"), usage.spent());
        assertEquals(new BigDecimal("-0.01"), usage.available());
        assertEquals(new BigDecimal("100.01"), usage.usagePercent());
    }

    @Test
    void zeroBudgetAmountDoesNotDivideByZero() {
        UUID categoryId = UUID.randomUUID();
        LocalDate month = LocalDate.of(2026, 3, 1);
        BudgetEntity budget = new BudgetEntity(workspaceId, categoryId, month, BigDecimal.ZERO);
        TransactionEntity expense = new TransactionEntity(workspaceId, categoryId, userId, TransactionType.EXPENSE,
                "Imprevisto", new BigDecimal("50.00"), LocalDate.of(2026, 3, 10));

        when(budgets.findByWorkspaceIdOrderByPeriodMonthDesc(workspaceId)).thenReturn(List.of(budget));
        when(transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(eq(workspaceId), any(), any()))
                .thenReturn(List.of(expense));

        ReportDtos.ReportResponse report = service.summary(userId, workspaceId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        ReportDtos.BudgetUsage usage = report.presupuestos().get(0);
        assertEquals(new BigDecimal("0.00"), usage.usagePercent());
    }

    @Test
    void spendingInADifferentMonthOrCategoryIsNotCountedAgainstTheBudget() {
        UUID categoryId = UUID.randomUUID();
        UUID otherCategoryId = UUID.randomUUID();
        BudgetEntity budget = new BudgetEntity(workspaceId, categoryId, LocalDate.of(2026, 3, 1), new BigDecimal("200.00"));

        TransactionEntity sameCategoryDifferentMonth = new TransactionEntity(workspaceId, categoryId, userId, TransactionType.EXPENSE,
                "Mes distinto", new BigDecimal("999.00"), LocalDate.of(2026, 2, 15));
        TransactionEntity differentCategorySameMonth = new TransactionEntity(workspaceId, otherCategoryId, userId, TransactionType.EXPENSE,
                "Categoria distinta", new BigDecimal("777.00"), LocalDate.of(2026, 3, 15));

        when(budgets.findByWorkspaceIdOrderByPeriodMonthDesc(workspaceId)).thenReturn(List.of(budget));
        when(transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(eq(workspaceId), any(), any()))
                .thenReturn(List.of(sameCategoryDifferentMonth, differentCategorySameMonth));

        ReportDtos.ReportResponse report = service.summary(userId, workspaceId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        ReportDtos.BudgetUsage usage = report.presupuestos().get(0);
        assertEquals(new BigDecimal("0.00"), usage.spent());
        assertEquals(new BigDecimal("200.00"), usage.available());
    }

    @Test
    void csvExportEscapesCommasAndQuotesInsideDescriptions() {
        UUID categoryId = UUID.randomUUID();
        TransactionEntity transaction = new TransactionEntity(workspaceId, categoryId, userId, TransactionType.EXPENSE,
                "Cafe, \"grande\"", new BigDecimal("15.00"), LocalDate.of(2026, 3, 5));

        when(budgets.findByWorkspaceIdOrderByPeriodMonthDesc(workspaceId)).thenReturn(List.of());
        when(transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(eq(workspaceId), any(), any()))
                .thenReturn(List.of(transaction));

        byte[] csvBytes = service.exportCsv(userId, workspaceId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Cafe, \"\"grande\"\"\""),
                "the whole field must stay quoted and internal quotes must be doubled: " + csv);
    }

    @Test
    void pdfAndXlsxExportsDoNotThrowWithNoTransactionsBudgetsOrGoals() {
        when(budgets.findByWorkspaceIdOrderByPeriodMonthDesc(workspaceId)).thenReturn(List.of());
        when(transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(eq(workspaceId), any(), any()))
                .thenReturn(List.of());

        byte[] pdf = service.exportPdf(userId, workspaceId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        byte[] xlsx = service.exportXlsx(userId, workspaceId, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

        assertTrue(pdf.length > 0);
        assertTrue(xlsx.length > 0);
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).startsWith("%PDF-1.4"));
    }

    private static SavingsDtos.SavingsSummaryResponse zeroSavingsSummary() {
        return new SavingsDtos.SavingsSummaryResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
