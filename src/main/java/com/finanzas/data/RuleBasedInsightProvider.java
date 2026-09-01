package com.finanzas.data;

import com.finanzas.model.Presupuesto;

import java.util.ArrayList;
import java.util.List;

public final class RuleBasedInsightProvider implements InsightProvider {
    @Override
    public List<String> generateInsights(DataManager data) {
        List<String> insights = new ArrayList<String>();
        if (data.getIngresosMesActual() <= 0 && data.getGastosMesActual() <= 0) {
            insights.add("Registra tu primer ingreso para comenzar a calcular tendencias reales.");
            return insights;
        }
        if (data.getSaldoActual() < 0) {
            insights.add("Revisa las categorias con mayor gasto y prioriza recuperar saldo positivo.");
        }
        for (Presupuesto presupuesto : data.getPresupuestos()) {
            if (presupuesto.getPorcentajeUsado() >= 85) {
                insights.add("Tu categoria " + presupuesto.getCategoria() + " esta cerca del limite mensual.");
                break;
            }
        }
        insights.add(data.getSavingsRecommendationText());
        return insights;
    }
}
