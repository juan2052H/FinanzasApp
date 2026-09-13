package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.InvoiceDtos;
import com.finanzas.backend.domain.InvoiceEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.InvoiceRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InvoiceService {
    private final InvoiceRepository invoices;
    private final TransactionRepository transactions;
    private final WorkspaceAccessService access;
    private final InvoiceAttachmentStorageService storage;
    private final AuditLogService auditLogs;

    public InvoiceService(InvoiceRepository invoices,
                          TransactionRepository transactions,
                          WorkspaceAccessService access,
                          InvoiceAttachmentStorageService storage,
                          AuditLogService auditLogs) {
        this.invoices = invoices;
        this.transactions = transactions;
        this.access = access;
        this.storage = storage;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public List<InvoiceDtos.InvoiceResponse> list(UUID userId, UUID workspaceId, LocalDate from, LocalDate to) {
        access.requireMember(userId, workspaceId);
        List<InvoiceEntity> list = (from != null && to != null)
                ? invoices.findByWorkspaceIdAndIssueDateBetweenOrderByIssueDateDescCreatedAtDesc(workspaceId, from, to)
                : invoices.findByWorkspaceIdOrderByIssueDateDescCreatedAtDesc(workspaceId);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDtos.InvoiceResponse get(UUID userId, UUID workspaceId, UUID invoiceId) {
        access.requireMember(userId, workspaceId);
        return toResponse(requireInvoice(workspaceId, invoiceId));
    }

    @Transactional
    public InvoiceDtos.InvoiceResponse create(UUID userId, UUID workspaceId, InvoiceDtos.InvoiceRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        if (request.transactionId() != null) {
            transactions.findByIdAndWorkspaceId(request.transactionId(), workspaceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaccion asociada no encontrada."));
        }
        InvoiceEntity invoice = new InvoiceEntity(
                workspaceId,
                request.transactionId(),
                request.invoiceNumber(),
                request.merchantName(),
                request.taxId(),
                request.issueDate(),
                request.subtotal(),
                request.taxAmount(),
                request.totalAmount(),
                "",
                request.notes(),
                userId);
        invoices.save(invoice);
        auditLogs.record(workspaceId, userId, "INVOICE_CREATED", "Invoice", invoice.getId(), Map.of(
                "invoiceNumber", invoice.getInvoiceNumber(),
                "merchant", invoice.getMerchantName(),
                "total", invoice.getTotalAmount().toPlainString()));
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceDtos.InvoiceResponse update(UUID userId, UUID workspaceId, UUID invoiceId, InvoiceDtos.InvoiceRequest request) {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        InvoiceEntity invoice = requireInvoice(workspaceId, invoiceId);
        if (request.transactionId() != null) {
            transactions.findByIdAndWorkspaceId(request.transactionId(), workspaceId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaccion asociada no encontrada."));
        }
        invoice.update(
                request.transactionId(),
                request.invoiceNumber(),
                request.merchantName(),
                request.taxId(),
                request.issueDate(),
                request.subtotal(),
                request.taxAmount(),
                request.totalAmount(),
                request.notes());
        auditLogs.record(workspaceId, userId, "INVOICE_UPDATED", "Invoice", invoice.getId(), Map.of(
                "invoiceNumber", invoice.getInvoiceNumber(),
                "total", invoice.getTotalAmount().toPlainString()));
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceDtos.InvoiceResponse uploadAttachment(UUID userId, UUID workspaceId, UUID invoiceId, MultipartFile file) throws IOException {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.MEMBER);
        InvoiceEntity invoice = requireInvoice(workspaceId, invoiceId);
        String ref = storage.store(workspaceId, invoiceId, file);
        invoice.setAttachmentRef(ref);
        auditLogs.record(workspaceId, userId, "INVOICE_ATTACHMENT_UPLOADED", "Invoice", invoice.getId(), Map.of(
                "attachmentRef", ref));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public InvoiceAttachmentStorageService.AttachmentResource downloadAttachment(UUID userId, UUID workspaceId, UUID invoiceId) throws IOException {
        access.requireMember(userId, workspaceId);
        requireInvoice(workspaceId, invoiceId);
        InvoiceAttachmentStorageService.AttachmentResource resource = storage.load(workspaceId, invoiceId);
        if (resource == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Soporte no encontrado.");
        }
        return resource;
    }

    @Transactional
    public void delete(UUID userId, UUID workspaceId, UUID invoiceId) throws IOException {
        access.requireRole(userId, workspaceId, WorkspaceRole.OWNER, WorkspaceRole.ADMIN);
        InvoiceEntity invoice = requireInvoice(workspaceId, invoiceId);
        storage.delete(workspaceId, invoiceId);
        invoices.delete(invoice);
        auditLogs.record(workspaceId, userId, "INVOICE_DELETED", "Invoice", invoiceId, Map.of(
                "invoiceNumber", invoice.getInvoiceNumber()));
    }

    private InvoiceEntity requireInvoice(UUID workspaceId, UUID invoiceId) {
        return invoices.findByIdAndWorkspaceId(invoiceId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Factura no encontrada."));
    }

    private InvoiceDtos.InvoiceResponse toResponse(InvoiceEntity invoice) {
        return new InvoiceDtos.InvoiceResponse(
                invoice.getId(),
                invoice.getWorkspaceId(),
                invoice.getTransactionId(),
                invoice.getInvoiceNumber(),
                invoice.getMerchantName(),
                invoice.getTaxId(),
                invoice.getIssueDate(),
                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getAttachmentRef(),
                invoice.getNotes(),
                invoice.getCreatedByUserId(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt());
    }
}
