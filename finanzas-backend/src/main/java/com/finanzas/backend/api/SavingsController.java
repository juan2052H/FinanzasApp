package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.SavingsDtos;
import com.finanzas.backend.service.SavingsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/savings")
public class SavingsController {
    private final SavingsService savings;

    public SavingsController(SavingsService savings) {
        this.savings = savings;
    }

    @GetMapping("/config")
    public SavingsDtos.SavingsConfigResponse config(Authentication authentication, @PathVariable UUID workspaceId) {
        return savings.config(CurrentUser.id(authentication), workspaceId);
    }

    @PutMapping("/config")
    public SavingsDtos.SavingsConfigResponse updateConfig(Authentication authentication,
                                                          @PathVariable UUID workspaceId,
                                                          @Valid @RequestBody SavingsDtos.SavingsConfigRequest request) {
        return savings.updateConfig(CurrentUser.id(authentication), workspaceId, request);
    }

    @GetMapping("/summary")
    public SavingsDtos.SavingsSummaryResponse summary(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return savings.summary(CurrentUser.id(authentication), workspaceId, from, to);
    }

    @GetMapping("/movements")
    public SavingsDtos.SavingsMovementPageResponse movements(Authentication authentication,
                                                             @PathVariable UUID workspaceId,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "50") int size) {
        return savings.listMovements(CurrentUser.id(authentication), workspaceId, page, size);
    }

    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsDtos.SavingsMovementResponse deposit(Authentication authentication,
                                                       @PathVariable UUID workspaceId,
                                                       @Valid @RequestBody SavingsDtos.SavingsMovementRequest request) {
        return savings.manualDeposit(CurrentUser.id(authentication), workspaceId, request);
    }

    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsDtos.SavingsMovementResponse withdrawal(Authentication authentication,
                                                          @PathVariable UUID workspaceId,
                                                          @Valid @RequestBody SavingsDtos.SavingsMovementRequest request) {
        return savings.withdrawal(CurrentUser.id(authentication), workspaceId, request);
    }

    @PostMapping("/goals/{goalId}/allocations")
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsDtos.SavingsMovementResponse allocateGoal(Authentication authentication,
                                                            @PathVariable UUID workspaceId,
                                                            @PathVariable UUID goalId,
                                                            @Valid @RequestBody SavingsDtos.GoalSavingsMovementRequest request) {
        return savings.allocateGoal(CurrentUser.id(authentication), workspaceId, goalId, request);
    }

    @PostMapping("/goals/{goalId}/releases")
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsDtos.SavingsMovementResponse releaseGoal(Authentication authentication,
                                                           @PathVariable UUID workspaceId,
                                                           @PathVariable UUID goalId,
                                                           @Valid @RequestBody SavingsDtos.GoalSavingsMovementRequest request) {
        return savings.releaseGoal(CurrentUser.id(authentication), workspaceId, goalId, request);
    }

    @GetMapping("/income-impact")
    public SavingsDtos.IncomeImpactResponse incomeImpact(Authentication authentication,
                                                         @PathVariable UUID workspaceId,
                                                         @RequestParam BigDecimal amount,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return savings.incomeImpact(CurrentUser.id(authentication), workspaceId, amount, date);
    }
}
