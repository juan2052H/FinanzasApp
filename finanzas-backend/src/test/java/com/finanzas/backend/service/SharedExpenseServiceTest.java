package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.HouseholdDtos;
import com.finanzas.backend.domain.AuthProvider;
import com.finanzas.backend.domain.ExpenseSplitEntity;
import com.finanzas.backend.domain.SettlementEntity;
import com.finanzas.backend.domain.SharedExpenseEntity;
import com.finanzas.backend.domain.SplitMethod;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.CategoryRepository;
import com.finanzas.backend.repo.ExpenseSplitRepository;
import com.finanzas.backend.repo.SettlementRepository;
import com.finanzas.backend.repo.SharedExpenseRepository;
import com.finanzas.backend.repo.UserRepository;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SharedExpenseServiceTest {
    private final SharedExpenseRepository sharedExpenses = mock(SharedExpenseRepository.class);
    private final ExpenseSplitRepository splits = mock(ExpenseSplitRepository.class);
    private final SettlementRepository settlements = mock(SettlementRepository.class);
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final WorkspaceAccessService access = mock(WorkspaceAccessService.class);
    private final AuditLogService auditLogs = mock(AuditLogService.class);
    private final SharedExpenseService service = new SharedExpenseService(
            sharedExpenses,
            splits,
            settlements,
            members,
            categories,
            users,
            access,
            auditLogs);

    @Test
    void settlementDoesNotRemoveMembersAndAllowsAnotherExpenseWithSameUsers() {
        UUID workspaceId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID debtorId = UUID.randomUUID();
        UUID firstExpenseId = UUID.randomUUID();
        UUID secondExpenseId = UUID.randomUUID();
        WorkspaceMemberEntity payerMember = new WorkspaceMemberEntity(workspaceId, payerId, WorkspaceRole.MEMBER);
        WorkspaceMemberEntity debtorMember = new WorkspaceMemberEntity(workspaceId, debtorId, WorkspaceRole.MEMBER);
        SharedExpenseEntity firstExpense = sharedExpense(firstExpenseId, workspaceId, payerId, new BigDecimal("100.00"));
        ExpenseSplitEntity debtorSplit = new ExpenseSplitEntity(firstExpenseId, debtorId, new BigDecimal("50.00"), new BigDecimal("50.0000"));

        when(access.requireMember(debtorId, workspaceId)).thenReturn(debtorMember);
        when(members.findByIdWorkspaceId(workspaceId)).thenReturn(List.of(payerMember, debtorMember));
        when(splits.findOutstandingDebtToUser(workspaceId, debtorId, payerId)).thenReturn(List.of(debtorSplit));
        when(sharedExpenses.findById(firstExpenseId)).thenReturn(Optional.of(firstExpense));
        when(splits.findBySharedExpenseId(firstExpenseId)).thenReturn(List.of(debtorSplit));
        when(settlements.save(any(SettlementEntity.class))).thenAnswer(invocation -> {
            SettlementEntity settlement = invocation.getArgument(0);
            ReflectionTestUtils.setField(settlement, "id", UUID.randomUUID());
            return settlement;
        });
        when(sharedExpenses.save(any(SharedExpenseEntity.class))).thenAnswer(invocation -> {
            SharedExpenseEntity expense = invocation.getArgument(0);
            ReflectionTestUtils.setField(expense, "id", secondExpenseId);
            return expense;
        });
        when(splits.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findAllById(any())).thenReturn(List.of(
                user(payerId, "Ana", "Garcia", "ana@example.com"),
                user(debtorId, "Ana", "Garcia", "ana.otra@example.com")));

        service.settle(debtorId, workspaceId, new HouseholdDtos.SettlementRequest(
                debtorId,
                payerId,
                new BigDecimal("50.00"),
                LocalDate.of(2026, 9, 1),
                "Pago parcial"));

        HouseholdDtos.SharedExpenseResponse nextExpense = service.create(payerId, workspaceId, new HouseholdDtos.SharedExpenseRequest(
                "Cena",
                new BigDecimal("80.00"),
                LocalDate.of(2026, 9, 2),
                payerId,
                null,
                SplitMethod.EQUAL,
                List.of()));

        assertEquals(new BigDecimal("50.00"), debtorSplit.getSettledAmount());
        assertEquals(secondExpenseId, nextExpense.id());
        assertEquals(2, nextExpense.splits().size());
        verify(members, never()).delete(any());
    }

    private SharedExpenseEntity sharedExpense(UUID id, UUID workspaceId, UUID paidByUserId, BigDecimal amount) {
        SharedExpenseEntity expense = new SharedExpenseEntity(
                workspaceId,
                paidByUserId,
                null,
                "Mercado",
                amount,
                LocalDate.of(2026, 9, 1),
                SplitMethod.EQUAL);
        ReflectionTestUtils.setField(expense, "id", id);
        return expense;
    }

    private UserEntity user(UUID id, String nombre, String apellido, String email) {
        UserEntity user = new UserEntity(nombre, apellido, email, "hash", "COP", "PERSONAL", AuthProvider.PASSWORD);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
