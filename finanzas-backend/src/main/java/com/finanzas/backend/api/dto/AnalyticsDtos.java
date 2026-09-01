package com.finanzas.backend.api.dto;

import java.math.BigDecimal;
import java.util.Map;

public final class AnalyticsDtos {
    private AnalyticsDtos() {
    }

    public record SummaryResponse(
            BigDecimal ingresos,
            BigDecimal gastos,
            BigDecimal balance,
            BigDecimal tasaAhorro,
            BigDecimal ahorroTotal,
            BigDecimal saldoDisponibleNoAhorrado,
            Map<String, BigDecimal> gastosPorCategoria) {
    }
}
