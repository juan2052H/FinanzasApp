package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.TaxDtos;
import com.finanzas.backend.service.TaxPreparationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tax-preparation")
public class TaxPreparationController {
    private final TaxPreparationService taxPreparation;

    public TaxPreparationController(TaxPreparationService taxPreparation) {
        this.taxPreparation = taxPreparation;
    }

    @GetMapping("/summary")
    public TaxDtos.TaxPreparationSummaryResponse summary(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) Integer year) {
        int targetYear = (year == null || year <= 1900) ? LocalDate.now().getYear() : year;
        return taxPreparation.summary(CurrentUser.id(authentication), workspaceId, targetYear);
    }

    @PutMapping("/config/{year}")
    public TaxDtos.TaxConfigurationResponse configure(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable int year,
            @Valid @RequestBody TaxDtos.TaxConfigurationRequest request) {
        return taxPreparation.configure(CurrentUser.id(authentication), workspaceId, year, request);
    }
}
