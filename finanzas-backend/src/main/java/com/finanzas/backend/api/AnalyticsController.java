package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.AnalyticsDtos;
import com.finanzas.backend.service.AnalyticsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/analytics")
public class AnalyticsController {
    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/summary")
    public AnalyticsDtos.SummaryResponse summary(Authentication authentication, @PathVariable UUID workspaceId) {
        return analytics.summary(CurrentUser.id(authentication), workspaceId);
    }
}
