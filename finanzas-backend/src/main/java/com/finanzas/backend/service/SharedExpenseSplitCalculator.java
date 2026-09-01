package com.finanzas.backend.service;

import com.finanzas.backend.domain.SplitMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SharedExpenseSplitCalculator {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.0000");

    public List<SplitAllocation> calculate(BigDecimal rawAmount, SplitMethod method, List<SplitInput> inputs, List<UUID> fallbackUserIds) {
        BigDecimal amount = money(rawAmount);
        if (amount.signum() <= 0) {
            throw badRequest("El monto del gasto compartido debe ser mayor a cero.");
        }
        SplitMethod selectedMethod = method == null ? SplitMethod.EQUAL : method;
        return switch (selectedMethod) {
            case EQUAL -> equal(amount, inputs, fallbackUserIds);
            case PERCENTAGE -> percentage(amount, inputs);
            case CUSTOM_AMOUNT -> customAmount(amount, inputs);
        };
    }

    private List<SplitAllocation> equal(BigDecimal amount, List<SplitInput> inputs, List<UUID> fallbackUserIds) {
        List<UUID> userIds = inputUserIds(inputs);
        if (userIds.isEmpty()) {
            userIds = new ArrayList<>(fallbackUserIds == null ? List.of() : fallbackUserIds);
        }
        requireParticipants(userIds);
        BigDecimal base = amount.divide(BigDecimal.valueOf(userIds.size()), 2, RoundingMode.DOWN);
        BigDecimal percentage = ONE_HUNDRED.divide(BigDecimal.valueOf(userIds.size()), 4, RoundingMode.HALF_UP);
        List<SplitAllocation> result = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        for (int index = 0; index < userIds.size(); index++) {
            BigDecimal splitAmount = index == userIds.size() - 1 ? amount.subtract(assigned) : base;
            assigned = assigned.add(splitAmount);
            result.add(new SplitAllocation(userIds.get(index), splitAmount, percentage));
        }
        return result;
    }

    private List<SplitAllocation> percentage(BigDecimal amount, List<SplitInput> inputs) {
        requireInputs(inputs);
        requireUnique(inputs.stream().map(SplitInput::userId).toList());
        BigDecimal totalPercentage = BigDecimal.ZERO;
        for (SplitInput input : inputs) {
            if (input.percentage() == null || input.percentage().signum() <= 0) {
                throw badRequest("Cada participante debe tener un porcentaje mayor a cero.");
            }
            totalPercentage = totalPercentage.add(input.percentage());
        }
        if (totalPercentage.setScale(4, RoundingMode.HALF_UP).compareTo(ONE_HUNDRED) != 0) {
            throw badRequest("Los porcentajes deben sumar exactamente 100%.");
        }

        List<SplitAllocation> result = new ArrayList<>();
        BigDecimal assigned = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        for (int index = 0; index < inputs.size(); index++) {
            SplitInput input = inputs.get(index);
            BigDecimal splitAmount = index == inputs.size() - 1
                    ? amount.subtract(assigned)
                    : money(amount.multiply(input.percentage()).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
            assigned = assigned.add(splitAmount);
            result.add(new SplitAllocation(input.userId(), splitAmount, input.percentage().setScale(4, RoundingMode.HALF_UP)));
        }
        return result;
    }

    private List<SplitAllocation> customAmount(BigDecimal amount, List<SplitInput> inputs) {
        requireInputs(inputs);
        requireUnique(inputs.stream().map(SplitInput::userId).toList());
        List<SplitAllocation> result = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY);
        for (SplitInput input : inputs) {
            if (input.amount() == null || input.amount().signum() < 0) {
                throw badRequest("Cada participante debe tener un monto valido.");
            }
            BigDecimal splitAmount = money(input.amount());
            total = total.add(splitAmount);
            BigDecimal percentage = amount.signum() == 0
                    ? BigDecimal.ZERO.setScale(4, RoundingMode.UNNECESSARY)
                    : splitAmount.multiply(ONE_HUNDRED).divide(amount, 4, RoundingMode.HALF_UP);
            result.add(new SplitAllocation(input.userId(), splitAmount, percentage));
        }
        if (total.compareTo(amount) != 0) {
            throw badRequest("La suma de los montos personalizados debe coincidir con el total.");
        }
        return result;
    }

    private List<UUID> inputUserIds(List<SplitInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return new ArrayList<>();
        }
        List<UUID> userIds = inputs.stream().map(SplitInput::userId).toList();
        requireUnique(userIds);
        return new ArrayList<>(userIds);
    }

    private void requireInputs(List<SplitInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            throw badRequest("Debes indicar participantes para este tipo de division.");
        }
    }

    private void requireParticipants(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw badRequest("El gasto compartido necesita al menos un participante.");
        }
        requireUnique(userIds);
    }

    private void requireUnique(List<UUID> userIds) {
        Set<UUID> seen = new HashSet<>();
        for (UUID userId : userIds) {
            if (userId == null) {
                throw badRequest("Los participantes no pueden estar vacios.");
            }
            if (!seen.add(userId)) {
                throw badRequest("No puedes repetir participantes en la division.");
            }
        }
    }

    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw badRequest("El monto es obligatorio.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record SplitInput(UUID userId, BigDecimal amount, BigDecimal percentage) {
    }

    public record SplitAllocation(UUID userId, BigDecimal amount, BigDecimal percentage) {
    }
}
