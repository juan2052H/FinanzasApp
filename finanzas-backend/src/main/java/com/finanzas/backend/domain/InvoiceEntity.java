package com.finanzas.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class InvoiceEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "invoice_number", nullable = false, length = 120)
    private String invoiceNumber;

    @Column(name = "merchant_name", nullable = false, length = 160)
    private String merchantName;

    @Column(name = "tax_id", nullable = false, length = 60)
    private String taxId = "";

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "attachment_ref", nullable = false, length = 1024)
    private String attachmentRef = "";

    @Column(nullable = false, length = 500)
    private String notes = "";

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InvoiceEntity() {
    }

    public InvoiceEntity(UUID workspaceId,
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
                         UUID createdByUserId) {
        this.workspaceId = workspaceId;
        this.transactionId = transactionId;
        this.invoiceNumber = require(invoiceNumber, "invoiceNumber");
        this.merchantName = require(merchantName, "merchantName");
        this.taxId = taxId == null ? "" : taxId.trim();
        this.issueDate = issueDate == null ? LocalDate.now() : issueDate;
        this.subtotal = normalizeNonNegative(subtotal);
        this.taxAmount = normalizeNonNegative(taxAmount);
        this.totalAmount = normalizePositive(totalAmount);
        this.attachmentRef = attachmentRef == null ? "" : attachmentRef.trim();
        this.notes = notes == null ? "" : notes.trim();
        this.createdByUserId = createdByUserId;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getTransactionId() { return transactionId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getMerchantName() { return merchantName; }
    public String getTaxId() { return taxId; }
    public LocalDate getIssueDate() { return issueDate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getAttachmentRef() { return attachmentRef; }
    public String getNotes() { return notes; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(UUID transactionId,
                       String invoiceNumber,
                       String merchantName,
                       String taxId,
                       LocalDate issueDate,
                       BigDecimal subtotal,
                       BigDecimal taxAmount,
                       BigDecimal totalAmount,
                       String notes) {
        this.transactionId = transactionId;
        this.invoiceNumber = require(invoiceNumber, "invoiceNumber");
        this.merchantName = require(merchantName, "merchantName");
        this.taxId = taxId == null ? "" : taxId.trim();
        this.issueDate = issueDate == null ? LocalDate.now() : issueDate;
        this.subtotal = normalizeNonNegative(subtotal);
        this.taxAmount = normalizeNonNegative(taxAmount);
        this.totalAmount = normalizePositive(totalAmount);
        this.notes = notes == null ? "" : notes.trim();
    }

    public void setAttachmentRef(String attachmentRef) {
        this.attachmentRef = attachmentRef == null ? "" : attachmentRef.trim();
    }

    private static BigDecimal normalizePositive(BigDecimal amount) {
        BigDecimal normalized = normalizeNonNegative(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto total debe ser mayor a cero.");
        }
        return normalized;
    }

    private static BigDecimal normalizeNonNegative(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static String require(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        return value.trim();
    }
}
