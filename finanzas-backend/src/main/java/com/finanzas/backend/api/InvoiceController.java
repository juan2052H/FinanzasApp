package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.InvoiceDtos;
import com.finanzas.backend.service.InvoiceAttachmentStorageService;
import com.finanzas.backend.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/invoices")
public class InvoiceController {
    private final InvoiceService invoices;

    public InvoiceController(InvoiceService invoices) {
        this.invoices = invoices;
    }

    @GetMapping
    public List<InvoiceDtos.InvoiceResponse> list(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return invoices.list(CurrentUser.id(authentication), workspaceId, from, to);
    }

    @GetMapping("/{invoiceId}")
    public InvoiceDtos.InvoiceResponse get(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID invoiceId) {
        return invoices.get(CurrentUser.id(authentication), workspaceId, invoiceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceDtos.InvoiceResponse create(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InvoiceDtos.InvoiceRequest request) {
        return invoices.create(CurrentUser.id(authentication), workspaceId, request);
    }

    @PutMapping("/{invoiceId}")
    public InvoiceDtos.InvoiceResponse update(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID invoiceId,
            @Valid @RequestBody InvoiceDtos.InvoiceRequest request) {
        return invoices.update(CurrentUser.id(authentication), workspaceId, invoiceId, request);
    }

    @PostMapping(value = "/{invoiceId}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InvoiceDtos.InvoiceResponse uploadAttachment(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID invoiceId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return invoices.uploadAttachment(CurrentUser.id(authentication), workspaceId, invoiceId, file);
    }

    @GetMapping("/{invoiceId}/attachment")
    public ResponseEntity<byte[]> downloadAttachment(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID invoiceId) throws IOException {
        InvoiceAttachmentStorageService.AttachmentResource resource =
                invoices.downloadAttachment(CurrentUser.id(authentication), workspaceId, invoiceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resource.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.filename() + "\"")
                .header(HttpHeaders.ETAG, resource.etag())
                .lastModified(resource.lastModified())
                .body(resource.content());
    }

    @DeleteMapping("/{invoiceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID invoiceId) throws IOException {
        invoices.delete(CurrentUser.id(authentication), workspaceId, invoiceId);
    }
}
