package com.finanzas.backend.service;

import com.finanzas.backend.domain.SplitMethod;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SharedExpenseSplitCalculatorTest {
    private final SharedExpenseSplitCalculator calculator = new SharedExpenseSplitCalculator();

    @Test
    void equalSplitDistributesExactCents() {
        UUID juan = UUID.randomUUID();
        UUID ana = UUID.randomUUID();
        UUID luis = UUID.randomUUID();
        UUID sara = UUID.randomUUID();

        List<SharedExpenseSplitCalculator.SplitAllocation> result = calculator.calculate(
                new BigDecimal("120000.00"),
                SplitMethod.EQUAL,
                List.of(),
                List.of(juan, ana, luis, sara));

        assertEquals(4, result.size());
        result.forEach(split -> assertEquals(new BigDecimal("30000.00"), split.amount()));
    }

    @Test
    void percentageSplitKeepsTotalExactAfterRounding() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        List<SharedExpenseSplitCalculator.SplitAllocation> result = calculator.calculate(
                new BigDecimal("100000.01"),
                SplitMethod.PERCENTAGE,
                List.of(
                        new SharedExpenseSplitCalculator.SplitInput(a, null, new BigDecimal("33.3333")),
                        new SharedExpenseSplitCalculator.SplitInput(b, null, new BigDecimal("33.3333")),
                        new SharedExpenseSplitCalculator.SplitInput(c, null, new BigDecimal("33.3334"))),
                List.of());

        BigDecimal total = result.stream()
                .map(SharedExpenseSplitCalculator.SplitAllocation::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("100000.01"), total);
    }

    @Test
    void customAmountRejectsMismatchWithTotal() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();

        assertThrows(ResponseStatusException.class, () -> calculator.calculate(
                new BigDecimal("50000.00"),
                SplitMethod.CUSTOM_AMOUNT,
                List.of(
                        new SharedExpenseSplitCalculator.SplitInput(a, new BigDecimal("20000.00"), null),
                        new SharedExpenseSplitCalculator.SplitInput(b, new BigDecimal("20000.00"), null)),
                List.of()));
    }

    @Test
    void duplicateParticipantsAreRejected() {
        UUID userId = UUID.randomUUID();

        assertThrows(ResponseStatusException.class, () -> calculator.calculate(
                new BigDecimal("50000.00"),
                SplitMethod.EQUAL,
                List.of(
                        new SharedExpenseSplitCalculator.SplitInput(userId, null, null),
                        new SharedExpenseSplitCalculator.SplitInput(userId, null, null)),
                List.of()));
    }
}
