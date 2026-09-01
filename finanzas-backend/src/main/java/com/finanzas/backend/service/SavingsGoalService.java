package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.SavingsGoalDtos;
import com.finanzas.backend.domain.SavingsGoalEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.SavingsGoalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SavingsGoalService {
    private final SavingsGoalRepository goals;
    private final WorkspaceAccessService access;
    private final AuditLogService auditLogs;

    public SavingsGoalService(SavingsGoalRepository goals, WorkspaceAccessService access, AuditLogService auditLogs) {
        this.goals = goals;
        this.access = access;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalDtos.SavingsGoalResponse> list(UUID userId, UUID workspaceId) {
        access.requireMember(userId, workspaceId);
        return goals.findByWorkspaceIdAndStatusNotOrderByDueDateAsc(workspaceId, "ARCHIVED").stream().map(this::toResponse).toList();
    }

    @Transactional
    public SavingsGoalDtos.SavingsGoalResponse create(UUID userId, UUID workspaceId, SavingsGoalDtos.SavingsGoalRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        SavingsGoalEntity goal = goals.save(new SavingsGoalEntity(
                workspaceId,
                request.name(),
                request.currentAmount(),
                request.targetAmount(),
                request.color(),
                request.icono(),
                request.dueDate()));
        auditLogs.record(workspaceId, userId, "SAVINGS_GOAL_CREATED", "SavingsGoal", goal.getId(), Map.of(
                "targetAmount", goal.getTargetAmount().toPlainString(),
                "name", goal.getNombre()));
        return toResponse(goal);
    }

    @Transactional
    public SavingsGoalDtos.SavingsGoalResponse update(UUID userId, UUID workspaceId, UUID goalId, SavingsGoalDtos.SavingsGoalRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        SavingsGoalEntity goal = goals.findByIdAndWorkspaceId(goalId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta no encontrada."));
        goal.update(
                request.name(),
                request.currentAmount(),
                request.targetAmount(),
                request.color(),
                request.icono(),
                request.dueDate());
        auditLogs.record(workspaceId, userId, "SAVINGS_GOAL_UPDATED", "SavingsGoal", goalId, Map.of(
                "targetAmount", goal.getTargetAmount().toPlainString(),
                "name", goal.getNombre()));
        return toResponse(goal);
    }

    @Transactional
    public SavingsGoalDtos.SavingsGoalResponse contribute(UUID userId, UUID workspaceId, UUID goalId, SavingsGoalDtos.ContributionRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        SavingsGoalEntity goal = goals.findByIdAndWorkspaceId(goalId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta no encontrada."));
        goal.contribute(request.amount());
        auditLogs.record(workspaceId, userId, "SAVINGS_GOAL_CONTRIBUTION_ADDED", "SavingsGoal", goalId, Map.of(
                "amount", request.amount().toPlainString()));
        return toResponse(goal);
    }

    @Transactional
    public void archive(UUID userId, UUID workspaceId, UUID goalId) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        SavingsGoalEntity goal = goals.findByIdAndWorkspaceId(goalId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta no encontrada."));
        goal.archive();
        auditLogs.record(workspaceId, userId, "SAVINGS_GOAL_ARCHIVED", "SavingsGoal", goalId, Map.of());
    }

    private SavingsGoalDtos.SavingsGoalResponse toResponse(SavingsGoalEntity goal) {
        return new SavingsGoalDtos.SavingsGoalResponse(
                goal.getId(),
                goal.getWorkspaceId(),
                goal.getNombre(),
                goal.getCurrentAmount(),
                goal.getTargetAmount(),
                goal.getColor(),
                goal.getIcono(),
                goal.getDueDate(),
                goal.getStatus());
    }
}
