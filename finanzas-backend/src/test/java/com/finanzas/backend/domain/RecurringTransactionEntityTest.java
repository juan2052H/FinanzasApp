package com.finanzas.backend.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecurringTransactionEntityTest {
    @Test
    void monthlyRecurringTransactionAdvancesOneMonth() {
        RecurringTransactionEntity recurring = new RecurringTransactionEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TransactionType.EXPENSE,
                "Internet",
                new BigDecimal("120000"),
                RecurringFrequency.MONTHLY,
                null,
                LocalDate.of(2026, 8, 18));

        recurring.advanceNextRunDate();

        assertEquals(LocalDate.of(2026, 9, 18), recurring.getNextRunDate());
    }

    @Test
    void customRecurringTransactionAdvancesConfiguredDays() {
        RecurringTransactionEntity recurring = new RecurringTransactionEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TransactionType.EXPENSE,
                "Medicamento",
                new BigDecimal("45000"),
                RecurringFrequency.CUSTOM,
                10,
                LocalDate.of(2026, 8, 18));

        recurring.advanceNextRunDate();

        assertEquals(LocalDate.of(2026, 8, 28), recurring.getNextRunDate());
    }

    @Test
    void notificationCanBeMarkedAsRead() {
        NotificationEntity notification = new NotificationEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BUDGET_THRESHOLD",
                "Presupuesto",
                "Has utilizado 85%.");

        notification.markRead();

        assertNotNull(notification.getReadAt());
    }
}
