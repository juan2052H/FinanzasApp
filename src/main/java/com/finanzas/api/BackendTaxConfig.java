package com.finanzas.api;

import java.io.Serializable;
import java.math.BigDecimal;

public final class BackendTaxConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String workspaceId;
    private final int taxYear;
    private final BigDecimal uvtValue;
    private final int grossIncomeUvt;
    private final int grossPurchasesUvt;
    private final int bankDepositsUvt;
    private final int grossWealthUvt;
    private final BigDecimal estimatedGrossWealth;
    private final String updatedAt;

    public BackendTaxConfig(String id,
                            String workspaceId,
                            int taxYear,
                            BigDecimal uvtValue,
                            int grossIncomeUvt,
                            int grossPurchasesUvt,
                            int bankDepositsUvt,
                            int grossWealthUvt,
                            BigDecimal estimatedGrossWealth,
                            String updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.taxYear = taxYear;
        this.uvtValue = uvtValue;
        this.grossIncomeUvt = grossIncomeUvt;
        this.grossPurchasesUvt = grossPurchasesUvt;
        this.bankDepositsUvt = bankDepositsUvt;
        this.grossWealthUvt = grossWealthUvt;
        this.estimatedGrossWealth = estimatedGrossWealth;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public int getTaxYear() { return taxYear; }
    public BigDecimal getUvtValue() { return uvtValue; }
    public int getGrossIncomeUvt() { return grossIncomeUvt; }
    public int getGrossPurchasesUvt() { return grossPurchasesUvt; }
    public int getBankDepositsUvt() { return bankDepositsUvt; }
    public int getGrossWealthUvt() { return grossWealthUvt; }
    public BigDecimal getEstimatedGrossWealth() { return estimatedGrossWealth; }
    public String getUpdatedAt() { return updatedAt; }
}
