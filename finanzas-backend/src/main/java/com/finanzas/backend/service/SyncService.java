package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.SyncDtos;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class SyncService {
    private static final Instant EPOCH = Instant.EPOCH;

    private final WorkspaceAccessService access;
    private final UserRepository users;
    private final JdbcTemplate jdbc;

    public SyncService(WorkspaceAccessService access, UserRepository users, JdbcTemplate jdbc) {
        this.access = access;
        this.users = users;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public SyncDtos.SyncChangesResponse changes(UUID actorUserId, UUID workspaceId, Instant since) {
        WorkspaceMemberEntity member = access.requireMember(actorUserId, workspaceId);
        UserEntity user = users.findById(actorUserId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Usuario no encontrado."));
        Map<String, Instant> revisions = resourceRevisions(workspaceId);
        Instant revision = revisions.values().stream().max(Instant::compareTo).orElse(EPOCH);
        Instant threshold = since == null ? null : since;
        List<String> changedResources = new ArrayList<>();
        for (Map.Entry<String, Instant> entry : revisions.entrySet()) {
            if (threshold == null || entry.getValue().isAfter(threshold)) {
                changedResources.add(entry.getKey());
            }
        }
        return new SyncDtos.SyncChangesResponse(
                workspaceId,
                revision,
                Instant.now(),
                changedResources,
                countPendingReceivedInvitations(user.getEmail()),
                canManage(member) ? countPendingWorkspaceInvitations(workspaceId) : 0,
                countUnreadNotifications(actorUserId, workspaceId));
    }

    private Map<String, Instant> resourceRevisions(UUID workspaceId) {
        Map<String, Instant> revisions = new LinkedHashMap<>();
        revisions.put("workspace", instant("select updated_at from workspaces where id = ?", workspaceId));
        revisions.put("members", instant("select max(created_at) from workspace_members where workspace_id = ?", workspaceId));
        revisions.put("invitations", instant("select max(updated_at) from workspace_invitations where workspace_id = ?", workspaceId));
        revisions.put("categories", instant("select max(updated_at) from categories where workspace_id = ?", workspaceId));
        revisions.put("transactions", instant("select max(updated_at) from transactions where workspace_id = ?", workspaceId));
        revisions.put("budgets", instant("select max(updated_at) from budgets where workspace_id = ?", workspaceId));
        revisions.put("savings_goals", instant("select max(updated_at) from savings_goals where workspace_id = ?", workspaceId));
        revisions.put("savings_config", instant("select max(updated_at) from savings_config where workspace_id = ?", workspaceId));
        revisions.put("savings_movements", instant("select max(created_at) from savings_movements where workspace_id = ?", workspaceId));
        revisions.put("recurring_transactions", instant("select max(updated_at) from recurring_transactions where workspace_id = ?", workspaceId));
        revisions.put("shared_expenses", instant("select max(updated_at) from shared_expenses where workspace_id = ?", workspaceId));
        revisions.put("expense_splits", instant("""
                select max(split.created_at)
                from expense_splits split
                join shared_expenses expense on expense.id = split.shared_expense_id
                where expense.workspace_id = ?
                """, workspaceId));
        revisions.put("settlements", instant("select max(created_at) from settlements where workspace_id = ?", workspaceId));
        revisions.put("notifications", instant("""
                select max(coalesce(read_at, created_at))
                from notifications
                where workspace_id = ?
                """, workspaceId));
        return revisions;
    }

    private Instant instant(String sql, Object... args) {
        Timestamp value = jdbc.queryForObject(sql, Timestamp.class, args);
        return value == null ? EPOCH : value.toInstant();
    }

    private int countPendingReceivedInvitations(String email) {
        Integer value = jdbc.queryForObject("""
                select count(*)
                from workspace_invitations
                where invited_email = ?
                  and status = 'PENDING'
                  and expires_at > now()
                """, Integer.class, email);
        return value == null ? 0 : value;
    }

    private int countPendingWorkspaceInvitations(UUID workspaceId) {
        Integer value = jdbc.queryForObject("""
                select count(*)
                from workspace_invitations
                where workspace_id = ?
                  and status = 'PENDING'
                  and expires_at > now()
                """, Integer.class, workspaceId);
        return value == null ? 0 : value;
    }

    private int countUnreadNotifications(UUID userId, UUID workspaceId) {
        Integer value = jdbc.queryForObject("""
                select count(*)
                from notifications
                where user_id = ?
                  and workspace_id = ?
                  and read_at is null
                """, Integer.class, userId, workspaceId);
        return value == null ? 0 : value;
    }

    private boolean canManage(WorkspaceMemberEntity member) {
        return member.getRole() == WorkspaceRole.OWNER || member.getRole() == WorkspaceRole.ADMIN;
    }
}
