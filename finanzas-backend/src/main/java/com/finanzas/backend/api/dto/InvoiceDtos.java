package com.finanzas.backend.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class InvoiceDtos {
    private InvoiceDtos() {
    }

    public record InvoiceRequest(
            UUID transactionId,
            String invoiceNumber,
            String merchantName,
            String taxId,
            LocalDate issueDate,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String notes) {
    }

    public record InvoiceResponse(
            UUID id,
            UUID workspaceId,
            UUID transactionId,
            String invoiceNumber,
            String merchantName,
            String taxId,
            LocalDate issueDate,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String attachmentRef,
            String notes,
            UUID createdByUserId,
            Instant createdAt,
            Instant updatedAt) {
    }
}
