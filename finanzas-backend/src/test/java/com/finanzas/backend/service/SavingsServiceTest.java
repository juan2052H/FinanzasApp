package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.SavingsDtos;
import com.finanzas.backend.domain.SavingsConfigEntity;
import com.finanzas.backend.domain.SavingsGoalEntity;
import com.finanzas.backend.domain.SavingsMovementEntity;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.SavingsConfigRepository;
import com.finanzas.backend.repo.SavingsGoalRepository;
import com.finanzas.backend.repo.SavingsMovementRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SavingsServiceTest {
    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final UUID incomeCategoryId = UUID.randomUUID();
    private final List<TransactionEntity> transactionStore = new ArrayList<>();
    private final List<SavingsMovementEntity> movementStore = new ArrayList<>();
    private final AtomicReference<SavingsConfigEntity> config = new AtomicReference<>();

    private SavingsGoalRepository goals;
    private SavingsService savings;

    @BeforeEach
    void setUp() {
        SavingsConfigRepository configs = mock(SavingsConfigRepository.class);
        SavingsMovementRepository movements = mock(SavingsMovementRepository.class);
        TransactionRepository transactions = mock(TransactionRepository.class);
        goals = mock(SavingsGoalRepository.class);
        WorkspaceAccessService access = mock(WorkspaceAccessService.class);
        AuditLogService auditLogs = mock(AuditLogService.class);

        when(access.requireMember(userId, workspaceId))
                .thenReturn(new WorkspaceMemberEntity(workspaceId, userId, WorkspaceRole.OWNER));

        when(configs.findByWorkspaceId(workspaceId)).thenAnswer(invocation -> Optional.ofNullable(config.get()));
        when(configs.findByWorkspaceIdForUpdate(workspaceId)).thenAnswer(invocation -> Optional.ofNullable(config.get()));
        when(configs.save(any(SavingsConfigEntity.class))).thenAnswer(invocation -> {
            SavingsConfigEntity saved = invocation.getArgument(0);
            config.set(saved);
            return saved;
        });

        when(movements.findByWorkspaceId(workspaceId)).thenAnswer(invocation -> new ArrayList<>(movementStore));
        when(movements.findByWorkspaceIdAndSourceTransactionId(eq(workspaceId), any(UUID.class))).thenAnswer(invocation -> {
            UUID sourceTransactionId = invocation.getArgument(1);
            return movementStore.stream()
                    .filter(movement -> sourceTransactionId.equals(movement.getSourceTransactionId()))
                    .toList();
        });
        when(movements.findByWorkspaceIdAndIdempotencyKey(eq(workspaceId), any(String.class))).thenAnswer(invocation -> {
            String key = invocation.getArgument(1);
            return movementStore.stream()
                    .filter(movement -> key.equals(movement.getIdempotencyKey()))
                    .findFirst();
        });
        when(movements.save(any(SavingsMovementEntity.class))).thenAnswer(invocation -> {
            SavingsMovementEntity saved = invocation.getArgument(0);
            setField(saved, "id", UUID.randomUUID());
            movementStore.add(saved);
            return saved;
        });

        when(transactions.findByWorkspaceIdOrderByTransactionDateDesc(workspaceId)).thenAnswer(invocation -> new ArrayList<>(transactionStore));
        when(transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(eq(workspaceId), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(1);
                    LocalDate to = invocation.getArgument(2);
                    return transactionStore.stream()
                            .filter(transaction -> !transaction.getTransactionDate().isBefore(from))
                            .filter(transaction -> !transaction.getTransactionDate().isAfter(to))
                            .toList();
                });

        savings = new SavingsService(configs, movements, transactions, goals, access, auditLogs);
    }

    @Test
    void sav001ToSav004AutomaticSavingsFollowIncomeLifecycle() {
        TransactionEntity income = transaction(TransactionType.INCOME, "1000.00");
        transactionStore.add(income);

        savings.recordAutomaticAllocationForCreatedIncome(userId, workspaceId, income);

        SavingsDtos.SavingsSummaryResponse sav001 = savings.summary(userId, workspaceId);
        assertEquals(new BigDecimal("200.00"), sav001.ahorroTotal());
        assertEquals(new BigDecimal("800.00"), sav001.saldoDisponibleNoAhorrado());

        transactionStore.add(transaction(TransactionType.EXPENSE, "100.00"));
        SavingsDtos.SavingsSummaryResponse sav002 = savings.summary(userId, workspaceId);
        assertEquals(new BigDecimal("200.00"), sav002.ahorroTotal());
        assertEquals(new BigDecimal("700.00"), sav002.saldoDisponibleNoAhorrado());

        income.update(incomeCategoryId, TransactionType.INCOME, "Salario editado", new BigDecimal("1500.00"), LocalDate.now());
        savings.reconcileAutomaticAllocationForUpdatedTransaction(
                userId,
                workspaceId,
                income.getId(),
                income.getType(),
                income.getMonto(),
                income.getTransactionDate());

        SavingsDtos.SavingsSummaryResponse sav003 = savings.summary(userId, workspaceId);
        assertEquals(new BigDecimal("300.00"), sav003.ahorroTotal());
        assertEquals(2, movementStore.stream().filter(movement -> income.getId().equals(movement.getSourceTransactionId())).count());

        savings.reverseAutomaticAllocationForDeletedTransaction(userId, workspaceId, income.getId());
        transactionStore.remove(income);

        SavingsDtos.SavingsSummaryResponse sav004 = savings.summary(userId, workspaceId);
        assertEquals(new BigDecimal("0.00"), sav004.ahorroTotal());
        assertEquals(new BigDecimal("0.00"), sourceBalance(income.getId()));
    }

    @Test
    void sav005WithdrawalReducesSavingsAndReturnsAvailableBalance() {
        TransactionEntity income = transaction(TransactionType.INCOME, "1000.00");
        transactionStore.add(income);
        savings.recordAutomaticAllocationForCreatedIncome(userId, workspaceId, income);

        savings.withdrawal(userId, workspaceId, new SavingsDtos.SavingsMovementRequest(
                new BigDecimal("50.00"),
                LocalDate.now(),
                "Retiro parcial",
                "ret-001"));

        SavingsDtos.SavingsSummaryResponse summary = savings.summary(userId, workspaceId);
        assertEquals(new BigDecimal("150.00"), summary.ahorroTotal());
        assertEquals(new BigDecimal("850.00"), summary.saldoDisponibleNoAhorrado());
    }

    @Test
    void sav006WithdrawalOverAvailableSavingsReturnsConflictAndDoesNotMutate() {
        TransactionEntity income = transaction(TransactionType.INCOME, "1000.00");
        transactionStore.add(income);
        savings.recordAutomaticAllocationForCreatedIncome(userId, workspaceId, income);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                savings.withdrawal(userId, workspaceId, new SavingsDtos.SavingsMovementRequest(
                        new BigDecimal("250.00"),
                        LocalDate.now(),
                        "Retiro excesivo",
                        "ret-002")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(new BigDecimal("200.00"), savings.summary(userId, workspaceId).ahorroTotal());
    }

    @Test
    void sav007SameIdempotencyKeyCreatesOneManualMovement() {
        SavingsDtos.SavingsMovementRequest request = new SavingsDtos.SavingsMovementRequest(
                new BigDecimal("25.00"),
                LocalDate.now(),
                "Deposito",
                "dep-001");

        savings.manualDeposit(userId, workspaceId, request);
        savings.manualDeposit(userId, workspaceId, request);

        assertEquals(1, movementStore.size());
        assertEquals(new BigDecimal("25.00"), savings.summary(userId, workspaceId).ahorroTotal());
    }

    @Test
    void sav009GoalAllocationCannotExceedFreeSavings() {
        TransactionEntity income = transaction(TransactionType.INCOME, "1000.00");
        transactionStore.add(income);
        savings.recordAutomaticAllocationForCreatedIncome(userId, workspaceId, income);
        SavingsGoalEntity goal = new SavingsGoalEntity(
                workspaceId,
                "Fondo",
                BigDecimal.ZERO,
                new BigDecimal("500.00"),
                "#1a73e8",
                "SEG",
                LocalDate.now().plusMonths(1));
        UUID goalId = UUID.randomUUID();
        setField(goal, "id", goalId);
        when(goals.findByIdAndWorkspaceId(goalId, workspaceId)).thenReturn(Optional.of(goal));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                savings.allocateGoal(userId, workspaceId, goalId, new SavingsDtos.GoalSavingsMovementRequest(
                        new BigDecimal("250.00"),
                        "Asignacion excesiva",
                        "goal-001")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals(new BigDecimal("0.00"), goal.getCurrentAmount());
    }

    @Test
    void automaticAllocationRoundsToCents() {
        TransactionEntity income = transaction(TransactionType.INCOME, "0.10");
        transactionStore.add(income);

        savings.recordAutomaticAllocationForCreatedIncome(userId, workspaceId, income);

        assertEquals(new BigDecimal("0.02"), savings.summary(userId, workspaceId).ahorroTotal());
    }

    private TransactionEntity transaction(TransactionType type, String amount) {
        TransactionEntity transaction = new TransactionEntity(
                workspaceId,
                incomeCategoryId,
                userId,
                type,
                "Movimiento",
                new BigDecimal(amount),
                LocalDate.now());
        setField(transaction, "id", UUID.randomUUID());
        return transaction;
    }

    private BigDecimal sourceBalance(UUID transactionId) {
        return movementStore.stream()
                .filter(movement -> transactionId.equals(movement.getSourceTransactionId()))
                .map(SavingsMovementEntity::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}
