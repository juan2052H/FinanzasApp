package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.NotificationDtos;
import com.finanzas.backend.domain.BudgetEntity;
import com.finanzas.backend.domain.CategoryEntity;
import com.finanzas.backend.domain.NotificationEntity;
import com.finanzas.backend.domain.RecurringTransactionEntity;
import com.finanzas.backend.domain.SavingsGoalEntity;
import com.finanzas.backend.domain.TransactionEntity;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.repo.BudgetRepository;
import com.finanzas.backend.repo.CategoryRepository;
import com.finanzas.backend.repo.NotificationRepository;
import com.finanzas.backend.repo.RecurringTransactionRepository;
import com.finanzas.backend.repo.SavingsGoalRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationService {
    private static final BigDecimal BUDGET_WARNING_THRESHOLD = new BigDecimal("85.00");

    private final NotificationRepository notifications;
    private final BudgetRepository budgets;
    private final TransactionRepository transactions;
    private final SavingsGoalRepository goals;
    private final RecurringTransactionRepository recurringTransactions;
    private final CategoryRepository categories;
    private final WorkspaceAccessService access;

    public NotificationService(NotificationRepository notifications,
                               BudgetRepository budgets,
                               TransactionRepository transactions,
                               SavingsGoalRepository goals,
                               RecurringTransactionRepository recurringTransactions,
                               CategoryRepository categories,
                               WorkspaceAccessService access) {
        this.notifications = notifications;
        this.budgets = budgets;
        this.transactions = transactions;
        this.goals = goals;
        this.recurringTransactions = recurringTransactions;
        this.categories = categories;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> list(UUID userId, UUID workspaceId, boolean includeRead) {
        access.requireMember(userId, workspaceId);
        List<NotificationEntity> result = includeRead
                ? notifications.findByUserIdAndWorkspaceIdOrderByCreatedAtDesc(userId, workspaceId)
                : notifications.findByUserIdAndWorkspaceIdAndReadAtIsNullOrderByCreatedAtDesc(userId, workspaceId);
        return result.stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<NotificationDtos.NotificationResponse> refresh(UUID userId, UUID workspaceId) {
        access.requireMember(userId, workspaceId);
        generateBudgetNotifications(userId, workspaceId);
        generateGoalNotifications(userId, workspaceId);
        generateRecurringNotifications(userId, workspaceId);
        return list(userId, workspaceId, false);
    }

    @Transactional
    public NotificationDtos.NotificationResponse markRead(UUID userId, UUID workspaceId, UUID notificationId) {
        access.requireMember(userId, workspaceId);
        NotificationEntity notification = notifications.findByIdAndUserIdAndWorkspaceId(notificationId, userId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificacion no encontrada."));
        notification.markRead();
        return toResponse(notification);
    }

    private void generateBudgetNotifications(UUID userId, UUID workspaceId) {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        List<TransactionEntity> monthExpenses = transactions.findByWorkspaceIdAndTypeAndTransactionDateBetweenOrderByTransactionDateDesc(
                workspaceId, TransactionType.EXPENSE, monthStart, monthEnd);
        Map<UUID, BigDecimal> spentByCategory = monthExpenses.stream()
                .collect(Collectors.groupingBy(
                        TransactionEntity::getCategoryId,
                        Collectors.mapping(TransactionEntity::getMonto, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        Map<UUID, CategoryEntity> categoriesById = categoriesById(workspaceId);
        budgets.findByWorkspaceIdOrderByPeriodMonthDesc(workspaceId).stream()
                .filter(budget -> budget.getPeriodMonth().equals(monthStart))
                .filter(budget -> budget.getAmount().signum() > 0)
                .forEach(budget -> {
                    BigDecimal spent = spentByCategory.getOrDefault(budget.getCategoryId(), BigDecimal.ZERO);
                    BigDecimal usage = spent.multiply(new BigDecimal("100")).divide(budget.getAmount(), 2, RoundingMode.HALF_UP);
                    if (usage.compareTo(BUDGET_WARNING_THRESHOLD) >= 0) {
                        CategoryEntity category = categoriesById.get(budget.getCategoryId());
                        String categoryName = category == null ? "Presupuesto" : category.getNombre();
                        upsertUnread(userId, workspaceId, "BUDGET_THRESHOLD",
                                "Presupuesto: " + categoryName,
                                "Has utilizado " + usage.toPlainString() + "% de " + categoryName + " este mes.");
                    }
                });
    }

    private void generateGoalNotifications(UUID userId, UUID workspaceId) {
        LocalDate today = LocalDate.now();
        goals.findByWorkspaceIdAndStatusNotOrderByDueDateAsc(workspaceId, "ARCHIVED").stream()
                .filter(goal -> !"COMPLETED".equals(goal.getStatus()))
                .filter(goal -> goal.getDueDate() != null)
                .filter(goal -> !goal.getDueDate().isBefore(today))
                .filter(goal -> ChronoUnit.DAYS.between(today, goal.getDueDate()) <= 30)
                .forEach(goal -> {
                    BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount()).max(BigDecimal.ZERO);
                    upsertUnread(userId, workspaceId, "GOAL_DUE_SOON",
                            "Meta cercana: " + goal.getNombre(),
                            "Te faltan " + remaining.toPlainString() + " para completar esta meta antes de " + goal.getDueDate() + ".");
                });
    }

    private void generateRecurringNotifications(UUID userId, UUID workspaceId) {
        LocalDate limit = LocalDate.now().plusDays(3);
        recurringTransactions.findByWorkspaceIdAndActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAsc(workspaceId, limit)
                .forEach(recurring -> upsertUnread(userId, workspaceId, "RECURRING_DUE_SOON",
                        "Proximo movimiento: " + recurring.getDescription(),
                        recurring.getDescription() + " esta programado para " + recurring.getNextRunDate()
                                + " por " + recurring.getAmount().toPlainString() + "."));
    }

    private void upsertUnread(UUID userId, UUID workspaceId, String type, String title, String body) {
        if (!notifications.existsByUserIdAndWorkspaceIdAndTypeAndTitleAndReadAtIsNull(userId, workspaceId, type, title)) {
            notifications.save(new NotificationEntity(workspaceId, userId, type, title, body));
        }
    }

    private Map<UUID, CategoryEntity> categoriesById(UUID workspaceId) {
        return categories.findByWorkspaceIdAndArchivedFalseOrderByNombre(workspaceId).stream()
                .collect(Collectors.toMap(CategoryEntity::getId, Function.identity()));
    }

    private NotificationDtos.NotificationResponse toResponse(NotificationEntity notification) {
        return new NotificationDtos.NotificationResponse(
                notification.getId(),
                notification.getWorkspaceId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
