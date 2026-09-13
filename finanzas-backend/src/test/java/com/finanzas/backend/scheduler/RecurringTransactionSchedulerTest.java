package com.finanzas.backend.scheduler;

import com.finanzas.backend.service.RecurringTransactionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecurringTransactionSchedulerTest {

    @Test
    void processDueRecurringTransactionsCallsService() {
        RecurringTransactionService service = mock(RecurringTransactionService.class);
        when(service.runAllDue(any(LocalDate.class))).thenReturn(2);

        RecurringTransactionScheduler scheduler = new RecurringTransactionScheduler(service);
        scheduler.processDueRecurringTransactions();

        verify(service).runAllDue(any(LocalDate.class));
    }
}
