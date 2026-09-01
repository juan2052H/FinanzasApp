package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.SavingsDtos;
import com.finanzas.backend.domain.SavingsConfigEntity;
import com.finanzas.backend.domain.SavingsMovementDirection;
import com.finanzas.backend.domain.SavingsMovementEntity;
import com.finanzas.backend.domain.SavingsMovementType;
import com.finanzas.backend.domain.SavingsGoalEntity;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.SavingsConfigRepository;
import com.finanzas.backend.repo.SavingsGoalRepository;
import com.finanzas.backend.repo.SavingsMovementRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SavingsService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    private final SavingsConfigRepository configs;
    private final SavingsMovementRepository movements;
    private final TransactionRepository transactions;
    private final SavingsGoalRepository goals;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;

    public SavingsService(SavingsConfigRepository configs,
                          SavingsMovementRepository movements,
                          TransactionRepository transactions,
                          SavingsGoalRepository goals,
                          WorkspaceAccessService access,
                          AuditLogService auditLogs) {
        this.configs = configs;
        this.movements = movements;
        this.transactions = transactions;
        this.goals = goals;
        this.access = access;
        this.auditLogs = auditLogs;
    }

    @Transactional
    public SavingsDtos.SavingsConfigResponse config(UUID userId, UUID workspaceId) {
        access.requireMember(userId, workspaceId);
        return toConfigResponse(ensureConfig(workspaceId, userId));
    }

    @Transactional
    public SavingsDtos.SavingsConfigResponse updateConfig(UUID userId, UUID workspaceId, SavingsDtos.SavingsConfigRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        SavingsConfigEntity config = ensureConfigForUpdate(workspaceId, userId);
        config.update(request.enabled(), request.percentage(), request.effectiveFrom(), userId);
        auditLogs.record(workspaceId, userId, "SAVINGS_CONFIG_UPDATED", "SavingsConfig", config.getId(), Map.of(
                "enabled", Boolean.toString(config.isEnabled()),
                "percentage", config.getPercentage().toPlainString()));
        return toConfigResponse(config);
    }

    @Transactional(readOnly = true)
    public SavingsDtos.SavingsSummaryResponse summary(UUID userId, UUID workspaceId) {
        access.requireMember(userId, workspaceId);
        return summarize(workspaceId, null, null);
    }

    @Transactional(readOnly = true)
    public SavingsDtos.SavingsSummaryResponse summary(UUID userId, UUID workspaceId, LocalDate from, LocalDate to) {
        access.requireMember(userId, workspaceId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango de fechas es invalido.");
        }
        return summarize(workspaceId, from, to);
    }

    @Transactional(readOnly = true)
    public SavingsDtos.SavingsMovementPageResponse listMovements(UUID userId, UUID workspaceId, int page, int size) {
        access.requireMember(userId, workspaceId);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<SavingsMovementEntity> result = movements.findByWorkspaceIdOrderByEffectiveDateDescCreatedAtDesc(workspaceId, pageable);
        return new SavingsDtos.SavingsMovementPageResponse(
                result.getContent().stream().map(this::toMovementResponse).toList(),
                safePage,
                safeSize,
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public SavingsDtos.SavingsMovementResponse manualDeposit(UUID userId, UUID workspaceId, SavingsDtos.SavingsMovementRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        ensureConfigForUpdate(workspaceId, userId);
        SavingsMovementEntity movement = recordMovement(
                workspaceId,
                userId,
                null,
                null,
                SavingsMovementType.MANUAL_DEPOSIT,
                SavingsMovementDirection.CREDIT,
                request.amount(),
                request.effectiveDate(),
                request.note(),
                idempotencyKey("manual-deposit", request.idempotencyKey()),
                null);
        auditLogs.record(workspaceId, userId, "SAVINGS_MANUAL_DEPOSIT", "SavingsMovement", movement.getId(), Map.of(
                "amount", movement.getAmount().toPlainString()));
        return toMovementResponse(movement);
    }

    @Transactional
    public SavingsDtos.SavingsMovementResponse withdrawal(UUID userId, UUID workspaceId, SavingsDtos.SavingsMovementRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        ensureConfigForUpdate(workspaceId, userId);
        BigDecimal amount = money(request.amount());
        SavingsDtos.SavingsSummaryResponse summary = summarize(workspaceId, null, null);
        if (summary.ahorroLibre().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El retiro supera el ahorro libre disponible.");
        }
        SavingsMovementEntity movement = recordMovement(
                workspaceId,
                userId,
                null,
                null,
                SavingsMovementType.WITHDRAWAL,
                SavingsMovementDirection.DEBIT,
                amount,
                request.effectiveDate(),
                request.note(),
                idempotencyKey("withdrawal", request.idempotencyKey()),
                null);
        auditLogs.record(workspaceId, userId, "SAVINGS_WITHDRAWAL", "SavingsMovement", movement.getId(), Map.of(
                "amount", movement.getAmount().toPlainString()));
        return toMovementResponse(movement);
    }

    @Transactional
    public SavingsDtos.SavingsMovementResponse allocateGoal(UUID userId, UUID workspaceId, UUID goalId, SavingsDtos.GoalSavingsMovementRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        ensureConfigForUpdate(workspaceId, userId);
        SavingsGoalEntity goal = requireGoal(workspaceId, goalId);
        BigDecimal amount = money(request.amount());
        SavingsDtos.SavingsSummaryResponse summary = summarize(workspaceId, null, null);
        if (summary.ahorroLibre().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La meta no puede superar el ahorro libre disponible.");
        }
        if (goal.getTargetAmount().subtract(goal.getCurrentAmount()).compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La asignacion supera el faltante de la meta.");
        }
        SavingsMovementEntity movement = recordMovement(
                workspaceId,
                userId,
                null,
                goalId,
                SavingsMovementType.GOAL_ALLOCATION,
                SavingsMovementDirection.CREDIT,
                amount,
                LocalDate.now(),
                request.note(),
                idempotencyKey("goal-allocation-" + goalId, request.idempotencyKey()),
                null);
        goal.allocate(amount);
        auditLogs.record(workspaceId, userId, "SAVINGS_GOAL_ALLOCATED", "SavingsMovement", movement.getId(), Map.of(
                "amount", movement.getAmount().toPlainString(),
                "goalId", goalId.toString()));
        return toMovementResponse(movement);
    }

    @Transactional
    public SavingsDtos.SavingsMovementResponse releaseGoal(UUID userId, UUID workspaceId, UUID goalId, SavingsDtos.GoalSavingsMovementRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        ensureConfigForUpdate(workspaceId, userId);
        SavingsGoalEntity goal = requireGoal(workspaceId, goalId);
        BigDecimal amount = money(request.amount());
        if (goal.getCurrentAmount().compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes liberar mas de lo asignado a la meta.");
        }
        SavingsMovementEntity movement = recordMovement(
                workspaceId,
                userId,
                null,
                goalId,
                SavingsMovementType.GOAL_RELEASE,
                SavingsMovementDirection.DEBIT,
                amount,
                LocalDate.now(),
                request.note(),
                idempotencyKey("goal-release-" + goalId, request.idempotencyKey()),
                null);
        goal.release(amount);
        auditLogs.record(workspaceId, userId, "SAVINGS_GOAL_RELEASED", "SavingsMovement", movement.getId(), Map.of(
                "amount", movement.getAmount().toPlainString(),
                "goalId", goalId.toString()));
        return toMovementResponse(movement);
    }

    @Transactional(readOnly = true)
    public SavingsDtos.IncomeImpactResponse incomeImpact(UUID userId, UUID workspaceId, BigDecimal amount, LocalDate date) {
        access.requireMember(userId, workspaceId);
        SavingsConfigEntity config = configs.findByWorkspaceId(workspaceId)
                .orElseGet(() -> new SavingsConfigEntity(workspaceId, userId));
        BigDecimal income = money(amount);
        BigDecimal automatic = config.isEnabled() ? calculateAllocation(income, config.getPercentage()) : money(BigDecimal.ZERO);
        return new SavingsDtos.IncomeImpactResponse(
                income,
                config.getPercentage(),
                automatic,
                money(income.subtract(automatic)));
    }

    public void recordAutomaticAllocationForCreatedIncome(UUID userId, UUID workspaceId, TransactionEntity transaction) {
        if (transaction.getType() != TransactionType.INCOME) {
            return;
        }
        SavingsConfigEntity config = ensureConfigForUpdate(workspaceId, userId);
        if (!config.isEnabled()) {
            return;
        }
        BigDecimal amount = calculateAllocation(transaction.getMonto(), config.getPercentage());
        if (amount.signum() <= 0) {
            return;
        }
        recordMovement(
                workspaceId,
                userId,
                transaction.getId(),
                null,
                SavingsMovementType.AUTO_ALLOCATION,
                SavingsMovementDirection.CREDIT,
                amount,
                transaction.getTransactionDate(),
                "Ahorro automatico por ingreso",
                "transaction:" + transaction.getId() + ":auto-allocation",
                null);
    }

    public void reconcileAutomaticAllocationForUpdatedTransaction(UUID userId,
                                                                  UUID workspaceId,
                                                                  UUID transactionId,
                                                                  TransactionType newType,
                                                                  BigDecimal newAmount,
                                                                  LocalDate newDate) {
        SavingsConfigEntity config = ensureConfigForUpdate(workspaceId, userId);
        BigDecimal currentAllocation = sourceTransactionBalance(workspaceId, transactionId);
        BigDecimal desiredAllocation = newType == TransactionType.INCOME && config.isEnabled()
                ? calculateAllocation(newAmount, config.getPercentage())
                : money(BigDecimal.ZERO);
        BigDecimal delta = money(desiredAllocation.subtract(currentAllocation));
        if (delta.signum() == 0) {
            return;
        }
        SavingsMovementDirection direction = delta.signum() > 0 ? SavingsMovementDirection.CREDIT : SavingsMovementDirection.DEBIT;
        SavingsMovementType type = delta.signum() > 0 ? SavingsMovementType.ADJUSTMENT : SavingsMovementType.REVERSAL;
        recordMovement(
                workspaceId,
                userId,
                transactionId,
                null,
                type,
                direction,
                delta.abs(),
                newDate,
                "Ajuste de ahorro por edicion de ingreso",
                "transaction:" + transactionId + ":reconcile:" + desiredAllocation.toPlainString(),
                null);
    }

    public void reverseAutomaticAllocationForDeletedTransaction(UUID userId, UUID workspaceId, UUID transactionId) {
        ensureConfigForUpdate(workspaceId, userId);
        BigDecimal currentAllocation = sourceTransactionBalance(workspaceId, transactionId);
        if (currentAllocation.signum() <= 0) {
            return;
        }
        recordMovement(
                workspaceId,
                userId,
                transactionId,
                null,
                SavingsMovementType.REVERSAL,
                SavingsMovementDirection.DEBIT,
                currentAllocation,
                LocalDate.now(),
                "Reversion de ahorro por eliminacion de ingreso",
                "transaction:" + transactionId + ":delete-reversal",
                null);
    }

    private SavingsConfigEntity ensureConfig(UUID workspaceId, UUID userId) {
        return configs.findByWorkspaceId(workspaceId)
                .orElseGet(() -> configs.save(new SavingsConfigEntity(workspaceId, userId)));
    }

    private SavingsConfigEntity ensureConfigForUpdate(UUID workspaceId, UUID userId) {
        return configs.findByWorkspaceIdForUpdate(workspaceId)
                .orElseGet(() -> configs.save(new SavingsConfigEntity(workspaceId, userId)));
    }

    private SavingsGoalEntity requireGoal(UUID workspaceId, UUID goalId) {
        return goals.findByIdAndWorkspaceId(goalId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta no encontrada."));
    }

    private SavingsMovementEntity recordMovement(UUID workspaceId,
                                                 UUID userId,
                                                 UUID sourceTransactionId,
                                                 UUID goalId,
                                                 SavingsMovementType type,
                                                 SavingsMovementDirection direction,
                                                 BigDecimal amount,
                                                 LocalDate effectiveDate,
                                                 String note,
                                                 String idempotencyKey,
                                                 UUID reversedMovementId) {
        String key = idempotencyKey(idempotencyKey);
        return movements.findByWorkspaceIdAndIdempotencyKey(workspaceId, key)
                .orElseGet(() -> saveMovement(workspaceId, userId, sourceTransactionId, goalId, type, direction, amount, effectiveDate, note, key, reversedMovementId));
    }

    private SavingsMovementEntity saveMovement(UUID workspaceId,
                                               UUID userId,
                                               UUID sourceTransactionId,
                                               UUID goalId,
                                               SavingsMovementType type,
                                               SavingsMovementDirection direction,
                                               BigDecimal amount,
                                               LocalDate effectiveDate,
                                               String note,
                                               String key,
                                               UUID reversedMovementId) {
        try {
            return movements.save(new SavingsMovementEntity(
                    workspaceId,
                    userId,
                    sourceTransactionId,
                    goalId,
                    type,
                    direction,
                    amount,
                    effectiveDate,
                    note,
                    key,
                    reversedMovementId));
        } catch (DataIntegrityViolationException ex) {
            return movements.findByWorkspaceIdAndIdempotencyKey(workspaceId, key)
                    .orElseThrow(() -> ex);
        }
    }

    private BigDecimal sourceTransactionBalance(UUID workspaceId, UUID transactionId) {
        return money(movements.findByWorkspaceIdAndSourceTransactionId(workspaceId, transactionId).stream()
                .filter(this::affectsSavingsBalance)
                .map(SavingsMovementEntity::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private SavingsDtos.SavingsSummaryResponse summarize(UUID workspaceId, LocalDate from, LocalDate to) {
        List<TransactionEntity> workspaceTransactions = (from == null && to == null)
                ? transactions.findByWorkspaceIdOrderByTransactionDateDesc(workspaceId)
                : transactions.findByWorkspaceIdAndTransactionDateBetweenOrderByTransactionDateDesc(
                        workspaceId,
                        from == null ? LocalDate.of(1970, 1, 1) : from,
                        to == null ? LocalDate.now().plusYears(20) : to);
        BigDecimal income = sumTransactions(workspaceTransactions, TransactionType.INCOME);
        BigDecimal expenses = sumTransactions(workspaceTransactions, TransactionType.EXPENSE);
        List<SavingsMovementEntity> workspaceMovements = movements.findByWorkspaceId(workspaceId).stream()
                .filter(movement -> from == null || !movement.getEffectiveDate().isBefore(from))
                .filter(movement -> to == null || !movement.getEffectiveDate().isAfter(to))
                .toList();
        BigDecimal totalSavings = money(workspaceMovements.stream()
                .filter(this::affectsSavingsBalance)
                .map(SavingsMovementEntity::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal assignedToGoals = money(workspaceMovements.stream()
                .filter(this::affectsGoalAllocation)
                .map(SavingsMovementEntity::signedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal withdrawals = money(workspaceMovements.stream()
                .filter(movement -> movement.getType() == SavingsMovementType.WITHDRAWAL)
                .map(SavingsMovementEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal freeSavings = money(totalSavings.subtract(assignedToGoals)).max(BigDecimal.ZERO.setScale(2));
        BigDecimal availableOutsideSavings = money(income.subtract(expenses).subtract(totalSavings));
        BigDecimal savingsRate = income.signum() <= 0
                ? money(BigDecimal.ZERO)
                : totalSavings.multiply(ONE_HUNDRED).divide(income, 2, RoundingMode.HALF_UP);
        return new SavingsDtos.SavingsSummaryResponse(
                income,
                expenses,
                totalSavings,
                assignedToGoals,
                freeSavings,
                withdrawals,
                availableOutsideSavings,
                savingsRate);
    }

    private boolean affectsSavingsBalance(SavingsMovementEntity movement) {
        return movement.getType() == SavingsMovementType.AUTO_ALLOCATION
                || movement.getType() == SavingsMovementType.MANUAL_DEPOSIT
                || movement.getType() == SavingsMovementType.WITHDRAWAL
                || movement.getType() == SavingsMovementType.ADJUSTMENT
                || movement.getType() == SavingsMovementType.REVERSAL;
    }

    private boolean affectsGoalAllocation(SavingsMovementEntity movement) {
        return movement.getType() == SavingsMovementType.GOAL_ALLOCATION
                || movement.getType() == SavingsMovementType.GOAL_RELEASE;
    }

    private BigDecimal sumTransactions(List<TransactionEntity> workspaceTransactions, TransactionType type) {
        return money(workspaceTransactions.stream()
                .filter(transaction -> transaction.getType() == type)
                .map(TransactionEntity::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private SavingsDtos.SavingsConfigResponse toConfigResponse(SavingsConfigEntity config) {
        return new SavingsDtos.SavingsConfigResponse(
                config.getWorkspaceId(),
                config.isEnabled(),
                config.getAllocationMode(),
                config.getPercentage(),
                config.getEffectiveFrom(),
                config.getVersion());
    }

    private SavingsDtos.SavingsMovementResponse toMovementResponse(SavingsMovementEntity movement) {
        return new SavingsDtos.SavingsMovementResponse(
                movement.getId(),
                movement.getWorkspaceId(),
                movement.getCreatedByUserId(),
                movement.getSourceTransactionId(),
                movement.getGoalId(),
                movement.getType(),
                movement.getDirection(),
                movement.getAmount(),
                movement.getEffectiveDate(),
                movement.getNote(),
                movement.getIdempotencyKey(),
                movement.getReversedMovementId(),
                movement.getCreatedAt());
    }

    private BigDecimal calculateAllocation(BigDecimal amount, BigDecimal percentage) {
        return money(money(amount).multiply(SavingsConfigEntity.normalizePercentage(percentage)).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP));
    }

    private BigDecimal money(BigDecimal amount) {
        return (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
    }

    private String idempotencyKey(String prefix, String providedKey) {
        return prefix + ":" + idempotencyKey(providedKey);
    }

    private String idempotencyKey(String providedKey) {
        if (providedKey == null || providedKey.trim().isEmpty()) {
            return "generated:" + UUID.randomUUID();
        }
        return providedKey.trim();
    }
}
