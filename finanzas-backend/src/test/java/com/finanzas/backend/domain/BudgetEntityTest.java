package com.finanzas.backend.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetEntityTest {
    @Test
    void constructorNormalizesPeriodMonthToFirstDayAndScalesAmount() {
        BudgetEntity budget = new BudgetEntity(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 3, 17), new BigDecimal("500000"));

        assertEquals(LocalDate.of(2026, 3, 1), budget.getPeriodMonth());
        assertEquals(new BigDecimal("500000.00"), budget.getAmount());
    }

    @Test
    void nullAmountNormalizesToZeroInsteadOfThrowing() {
        BudgetEntity budget = new BudgetEntity(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), null);

        assertEquals(new BigDecimal("0.00"), budget.getAmount());
    }

    @Test
    void negativeAmountIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new BudgetEntity(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), new BigDecimal("-0.01")));
    }

    @Test
    void updateAlsoRejectsNegativeAmountAndKeepsThePreviousOneUnchanged() {
        BudgetEntity budget = new BudgetEntity(UUID.randomUUID(), UUID.randomUUID(), LocalDate.now(), new BigDecimal("100.00"));

        assertThrows(IllegalArgumentException.class,
                () -> budget.update(budget.getCategoryId(), budget.getPeriodMonth(), new BigDecimal("-5.00")));
        assertEquals(new BigDecimal("100.00"), budget.getAmount());
    }
}
