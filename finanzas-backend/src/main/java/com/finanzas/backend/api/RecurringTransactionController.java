package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.RecurringDtos;
import com.finanzas.backend.api.dto.TransactionDtos;
import com.finanzas.backend.service.RecurringTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/recurring-transactions")
public class RecurringTransactionController {
    private final RecurringTransactionService recurringTransactions;

    public RecurringTransactionController(RecurringTransactionService recurringTransactions) {
        this.recurringTransactions = recurringTransactions;
    }

    @GetMapping
    public List<RecurringDtos.RecurringTransactionResponse> list(Authentication authentication,
                                                                @PathVariable UUID workspaceId,
                                                                @RequestParam(defaultValue = "true") boolean activeOnly) {
        return recurringTransactions.list(CurrentUser.id(authentication), workspaceId, activeOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringDtos.RecurringTransactionResponse create(Authentication authentication,
                                                            @PathVariable UUID workspaceId,
                                                            @Valid @RequestBody RecurringDtos.RecurringTransactionRequest request) {
        return recurringTransactions.create(CurrentUser.id(authentication), workspaceId, request);
    }

    @DeleteMapping("/{recurringTransactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(Authentication authentication,
                           @PathVariable UUID workspaceId,
                           @PathVariable UUID recurringTransactionId) {
        recurringTransactions.deactivate(CurrentUser.id(authentication), workspaceId, recurringTransactionId);
    }

    @PostMapping("/{recurringTransactionId}/run")
    public TransactionDtos.TransactionResponse run(Authentication authentication,
                                                   @PathVariable UUID workspaceId,
                                                   @PathVariable UUID recurringTransactionId) {
        return recurringTransactions.run(CurrentUser.id(authentication), workspaceId, recurringTransactionId);
    }
}
