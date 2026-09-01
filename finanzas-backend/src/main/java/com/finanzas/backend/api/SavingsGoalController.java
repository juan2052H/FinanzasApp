package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.SavingsGoalDtos;
import com.finanzas.backend.service.SavingsGoalService;
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
@RequestMapping("/api/workspaces/{workspaceId}/goals")
public class SavingsGoalController {
    private final SavingsGoalService goals;

    public SavingsGoalController(SavingsGoalService goals) {
        this.goals = goals;
    }

    @GetMapping
    public List<SavingsGoalDtos.SavingsGoalResponse> list(Authentication authentication, @PathVariable UUID workspaceId) {
        return goals.list(CurrentUser.id(authentication), workspaceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavingsGoalDtos.SavingsGoalResponse create(Authentication authentication, @PathVariable UUID workspaceId,
                                                      @Valid @RequestBody SavingsGoalDtos.SavingsGoalRequest request) {
        return goals.create(CurrentUser.id(authentication), workspaceId, request);
    }

    @PutMapping("/{goalId}")
    public SavingsGoalDtos.SavingsGoalResponse update(Authentication authentication,
                                                      @PathVariable UUID workspaceId,
                                                      @PathVariable UUID goalId,
                                                      @Valid @RequestBody SavingsGoalDtos.SavingsGoalRequest request) {
        return goals.update(CurrentUser.id(authentication), workspaceId, goalId, request);
    }

    @PostMapping("/{goalId}/contributions")
    public SavingsGoalDtos.SavingsGoalResponse contribute(Authentication authentication, @PathVariable UUID workspaceId,
                                                          @PathVariable UUID goalId,
                                                          @Valid @RequestBody SavingsGoalDtos.ContributionRequest request) {
        return goals.contribute(CurrentUser.id(authentication), workspaceId, goalId, request);
    }

    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(Authentication authentication, @PathVariable UUID workspaceId, @PathVariable UUID goalId) {
        goals.archive(CurrentUser.id(authentication), workspaceId, goalId);
    }
}
