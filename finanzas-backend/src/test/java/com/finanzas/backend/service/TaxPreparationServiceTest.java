package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.SavingsDtos;
import com.finanzas.backend.api.dto.TaxDtos;
import com.finanzas.backend.domain.TaxConfigurationEntity;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.repo.TaxConfigurationRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TaxPreparationServiceTest {
    private TaxConfigurationRepository taxConfigs;
    private TransactionRepository transactions;
    private SavingsService savings;
    private WorkspaceAccessService access;
    private AuditLogService auditLogs;
    private TaxPreparationService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        taxConfigs = mock(TaxConfigurationRepository.class);
        transactions = mock(TransactionRepository.class);
        savings = mock(SavingsService.class);
        access = mock(WorkspaceAccessService.class);
        auditLogs = mock(AuditLogService.class);
        service = new TaxPreparationService(taxConfigs, transactions, savings, access, auditLogs);
    }

    @Test
    void configureTaxParameters() {
        TaxDtos.TaxConfigurationRequest request = new TaxDtos.TaxConfigurationRequest(
                new BigDecimal("47065.00"), 1400, 1400, 1400, 4500, new BigDecimal("200000000.00"));

        when(taxConfigs.findByWorkspaceIdAndTaxYear(workspaceId, 2024)).thenReturn(Optional.empty());
        when(taxConfigs.save(any(TaxConfigurationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxDtos.TaxConfigurationResponse response = service.configure(userId, workspaceId, 2024, request);
        assertNotNull(response);
        assertEquals(2024, response.taxYear());
        assertEquals(new BigDecimal("47065.00"), response.uvtValue());
        assertEquals(new BigDecimal("200000000.00"), response.estimatedGrossWealth());
    }

    @Test
    void summaryDetectsObligationWhenIncomeExceedsThreshold() {
        TaxConfigurationEntity config = new TaxConfigurationEntity(
                workspaceId, 2024, new BigDecimal("47065.00"), 1400, 1400, 1400, 4500, BigDecimal.ZERO);
        when(taxConfigs.findByWorkspaceIdAndTaxYear(workspaceId, 2024)).thenReturn(Optional.of(config));

        // 1400 * 47065 = 65,891,000 COP threshold
        TransactionEntity incomeTx = new TransactionEntity(
                workspaceId, UUID.randomUUID(), userId, TransactionType.INCOME, "Salario anual",
                new BigDecimal("70000000.00"), LocalDate.of(2024, 6, 1));
        when(transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(eq(workspaceId), any(), any()))
                .thenReturn(List.of(incomeTx));

        SavingsDtos.SavingsSummaryResponse savingsSummary = new SavingsDtos.SavingsSummaryResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(savings.summary(eq(userId), eq(workspaceId), any(), any())).thenReturn(savingsSummary);

        TaxDtos.TaxPreparationSummaryResponse summary = service.summary(userId, workspaceId, 2024);
        assertNotNull(summary);
        assertTrue(summary.exceedsIncomeThreshold());
        assertTrue(summary.obligationToDeclare());
        assertEquals(1, summary.obligationReasons().size());
        assertTrue(summary.obligationReasons().get(0).contains("Ingresos brutos"));
        assertNotNull(summary.legalDisclaimer());
    }

    @Test
    void summaryUnderThresholdsNotObligated() {
        TaxConfigurationEntity config = new TaxConfigurationEntity(
                workspaceId, 2024, new BigDecimal("47065.00"), 1400, 1400, 1400, 4500, BigDecimal.ZERO);
        when(taxConfigs.findByWorkspaceIdAndTaxYear(workspaceId, 2024)).thenReturn(Optional.of(config));

        TransactionEntity incomeTx = new TransactionEntity(
                workspaceId, UUID.randomUUID(), userId, TransactionType.INCOME, "Salario",
                new BigDecimal("30000000.00"), LocalDate.of(2024, 6, 1));
        when(transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(eq(workspaceId), any(), any()))
                .thenReturn(List.of(incomeTx));

        SavingsDtos.SavingsSummaryResponse savingsSummary = new SavingsDtos.SavingsSummaryResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(savings.summary(eq(userId), eq(workspaceId), any(), any())).thenReturn(savingsSummary);

        TaxDtos.TaxPreparationSummaryResponse summary = service.summary(userId, workspaceId, 2024);
        assertNotNull(summary);
        assertFalse(summary.exceedsIncomeThreshold());
        assertFalse(summary.obligationToDeclare());
        assertTrue(summary.obligationReasons().isEmpty());
    }
}
