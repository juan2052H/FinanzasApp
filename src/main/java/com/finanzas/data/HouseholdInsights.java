package com.finanzas.data;

import com.finanzas.model.GastoHogar;
import com.finanzas.model.Money;
import com.finanzas.model.Settlement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class HouseholdInsights {
    private HouseholdInsights() {
    }

    static Map<String, Double> calcularDeudas(List<String> miembrosHogar, List<GastoHogar> gastosHogar) {
        return calcularDeudas(miembrosHogar, gastosHogar, java.util.Collections.<Settlement>emptyList());
    }

    static Map<String, Double> calcularDeudas(List<String> miembrosHogar, List<GastoHogar> gastosHogar, List<Settlement> settlements) {
        Map<String, BigDecimal> saldos = new LinkedHashMap<String, BigDecimal>();
        for (String miembro : miembrosHogar) {
            saldos.put(miembro, Money.ZERO);
        }

        for (GastoHogar gasto : gastosHogar) {
            if (gasto.isDividido()) {
                saldos.merge(gasto.getPagadoPor(), gasto.getMontoDecimal(), BigDecimal::add);
                Map<String, BigDecimal> splitAmounts = gasto.getSplitAmounts();
                if (splitAmounts.isEmpty()) {
                    BigDecimal parteIgual = miembrosHogar.isEmpty()
                            ? Money.ZERO
                            : gasto.getMontoDecimal().divide(BigDecimal.valueOf(miembrosHogar.size()), Money.SCALE, RoundingMode.HALF_UP);
                    for (String miembro : miembrosHogar) {
                        saldos.merge(miembro, parteIgual.negate(), BigDecimal::add);
                    }
                } else {
                    for (Map.Entry<String, BigDecimal> split : splitAmounts.entrySet()) {
                        saldos.merge(split.getKey(), Money.normalize(split.getValue()).negate(), BigDecimal::add);
                    }
                }
            }
        }

        if (settlements != null) {
            for (Settlement settlement : settlements) {
                saldos.merge(settlement.getFromMember(), settlement.getAmountDecimal(), BigDecimal::add);
                saldos.merge(settlement.getToMember(), settlement.getAmountDecimal().negate(), BigDecimal::add);
            }
        }

        Map<String, Double> result = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, BigDecimal> entry : saldos.entrySet()) {
            result.put(entry.getKey(), Money.toDouble(entry.getValue()));
        }
        return result;
    }
}
