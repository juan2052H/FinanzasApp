package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.HouseholdDtos;
import com.finanzas.backend.domain.SharedExpenseStatus;
import com.finanzas.backend.service.SharedExpenseService;
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
@RequestMapping("/api/workspaces/{workspaceId}/shared-expenses")
public class SharedExpenseController {
    private final SharedExpenseService sharedExpenses;

    public SharedExpenseController(SharedExpenseService sharedExpenses) {
        this.sharedExpenses = sharedExpenses;
    }

    @GetMapping
    public List<HouseholdDtos.SharedExpenseResponse> list(Authentication authentication,
                                                         @PathVariable UUID workspaceId,
                                                         @RequestParam(required = false) SharedExpenseStatus status) {
        return sharedExpenses.list(CurrentUser.id(authentication), workspaceId, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdDtos.SharedExpenseResponse create(Authentication authentication,
                                                      @PathVariable UUID workspaceId,
                                                      @Valid @RequestBody HouseholdDtos.SharedExpenseRequest request) {
        return sharedExpenses.create(CurrentUser.id(authentication), workspaceId, request);
    }

    @DeleteMapping("/{sharedExpenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID sharedExpenseId) {
        sharedExpenses.cancel(CurrentUser.id(authentication), workspaceId, sharedExpenseId);
    }

    @GetMapping("/balances")
    public List<HouseholdDtos.MemberBalanceResponse> balances(Authentication authentication, @PathVariable UUID workspaceId) {
        return sharedExpenses.balances(CurrentUser.id(authentication), workspaceId);
    }

    @GetMapping("/settlements")
    public List<HouseholdDtos.SettlementResponse> settlements(Authentication authentication, @PathVariable UUID workspaceId) {
        return sharedExpenses.listSettlements(CurrentUser.id(authentication), workspaceId);
    }

    @PostMapping("/settlements")
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdDtos.SettlementResponse settle(Authentication authentication,
                                                  @PathVariable UUID workspaceId,
                                                  @Valid @RequestBody HouseholdDtos.SettlementRequest request) {
        return sharedExpenses.settle(CurrentUser.id(authentication), workspaceId, request);
    }
}
