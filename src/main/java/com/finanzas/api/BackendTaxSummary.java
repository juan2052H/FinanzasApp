package com.finanzas.api;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public final class BackendTaxSummary {
    private final String workspaceId;
    private final int taxYear;
    private final BigDecimal uvtValue;
    private final BigDecimal totalIncome;
    private final BigDecimal incomeThresholdAmount;
    private final boolean exceedsIncomeThreshold;
    private final BigDecimal totalExpenses;
    private final BigDecimal purchasesThresholdAmount;
    private final boolean exceedsPurchasesThreshold;
    private final BigDecimal totalBankDepositsOrSavings;
    private final BigDecimal depositsThresholdAmount;
    private final boolean exceedsDepositsThreshold;
    private final BigDecimal estimatedGrossWealth;
    private final BigDecimal wealthThresholdAmount;
    private final boolean exceedsWealthThreshold;
    private final boolean obligationToDeclare;
    private final List<String> obligationReasons;
    private final String legalDisclaimer;

    public BackendTaxSummary(String workspaceId,
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
        this.workspaceId = workspaceId;
        this.taxYear = taxYear;
        this.uvtValue = uvtValue;
        this.totalIncome = totalIncome;
        this.incomeThresholdAmount = incomeThresholdAmount;
        this.exceedsIncomeThreshold = exceedsIncomeThreshold;
        this.totalExpenses = totalExpenses;
        this.purchasesThresholdAmount = purchasesThresholdAmount;
        this.exceedsPurchasesThreshold = exceedsPurchasesThreshold;
        this.totalBankDepositsOrSavings = totalBankDepositsOrSavings;
        this.depositsThresholdAmount = depositsThresholdAmount;
        this.exceedsDepositsThreshold = exceedsDepositsThreshold;
        this.estimatedGrossWealth = estimatedGrossWealth;
        this.wealthThresholdAmount = wealthThresholdAmount;
        this.exceedsWealthThreshold = exceedsWealthThreshold;
        this.obligationToDeclare = obligationToDeclare;
        this.obligationReasons = obligationReasons == null ? Collections.emptyList() : Collections.unmodifiableList(obligationReasons);
        this.legalDisclaimer = legalDisclaimer;
    }

    public String getWorkspaceId() { return workspaceId; }
    public int getTaxYear() { return taxYear; }
    public BigDecimal getUvtValue() { return uvtValue; }
    public BigDecimal getTotalIncome() { return totalIncome; }
    public BigDecimal getIncomeThresholdAmount() { return incomeThresholdAmount; }
    public boolean isExceedsIncomeThreshold() { return exceedsIncomeThreshold; }
    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public BigDecimal getPurchasesThresholdAmount() { return purchasesThresholdAmount; }
    public boolean isExceedsPurchasesThreshold() { return exceedsPurchasesThreshold; }
    public BigDecimal getTotalBankDepositsOrSavings() { return totalBankDepositsOrSavings; }
    public BigDecimal getDepositsThresholdAmount() { return depositsThresholdAmount; }
    public boolean isExceedsDepositsThreshold() { return exceedsDepositsThreshold; }
    public BigDecimal getEstimatedGrossWealth() { return estimatedGrossWealth; }
    public BigDecimal getWealthThresholdAmount() { return wealthThresholdAmount; }
    public boolean isExceedsWealthThreshold() { return exceedsWealthThreshold; }
    public boolean isObligationToDeclare() { return obligationToDeclare; }
    public List<String> getObligationReasons() { return obligationReasons; }
    public String getLegalDisclaimer() { return legalDisclaimer; }
}
