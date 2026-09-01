package com.finanzas.data;

import java.time.LocalDate;

public final class ReportPeriod {
    public static final String CURRENT_MONTH = "Mes actual";
    public static final String LAST_30_DAYS = "Ultimos 30 dias";
    public static final String LAST_6_MONTHS = "Ultimos 6 meses";
    public static final String LAST_12_MONTHS = "Ultimos 12 meses";
    public static final String ALL_HISTORY = "Todo el historial";
    public static final String CUSTOM = "Rango personalizado";

    private final String label;
    private final LocalDate startDate;
    private final LocalDate endDate;

    private ReportPeriod(String label, LocalDate startDate, LocalDate endDate) {
        this.label = label;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static String[] labels() {
        return new String[]{CURRENT_MONTH, LAST_30_DAYS, LAST_6_MONTHS, LAST_12_MONTHS, ALL_HISTORY, CUSTOM};
    }

    public static ReportPeriod fromSelection(String label, LocalDate customStart, LocalDate customEnd) {
        if (CUSTOM.equals(label)) {
            return custom(customStart, customEnd);
        }
        return fromLabel(label);
    }

    public static ReportPeriod fromLabel(String label) {
        LocalDate today = LocalDate.now();
        if (CURRENT_MONTH.equals(label)) {
            return new ReportPeriod(CURRENT_MONTH, today.withDayOfMonth(1), today);
        }
        if (LAST_30_DAYS.equals(label)) {
            return new ReportPeriod(LAST_30_DAYS, today.minusDays(29), today);
        }
        if (LAST_6_MONTHS.equals(label)) {
            return new ReportPeriod(LAST_6_MONTHS, today.minusMonths(5).withDayOfMonth(1), today);
        }
        if (LAST_12_MONTHS.equals(label)) {
            return new ReportPeriod(LAST_12_MONTHS, today.minusMonths(11).withDayOfMonth(1), today);
        }
        return new ReportPeriod(ALL_HISTORY, null, today);
    }

    public static ReportPeriod custom(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Ingresa fecha inicial y final para el rango personalizado.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
        return new ReportPeriod(CUSTOM, startDate, endDate);
    }

    public boolean contains(LocalDate date) {
        if (date == null) {
            return false;
        }
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    public String getLabel() {
        return label;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDisplayLabel() {
        if (CUSTOM.equals(label)) {
            return label + " (" + startDate + " a " + endDate + ")";
        }
        return label;
    }
}
