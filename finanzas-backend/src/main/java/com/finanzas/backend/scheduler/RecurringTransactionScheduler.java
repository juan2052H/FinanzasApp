package com.finanzas.backend.scheduler;

import com.finanzas.backend.service.RecurringTransactionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "finanzas.recurring.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class RecurringTransactionScheduler {
    private static final Logger LOGGER = Logger.getLogger(RecurringTransactionScheduler.class.getName());
    private final RecurringTransactionService recurringService;

    public RecurringTransactionScheduler(RecurringTransactionService recurringService) {
        this.recurringService = recurringService;
    }

    @Scheduled(cron = "${finanzas.recurring.cron:0 0 1 * * ?}")
    public void processDueRecurringTransactions() {
        try {
            int executed = recurringService.runAllDue(LocalDate.now());
            if (executed > 0) {
                LOGGER.info("Scheduler de recurrencias ejecuto " + executed + " transacciones recurrentes vencidas.");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Error durante la ejecucion automatica de transacciones recurrentes.", ex);
        }
    }
}
