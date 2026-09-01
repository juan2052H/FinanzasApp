package com.finanzas.data;

import java.math.BigDecimal;

public final class RecurringExpenseInsight {
    private final String description;
    private final String category;
    private final BigDecimal estimatedMonthlyAmount;
    private final BigDecimal estimatedAnnualAmount;
    private final int evidenceCount;

    public RecurringExpenseInsight(String description, String category, BigDecimal estimatedMonthlyAmount, int evidenceCount) {
        this.description = description;
        this.category = category;
        this.estimatedMonthlyAmount = estimatedMonthlyAmount;
        this.estimatedAnnualAmount = estimatedMonthlyAmount.multiply(BigDecimal.valueOf(12));
        this.evidenceCount = evidenceCount;
    }

    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getEstimatedMonthlyAmount() { return estimatedMonthlyAmount; }
    public BigDecimal getEstimatedAnnualAmount() { return estimatedAnnualAmount; }
    public int getEvidenceCount() { return evidenceCount; }
}
