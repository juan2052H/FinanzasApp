package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.TransactionDtos;
import com.finanzas.backend.domain.TransactionType;
import com.finanzas.backend.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/transactions")
public class TransactionController {
    private final TransactionService transactions;

    public TransactionController(TransactionService transactions) {
        this.transactions = transactions;
    }

    @GetMapping
    public List<TransactionDtos.TransactionResponse> list(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transactions.list(CurrentUser.id(authentication), workspaceId, type, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionDtos.TransactionResponse create(Authentication authentication, @PathVariable UUID workspaceId,
                                                      @Valid @RequestBody TransactionDtos.TransactionRequest request) {
        return transactions.create(CurrentUser.id(authentication), workspaceId, request);
    }

    @PutMapping("/{transactionId}")
    public TransactionDtos.TransactionResponse update(Authentication authentication,
                                                      @PathVariable UUID workspaceId,
                                                      @PathVariable UUID transactionId,
                                                      @Valid @RequestBody TransactionDtos.TransactionRequest request) {
        return transactions.update(CurrentUser.id(authentication), workspaceId, transactionId, request);
    }

    @DeleteMapping("/{transactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID transactionId) {
        transactions.delete(CurrentUser.id(authentication), workspaceId, transactionId);
    }
}
