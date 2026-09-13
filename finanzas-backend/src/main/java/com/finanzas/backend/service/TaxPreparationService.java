package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.SavingsDtos;
import com.finanzas.backend.api.dto.TaxDtos;
import com.finanzas.backend.domain.TaxConfigurationEntity;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.TaxConfigurationRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TaxPreparationService {
    private static final String DISCLAIMER =
            "Herramienta informativa de preparacion tributaria basada en parametros DIAN de personas naturales (E.T. Colombia). "
            + "No constituye declaracion oficial, liquidacion definitiva ni certificacion tributaria. "
            + "Consulta siempre con un Contador Publico o profesional tributario certificado.";

    private final TaxConfigurationRepository taxConfigs;
    private final TransactionRepository transactions;
    private final SavingsService savings;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;

    public TaxPreparationService(TaxConfigurationRepository taxConfigs,
                                 TransactionRepository transactions,
                                 SavingsService savings,
                                 WorkspaceAccessService access,
                                 AuditLogService auditLogs) {
        this.taxConfigs = taxConfigs;
        this.transactions = transactions;
        this.savings = savings;
        this.access = access;
        this.auditLogs = auditLogs;
    }

    @Transactional
    public TaxDtos.TaxConfigurationResponse configure(UUID userId, UUID workspaceId, int year, TaxDtos.TaxConfigurationRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        BigDecimal uvt = (request.uvtValue() != null && request.uvtValue().compareTo(BigDecimal.ZERO) > 0)
                ? request.uvtValue()
                : defaultUvtForYear(year);
        int grossIncomeUvt = request.grossIncomeUvt() != null ? request.grossIncomeUvt() : 1400;
        int grossPurchasesUvt = request.grossPurchasesUvt() != null ? request.grossPurchasesUvt() : 1400;
        int bankDepositsUvt = request.bankDepositsUvt() != null ? request.bankDepositsUvt() : 1400;
        int grossWealthUvt = request.grossWealthUvt() != null ? request.grossWealthUvt() : 4500;
        BigDecimal wealth = request.estimatedGrossWealth() != null ? request.estimatedGrossWealth() : BigDecimal.ZERO;

        TaxConfigurationEntity config = taxConfigs.findByWorkspaceIdAndTaxYear(workspaceId, year)
                .map(existing -> {
                    existing.update(uvt, grossIncomeUvt, grossPurchasesUvt, bankDepositsUvt, grossWealthUvt, wealth);
                    return existing;
                })
                .orElseGet(() -> taxConfigs.save(new TaxConfigurationEntity(
                        workspaceId, year, uvt, grossIncomeUvt, grossPurchasesUvt, bankDepositsUvt, grossWealthUvt, wealth)));

        auditLogs.record(workspaceId, userId, "TAX_CONFIG_UPDATED", "TaxConfiguration", config.getId(), Map.of(
                "year", Integer.toString(year),
                "uvt", config.getUvtValue().toPlainString()));
        return toConfigResponse(config);
    }

    @Transactional(readOnly = true)
    public TaxDtos.TaxPreparationSummaryResponse summary(UUID userId, UUID workspaceId, int year) {
        access.requireMember(userId, workspaceId);
        TaxConfigurationEntity config = taxConfigs.findByWorkspaceIdAndTaxYear(workspaceId, year)
                .orElseGet(() -> new TaxConfigurationEntity(
                        workspaceId,
                        year,
                        defaultUvtForYear(year),
                        1400,
                        1400,
                        1400,
                        4500,
                        BigDecimal.ZERO));

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        List<TransactionEntity> yearTransactions = transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                workspaceId, from, to);

        BigDecimal totalIncome = yearTransactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .map(TransactionEntity::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalExpenses = yearTransactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(TransactionEntity::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        SavingsDtos.SavingsSummaryResponse savingsSummary = savings.summary(userId, workspaceId, from, to);
        BigDecimal totalDeposits = savingsSummary.ahorroTotal().setScale(2, RoundingMode.HALF_UP);

        BigDecimal incomeThreshold = config.getUvtValue().multiply(BigDecimal.valueOf(config.getGrossIncomeUvt())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal purchasesThreshold = config.getUvtValue().multiply(BigDecimal.valueOf(config.getGrossPurchasesUvt())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal depositsThreshold = config.getUvtValue().multiply(BigDecimal.valueOf(config.getBankDepositsUvt())).setScale(2, RoundingMode.HALF_UP);
        BigDecimal wealthThreshold = config.getUvtValue().multiply(BigDecimal.valueOf(config.getGrossWealthUvt())).setScale(2, RoundingMode.HALF_UP);

        boolean exceedsIncome = totalIncome.compareTo(incomeThreshold) >= 0;
        boolean exceedsPurchases = totalExpenses.compareTo(purchasesThreshold) >= 0;
        boolean exceedsDeposits = totalDeposits.compareTo(depositsThreshold) >= 0;
        boolean exceedsWealth = config.getEstimatedGrossWealth().compareTo(wealthThreshold) >= 0;

        List<String> reasons = new ArrayList<>();
        if (exceedsIncome) {
            reasons.add("Ingresos brutos ($" + totalIncome + ") superan el tope de " + config.getGrossIncomeUvt() + " UVT ($" + incomeThreshold + ").");
        }
        if (exceedsPurchases) {
            reasons.add("Consumos y compras ($" + totalExpenses + ") superan el tope de " + config.getGrossPurchasesUvt() + " UVT ($" + purchasesThreshold + ").");
        }
        if (exceedsDeposits) {
            reasons.add("Consignaciones y depositos ($" + totalDeposits + ") superan el tope de " + config.getBankDepositsUvt() + " UVT ($" + depositsThreshold + ").");
        }
        if (exceedsWealth) {
            reasons.add("Patrimonio bruto estimado ($" + config.getEstimatedGrossWealth() + ") supera el tope de " + config.getGrossWealthUvt() + " UVT ($" + wealthThreshold + ").");
        }

        boolean obligated = !reasons.isEmpty();

        return new TaxDtos.TaxPreparationSummaryResponse(
                workspaceId,
                year,
                config.getUvtValue(),
                totalIncome,
                incomeThreshold,
                exceedsIncome,
                totalExpenses,
                purchasesThreshold,
                exceedsPurchases,
                totalDeposits,
                depositsThreshold,
                exceedsDeposits,
                config.getEstimatedGrossWealth(),
                wealthThreshold,
                exceedsWealth,
                obligated,
                reasons,
                DISCLAIMER);
    }

    public static BigDecimal defaultUvtForYear(int year) {
        if (year <= 2023) return new BigDecimal("42412.00");
        if (year == 2024) return new BigDecimal("47065.00");
        if (year == 2025) return new BigDecimal("49799.00");
        return new BigDecimal("52384.00"); // 2026+
    }

    private TaxDtos.TaxConfigurationResponse toConfigResponse(TaxConfigurationEntity entity) {
        return new TaxDtos.TaxConfigurationResponse(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getTaxYear(),
                entity.getUvtValue(),
                entity.getGrossIncomeUvt(),
                entity.getGrossPurchasesUvt(),
                entity.getBankDepositsUvt(),
                entity.getGrossWealthUvt(),
                entity.getEstimatedGrossWealth(),
                entity.getUpdatedAt());
    }
}
