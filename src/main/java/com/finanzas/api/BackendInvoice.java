package com.finanzas.api;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public final class BackendInvoice implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String workspaceId;
    private final String transactionId;
    private final String invoiceNumber;
    private final String merchantName;
    private final String taxId;
    private final LocalDate issueDate;
    private final BigDecimal subtotal;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;
    private final String attachmentRef;
    private final String notes;
    private final String createdByUserId;
    private final String createdAt;
    private final String updatedAt;

    public BackendInvoice(String id,
                          String workspaceId,
                          String transactionId,
                          String invoiceNumber,
                          String merchantName,
                          String taxId,
                          LocalDate issueDate,
                          BigDecimal subtotal,
                          BigDecimal taxAmount,
                          BigDecimal totalAmount,
                          String attachmentRef,
                          String notes,
                          String createdByUserId,
                          String createdAt,
                          String updatedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.transactionId = transactionId;
        this.invoiceNumber = invoiceNumber;
        this.merchantName = merchantName;
        this.taxId = taxId;
        this.issueDate = issueDate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.attachmentRef = attachmentRef;
        this.notes = notes;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getWorkspaceId() { return workspaceId; }
    public String getTransactionId() { return transactionId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public String getMerchantName() { return merchantName; }
    public String getTaxId() { return taxId; }
    public LocalDate getIssueDate() { return issueDate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getAttachmentRef() { return attachmentRef; }
    public String getNotes() { return notes; }
    public String getCreatedByUserId() { return createdByUserId; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public boolean hasAttachment() {
        return attachmentRef != null && !attachmentRef.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BackendInvoice)) return false;
        BackendInvoice that = (BackendInvoice) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
