package com.finanzas.backend.repo;

import com.finanzas.backend.domain.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
    List<InvoiceEntity> findByWorkspaceIdOrderByIssueDateDescCreatedAtDesc(UUID workspaceId);
    List<InvoiceEntity> findByWorkspaceIdAndIssueDateBetweenOrderByIssueDateDescCreatedAtDesc(UUID workspaceId, LocalDate from, LocalDate to);
    Optional<InvoiceEntity> findByIdAndWorkspaceId(UUID id, UUID workspaceId);
    Optional<InvoiceEntity> findByWorkspaceIdAndInvoiceNumberIgnoreCase(UUID workspaceId, String invoiceNumber);
    List<InvoiceEntity> findByWorkspaceIdAndTransactionId(UUID workspaceId, UUID transactionId);
}
