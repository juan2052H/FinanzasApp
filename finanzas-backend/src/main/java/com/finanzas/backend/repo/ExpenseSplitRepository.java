package com.finanzas.backend.repo;

import com.finanzas.backend.domain.ExpenseSplitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplitEntity, UUID> {
    List<ExpenseSplitEntity> findBySharedExpenseId(UUID sharedExpenseId);
    List<ExpenseSplitEntity> findBySharedExpenseIdIn(List<UUID> sharedExpenseIds);

    @Query("""
            select split
            from ExpenseSplitEntity split, SharedExpenseEntity expense
            where split.sharedExpenseId = expense.id
              and expense.workspaceId = :workspaceId
              and expense.paidByUserId = :paidByUserId
              and split.userId = :debtorUserId
              and split.amount > split.settledAmount
            order by expense.expenseDate asc, expense.createdAt asc
            """)
    List<ExpenseSplitEntity> findOutstandingDebtToUser(@Param("workspaceId") UUID workspaceId,
                                                       @Param("debtorUserId") UUID debtorUserId,
                                                       @Param("paidByUserId") UUID paidByUserId);
}
