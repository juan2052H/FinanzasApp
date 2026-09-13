package com.finanzas.backend.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TaxDtos {
    private TaxDtos() {
    }

    public record TaxConfigurationRequest(
            BigDecimal uvtValue,
            Integer grossIncomeUvt,
            Integer grossPurchasesUvt,
            Integer bankDepositsUvt,
            Integer grossWealthUvt,
            BigDecimal estimatedGrossWealth) {
    }

    public record TaxConfigurationResponse(
            UUID id,
            UUID workspaceId,
            int taxYear,
            BigDecimal uvtValue,
            int grossIncomeUvt,
            int grossPurchasesUvt,
            int bankDepositsUvt,
            int grossWealthUvt,
            BigDecimal estimatedGrossWealth,
            Instant updatedAt) {
    }

    public record TaxPreparationSummaryResponse(
            UUID workspaceId,
            int taxYear,
            BigDecimal uvtValue,
            BigDecimal totalIncome,
            BigDecimal incomeThresholdAmount,
            boolean exceedsIncomeThreshold,
            BigDecimal totalExpenses,
            BigDecimal purchasesThresholdAmount,
            boolean exceedsPurchasesThreshold,
            BigDecimal totalBankDepositsOrSavings,
            BigDecimal depositsThresholdAmount,
            boolean exceedsDepositsThreshold,
            BigDecimal estimatedGrossWealth,
            BigDecimal wealthThresholdAmount,
            boolean exceedsWealthThreshold,
            boolean obligationToDeclare,
            List<String> obligationReasons,
            String legalDisclaimer) {
    }
}
