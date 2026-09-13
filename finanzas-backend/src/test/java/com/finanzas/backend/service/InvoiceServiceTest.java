package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.InvoiceDtos;
import com.finanzas.backend.domain.InvoiceEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.repo.InvoiceRepository;
import com.finanzas.backend.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceServiceTest {
    private InvoiceRepository invoiceRepo;
    private TransactionRepository transactionRepo;
    private WorkspaceAccessService access;
    private InvoiceAttachmentStorageService storage;
    private AuditLogService auditLogs;
    private InvoiceService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final UUID invoiceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invoiceRepo = mock(InvoiceRepository.class);
        transactionRepo = mock(TransactionRepository.class);
        access = mock(WorkspaceAccessService.class);
        storage = mock(InvoiceAttachmentStorageService.class);
        auditLogs = mock(AuditLogService.class);
        service = new InvoiceService(invoiceRepo, transactionRepo, access, storage, auditLogs);
    }

    @Test
    void createAndListInvoices() {
        InvoiceDtos.InvoiceRequest request = new InvoiceDtos.InvoiceRequest(
                null, "FAC-001", "Exito", "900123456", LocalDate.now(),
                new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"), "Mercado");

        InvoiceEntity saved = new InvoiceEntity(
                workspaceId, null, "FAC-001", "Exito", "900123456", LocalDate.now(),
                new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"), "", "Mercado", userId);

        when(invoiceRepo.save(any(InvoiceEntity.class))).thenReturn(saved);
        when(invoiceRepo.findByWorkspaceIdOrderByIssueDateDescCreatedAtDesc(workspaceId)).thenReturn(List.of(saved));

        InvoiceDtos.InvoiceResponse created = service.create(userId, workspaceId, request);
        assertNotNull(created);
        assertEquals("FAC-001", created.invoiceNumber());
        assertEquals("Exito", created.merchantName());
        assertEquals(new BigDecimal("119.00"), created.totalAmount());

        List<InvoiceDtos.InvoiceResponse> list = service.list(userId, workspaceId, null, null);
        assertEquals(1, list.size());
        assertEquals("FAC-001", list.get(0).invoiceNumber());
    }

    @Test
    void uploadAndDownloadAttachment() throws IOException {
        InvoiceEntity invoice = new InvoiceEntity(
                workspaceId, null, "FAC-002", "Jumbo", "900987654", LocalDate.now(),
                new BigDecimal("50.00"), new BigDecimal("9.50"), new BigDecimal("59.50"), "", "Factura", userId);

        when(invoiceRepo.findByIdAndWorkspaceId(invoiceId, workspaceId)).thenReturn(Optional.of(invoice));
        when(storage.store(eq(workspaceId), eq(invoiceId), any())).thenReturn("ws/inv/receipt.pdf");

        MockMultipartFile file = new MockMultipartFile("file", "receipt.pdf", "application/pdf", "dummy pdf".getBytes());
        InvoiceDtos.InvoiceResponse updated = service.uploadAttachment(userId, workspaceId, invoiceId, file);

        assertEquals("ws/inv/receipt.pdf", updated.attachmentRef());
        verify(storage).store(eq(workspaceId), eq(invoiceId), any());
    }

    @Test
    void deleteInvoice() throws IOException {
        InvoiceEntity invoice = new InvoiceEntity(
                workspaceId, null, "FAC-003", "Olimpica", "900111222", LocalDate.now(),
                new BigDecimal("20.00"), new BigDecimal("3.80"), new BigDecimal("23.80"), "", "", userId);

        when(invoiceRepo.findByIdAndWorkspaceId(invoiceId, workspaceId)).thenReturn(Optional.of(invoice));

        service.delete(userId, workspaceId, invoiceId);
        verify(storage).delete(workspaceId, invoiceId);
        verify(invoiceRepo).delete(invoice);
    }
}
