package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.BudgetDtos;
import com.finanzas.backend.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/budgets")
public class BudgetController {
    private final BudgetService budgets;

    public BudgetController(BudgetService budgets) {
        this.budgets = budgets;
    }

    @GetMapping
    public List<BudgetDtos.BudgetResponse> list(Authentication authentication, @PathVariable UUID workspaceId) {
        return budgets.list(CurrentUser.id(authentication), workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetDtos.BudgetResponse create(Authentication authentication, @PathVariable UUID workspaceId,
                                            @Valid @RequestBody BudgetDtos.BudgetRequest request) {
        return budgets.create(CurrentUser.id(authentication), workspaceId, request);
    }

    @PutMapping("/{budgetId}")
    public BudgetDtos.BudgetResponse update(Authentication authentication,
                                            @PathVariable UUID workspaceId,
                                            @PathVariable UUID budgetId,
                                            @Valid @RequestBody BudgetDtos.BudgetRequest request) {
        return budgets.update(CurrentUser.id(authentication), workspaceId, budgetId, request);
    }

    @DeleteMapping("/{budgetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID budgetId) {
        budgets.delete(CurrentUser.id(authentication), workspaceId, budgetId);
    }
}
