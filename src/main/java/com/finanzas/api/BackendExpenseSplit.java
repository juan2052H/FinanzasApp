package com.finanzas.api;

import java.math.BigDecimal;

public final class BackendExpenseSplit {
    private final String userId;
    private final String nombre;
    private final String email;
    private final BigDecimal amount;
    private final BigDecimal percentage;

    BackendExpenseSplit(String userId, String nombre, String email, BigDecimal amount, BigDecimal percentage) {
        this.userId = userId == null ? "" : userId;
        this.nombre = nombre == null ? "" : nombre;
        this.email = email == null ? "" : email;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.percentage = percentage == null ? BigDecimal.ZERO : percentage;
    }

    public String getUserId() { return userId; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getPercentage() { return percentage; }

    public String getDisplayName() {
        return nombre == null || nombre.trim().isEmpty() ? email : nombre.trim();
    }
}
