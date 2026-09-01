package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.ReportDtos;
import com.finanzas.backend.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/reports")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/summary")
    public ReportDtos.ReportResponse summary(Authentication authentication,
                                             @PathVariable UUID workspaceId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.summary(CurrentUser.id(authentication), workspaceId, from, to);
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(Authentication authentication,
                                            @PathVariable UUID workspaceId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] csv = reports.exportCsv(CurrentUser.id(authentication), workspaceId, from, to);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("finanzas-report.csv").build().toString())
                .body(csv);
    }

    @GetMapping("/export.pdf")
    public ResponseEntity<byte[]> exportPdf(Authentication authentication,
                                            @PathVariable UUID workspaceId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] pdf = reports.exportPdf(CurrentUser.id(authentication), workspaceId, from, to);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("finanzas-report.pdf").build().toString())
                .body(pdf);
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportXlsx(Authentication authentication,
                                             @PathVariable UUID workspaceId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] xlsx = reports.exportXlsx(CurrentUser.id(authentication), workspaceId, from, to);
        return ResponseEntity.ok()
                .contentType(new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("finanzas-report.xlsx").build().toString())
                .body(xlsx);
    }
}
