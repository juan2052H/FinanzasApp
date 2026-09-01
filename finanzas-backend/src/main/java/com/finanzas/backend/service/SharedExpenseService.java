package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.HouseholdDtos;
import com.finanzas.backend.domain.CategoryEntity;
import com.finanzas.backend.domain.ExpenseSplitEntity;
import com.finanzas.backend.domain.SettlementEntity;
import com.finanzas.backend.domain.SharedExpenseEntity;
import com.finanzas.backend.domain.SharedExpenseStatus;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.CategoryRepository;
import com.finanzas.backend.repo.ExpenseSplitRepository;
import com.finanzas.backend.repo.SettlementRepository;
import com.finanzas.backend.repo.SharedExpenseRepository;
import com.finanzas.backend.repo.UserRepository;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SharedExpenseService {
    private final SharedExpenseRepository sharedExpenses;
    private final ExpenseSplitRepository splits;
    private final SettlementRepository settlements;
    private final WorkspaceMemberRepository members;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;
    private final SharedExpenseSplitCalculator splitCalculator = new SharedExpenseSplitCalculator();

    public SharedExpenseService(SharedExpenseRepository sharedExpenses,
                                ExpenseSplitRepository splits,
                                SettlementRepository settlements,
                                WorkspaceMemberRepository members,
                                CategoryRepository categories,
                                UserRepository users,
                                WorkspaceAccessService access,
                                AuditLogService auditLogs) {
        this.sharedExpenses = sharedExpenses;
        this.splits = splits;
        this.settlements = settlements;
        this.members = members;
        this.categories = categories;
        this.users = users;
        this.access = access;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<HouseholdDtos.SharedExpenseResponse> list(UUID actorUserId, UUID workspaceId, SharedExpenseStatus status) {
        access.requireMember(actorUserId, workspaceId);
        List<SharedExpenseEntity> expenses = status == null
                ? sharedExpenses.findByWorkspaceIdOrderByExpenseDateDescCreatedAtDesc(workspaceId)
                : sharedExpenses.findByWorkspaceIdAndStatusOrderByExpenseDateDescCreatedAtDesc(workspaceId, status);
        return toExpenseResponses(expenses);
    }

    @Transactional
    public HouseholdDtos.SharedExpenseResponse create(UUID actorUserId, UUID workspaceId, HouseholdDtos.SharedExpenseRequest request) {
        requireFinancialWrite(actorUserId, workspaceId);
        Map<UUID, WorkspaceMemberEntity> membersById = membersByWorkspace(workspaceId);
        UUID paidByUserId = request.paidByUserId() == null ? actorUserId : request.paidByUserId();
        if (!membersById.containsKey(paidByUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quien pago debe pertenecer al workspace.");
        }
        UUID categoryId = validateCategory(workspaceId, request.categoryId());
        BigDecimal amount = SharedExpenseSplitCalculator.money(request.amount());
        List<SharedExpenseSplitCalculator.SplitInput> inputs = toSplitInputs(request.participants());
        List<SharedExpenseSplitCalculator.SplitAllocation> allocations = splitCalculator.calculate(
                amount,
                request.splitMethod(),
                inputs,
                new ArrayList<>(membersById.keySet()));
        ensureParticipantsBelongToWorkspace(allocations, membersById);

        SharedExpenseEntity expense = sharedExpenses.save(new SharedExpenseEntity(
                workspaceId,
                paidByUserId,
                categoryId,
                request.description(),
                amount,
                request.date(),
                request.splitMethod()));
        List<ExpenseSplitEntity> savedSplits = splits.saveAll(allocations.stream()
                .map(allocation -> new ExpenseSplitEntity(expense.getId(), allocation.userId(), allocation.amount(), allocation.percentage()))
                .toList());
        auditLogs.record(workspaceId, actorUserId, "SHARED_EXPENSE_CREATED", "SharedExpense", expense.getId(), Map.of(
                "amount", amount.toPlainString(),
                "paidByUserId", paidByUserId.toString(),
                "splitMethod", expense.getSplitMethod().name(),
                "participants", savedSplits.size()));
        return toExpenseResponse(expense, savedSplits);
    }

    @Transactional
    public void cancel(UUID actorUserId, UUID workspaceId, UUID sharedExpenseId) {
        requireFinancialWrite(actorUserId, workspaceId);
        SharedExpenseEntity expense = sharedExpenses.findByIdAndWorkspaceId(sharedExpenseId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gasto compartido no encontrado."));
        expense.cancel();
        auditLogs.record(workspaceId, actorUserId, "SHARED_EXPENSE_CANCELLED", "SharedExpense", sharedExpenseId, Map.of(
                "amount", expense.getAmount().toPlainString()));
    }

    @Transactional(readOnly = true)
    public List<HouseholdDtos.MemberBalanceResponse> balances(UUID actorUserId, UUID workspaceId) {
        access.requireMember(actorUserId, workspaceId);
        Map<UUID, WorkspaceMemberEntity> membersById = membersByWorkspace(workspaceId);
        Map<UUID, Balance> balances = new LinkedHashMap<>();
        membersById.keySet().forEach(userId -> balances.put(userId, new Balance(userId)));

        List<SharedExpenseEntity> openExpenses = sharedExpenses.findByWorkspaceIdAndStatusOrderByExpenseDateDescCreatedAtDesc(workspaceId, SharedExpenseStatus.OPEN);
        Map<UUID, SharedExpenseEntity> expensesById = openExpenses.stream()
                .collect(Collectors.toMap(SharedExpenseEntity::getId, Function.identity()));
        if (!openExpenses.isEmpty()) {
            splits.findBySharedExpenseIdIn(openExpenses.stream().map(SharedExpenseEntity::getId).toList())
                    .forEach(split -> applyBalance(split, expensesById.get(split.getSharedExpenseId()), balances));
        }

        Map<UUID, UserView> usersById = usersById(balances.keySet());
        return balances.values().stream()
                .map(balance -> balance.toResponse(usersById.get(balance.userId)))
                .sorted(Comparator.comparing(HouseholdDtos.MemberBalanceResponse::nombre))
                .toList();
    }

    @Transactional
    public HouseholdDtos.SettlementResponse settle(UUID actorUserId, UUID workspaceId, HouseholdDtos.SettlementRequest request) {
        WorkspaceMemberEntity actor = access.requireMember(actorUserId, workspaceId);
        if (actor.getRole() == WorkspaceRole.VIEWER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permisos para registrar liquidaciones.");
        }
        if (actor.getRole() == WorkspaceRole.MEMBER && !actorUserId.equals(request.fromUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Un miembro solo puede registrar pagos hechos por si mismo.");
        }
        Map<UUID, WorkspaceMemberEntity> membersById = membersByWorkspace(workspaceId);
        if (!membersById.containsKey(request.fromUserId()) || !membersById.containsKey(request.toUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La liquidacion solo puede involucrar miembros del workspace.");
        }
        if (request.fromUserId().equals(request.toUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes liquidar una deuda contigo mismo.");
        }
        BigDecimal amount = SharedExpenseSplitCalculator.money(request.amount());
        if (amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto de la liquidacion debe ser mayor a cero.");
        }
        List<ExpenseSplitEntity> outstandingSplits = splits.findOutstandingDebtToUser(workspaceId, request.fromUserId(), request.toUserId());
        BigDecimal outstanding = outstandingSplits.stream()
                .map(ExpenseSplitEntity::outstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (outstanding.compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto supera la deuda pendiente entre esos miembros.");
        }

        BigDecimal remaining = amount;
        List<UUID> touchedExpenseIds = new ArrayList<>();
        for (ExpenseSplitEntity split : outstandingSplits) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal applied = remaining.min(split.outstandingAmount());
            split.applySettlement(applied);
            remaining = remaining.subtract(applied);
            touchedExpenseIds.add(split.getSharedExpenseId());
        }
        markSettledExpenses(touchedExpenseIds);

        SettlementEntity settlement = settlements.save(new SettlementEntity(
                workspaceId,
                request.fromUserId(),
                request.toUserId(),
                amount,
                request.date() == null ? LocalDate.now() : request.date(),
                request.note()));
        auditLogs.record(workspaceId, actorUserId, "SETTLEMENT_REGISTERED", "Settlement", settlement.getId(), Map.of(
                "amount", amount.toPlainString(),
                "fromUserId", request.fromUserId().toString(),
                "toUserId", request.toUserId().toString()));
        return toSettlementResponse(settlement, usersById(List.of(request.fromUserId(), request.toUserId())));
    }

    @Transactional(readOnly = true)
    public List<HouseholdDtos.SettlementResponse> listSettlements(UUID actorUserId, UUID workspaceId) {
        access.requireMember(actorUserId, workspaceId);
        List<SettlementEntity> result = settlements.findByWorkspaceIdOrderBySettlementDateDescCreatedAtDesc(workspaceId);
        List<UUID> userIds = result.stream()
                .flatMap(settlement -> List.of(settlement.getFromUserId(), settlement.getToUserId()).stream())
                .distinct()
                .toList();
        Map<UUID, UserView> usersById = usersById(userIds);
        return result.stream()
                .map(settlement -> toSettlementResponse(settlement, usersById))
                .toList();
    }

    private void requireFinancialWrite(UUID actorUserId, UUID workspaceId) {
        access.requireRole(actorUserId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
    }

    private UUID validateCategory(UUID workspaceId, UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        CategoryEntity category = categories.findByIdAndWorkspaceId(categoryId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria no pertenece al workspace."));
        return category.getId();
    }

    private List<SharedExpenseSplitCalculator.SplitInput> toSplitInputs(List<HouseholdDtos.SplitParticipantRequest> participants) {
        if (participants == null) {
            return List.of();
        }
        return participants.stream()
                .map(participant -> new SharedExpenseSplitCalculator.SplitInput(
                        participant.userId(),
                        participant.amount(),
                        participant.percentage()))
                .toList();
    }

    private void ensureParticipantsBelongToWorkspace(List<SharedExpenseSplitCalculator.SplitAllocation> allocations,
                                                     Map<UUID, WorkspaceMemberEntity> membersById) {
        allocations.forEach(allocation -> {
            if (!membersById.containsKey(allocation.userId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todos los participantes deben pertenecer al workspace.");
            }
        });
    }

    private Map<UUID, WorkspaceMemberEntity> membersByWorkspace(UUID workspaceId) {
        return members.findByIdWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(WorkspaceMemberEntity::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private List<HouseholdDtos.SharedExpenseResponse> toExpenseResponses(List<SharedExpenseEntity> expenses) {
        if (expenses.isEmpty()) {
            return List.of();
        }
        List<UUID> expenseIds = expenses.stream().map(SharedExpenseEntity::getId).toList();
        Map<UUID, List<ExpenseSplitEntity>> splitsByExpense = splits.findBySharedExpenseIdIn(expenseIds).stream()
                .collect(Collectors.groupingBy(ExpenseSplitEntity::getSharedExpenseId));
        return expenses.stream()
                .map(expense -> toExpenseResponse(expense, splitsByExpense.getOrDefault(expense.getId(), List.of())))
                .toList();
    }

    private HouseholdDtos.SharedExpenseResponse toExpenseResponse(SharedExpenseEntity expense, List<ExpenseSplitEntity> expenseSplits) {
        List<UUID> userIds = new ArrayList<>();
        userIds.add(expense.getPaidByUserId());
        expenseSplits.stream().map(ExpenseSplitEntity::getUserId).forEach(userIds::add);
        Map<UUID, UserView> usersById = usersById(userIds);
        UserView payer = usersById.get(expense.getPaidByUserId());
        return new HouseholdDtos.SharedExpenseResponse(
                expense.getId(),
                expense.getWorkspaceId(),
                expense.getPaidByUserId(),
                payer == null ? "" : payer.displayName(),
                expense.getCategoryId(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getExpenseDate(),
                expense.getSplitMethod(),
                expense.getStatus(),
                expenseSplits.stream()
                        .map(split -> toSplitResponse(split, usersById.get(split.getUserId()), expense.getPaidByUserId()))
                        .toList());
    }

    private HouseholdDtos.ExpenseSplitResponse toSplitResponse(ExpenseSplitEntity split, UserView user, UUID paidByUserId) {
        BigDecimal outstanding = paidByUserId.equals(split.getUserId())
                ? BigDecimal.ZERO
                : split.outstandingAmount();
        return new HouseholdDtos.ExpenseSplitResponse(
                split.getId(),
                split.getUserId(),
                user == null ? "" : user.displayName(),
                user == null ? "" : user.email(),
                split.getAmount(),
                split.getPercentage(),
                split.getSettledAmount(),
                outstanding);
    }

    private void applyBalance(ExpenseSplitEntity split, SharedExpenseEntity expense, Map<UUID, Balance> balances) {
        if (expense == null || expense.getPaidByUserId().equals(split.getUserId())) {
            return;
        }
        BigDecimal outstanding = SharedExpenseSplitCalculator.money(split.outstandingAmount());
        if (outstanding.signum() <= 0) {
            return;
        }
        Balance debtor = balances.get(split.getUserId());
        Balance payer = balances.get(expense.getPaidByUserId());
        if (debtor != null) {
            debtor.owes = debtor.owes.add(outstanding);
        }
        if (payer != null) {
            payer.owed = payer.owed.add(outstanding);
        }
    }

    private void markSettledExpenses(Collection<UUID> expenseIds) {
        expenseIds.stream().distinct().forEach(expenseId -> sharedExpenses.findById(expenseId).ifPresent(expense -> {
            boolean allSettled = splits.findBySharedExpenseId(expenseId).stream()
                    .filter(split -> !split.getUserId().equals(expense.getPaidByUserId()))
                    .allMatch(split -> split.outstandingAmount().signum() <= 0);
            if (allSettled) {
                expense.markSettled();
            }
        }));
    }

    private Map<UUID, UserView> usersById(Collection<UUID> userIds) {
        return users.findAllById(userIds).stream()
                .map(user -> new UserView(user.getId(), user.getNombre(), user.getApellido(), user.getEmail()))
                .collect(Collectors.toMap(UserView::id, Function.identity(), (left, right) -> left));
    }

    private HouseholdDtos.SettlementResponse toSettlementResponse(SettlementEntity settlement, Map<UUID, UserView> usersById) {
        UserView from = usersById.get(settlement.getFromUserId());
        UserView to = usersById.get(settlement.getToUserId());
        return new HouseholdDtos.SettlementResponse(
                settlement.getId(),
                settlement.getWorkspaceId(),
                settlement.getFromUserId(),
                from == null ? "" : from.displayName(),
                settlement.getToUserId(),
                to == null ? "" : to.displayName(),
                settlement.getAmount(),
                settlement.getSettlementDate(),
                settlement.getNote());
    }

    private record UserView(UUID id, String nombre, String apellido, String email) {
        String displayName() {
            String fullName = ((nombre == null ? "" : nombre) + " " + (apellido == null ? "" : apellido)).trim();
            return fullName.isEmpty() ? email : fullName;
        }
    }

    private static final class Balance {
        private final UUID userId;
        private BigDecimal owes = BigDecimal.ZERO;
        private BigDecimal owed = BigDecimal.ZERO;

        private Balance(UUID userId) {
            this.userId = userId;
        }

        private HouseholdDtos.MemberBalanceResponse toResponse(UserView user) {
            BigDecimal net = owed.subtract(owes);
            return new HouseholdDtos.MemberBalanceResponse(
                    userId,
                    user == null ? "" : user.displayName(),
                    user == null ? "" : user.email(),
                    owes,
                    owed,
                    net);
        }
    }
}
