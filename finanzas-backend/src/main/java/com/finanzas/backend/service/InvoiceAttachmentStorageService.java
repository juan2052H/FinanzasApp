package com.finanzas.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class InvoiceAttachmentStorageService {
    private static final long MAX_BYTES = 10L * 1024L * 1024L; // 10MB
    private final Path storageRoot;

    public InvoiceAttachmentStorageService(@Value("${finanzas.storage.root}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public String store(UUID workspaceId, UUID invoiceId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo de soporte.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("El archivo supera el limite de 10 MB.");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String extension;
        String contentType;

        byte[] bytes = file.getBytes();
        if (isPdf(bytes, originalName)) {
            extension = ".pdf";
            contentType = "application/pdf";
        } else if (isPng(bytes, originalName)) {
            extension = ".png";
            contentType = "image/png";
        } else if (isJpeg(bytes, originalName)) {
            extension = ".jpg";
            contentType = "image/jpeg";
        } else {
            throw new IllegalArgumentException("Solo se permiten archivos reales PDF, PNG o JPG.");
        }

        Path invoiceDir = invoiceDir(workspaceId, invoiceId);
        if (!invoiceDir.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Ruta de almacenamiento no segura.");
        }
        Files.createDirectories(invoiceDir);
        deleteRegularFiles(invoiceDir);

        Path target = invoiceDir.resolve("attachment" + extension).normalize();
        if (!target.startsWith(invoiceDir)) {
            throw new IllegalArgumentException("Ruta de archivo no segura.");
        }
        Files.write(target, bytes);
        return publicRef(workspaceId, invoiceId, extension);
    }

    public AttachmentResource load(UUID workspaceId, UUID invoiceId) throws IOException {
        Path dir = invoiceDir(workspaceId, invoiceId);
        if (!dir.startsWith(storageRoot) || !Files.isDirectory(dir)) {
            return null;
        }
        try (Stream<Path> files = Files.list(dir)) {
            Path file = files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("attachment."))
                    .findFirst()
                    .orElse(null);
            if (file == null) {
                return null;
            }
            byte[] content = Files.readAllBytes(file);
            String extension = file.getFileName().toString().substring("attachment".length()).toLowerCase(Locale.ROOT);
            String mimeType = ".pdf".equals(extension) ? "application/pdf" : (".png".equals(extension) ? "image/png" : "image/jpeg");
            String etag = quote(hash(content));
            return new AttachmentResource(content, mimeType, etag, Files.getLastModifiedTime(file).toInstant(), file.getFileName().toString());
        }
    }

    public void delete(UUID workspaceId, UUID invoiceId) throws IOException {
        deleteRegularFiles(invoiceDir(workspaceId, invoiceId));
    }

    public static String publicRef(UUID workspaceId, UUID invoiceId, String extension) {
        return "invoices/" + workspaceId + "/" + invoiceId + "/attachment" + extension;
    }

    private Path invoiceDir(UUID workspaceId, UUID invoiceId) {
        return storageRoot.resolve("invoices")
                .resolve(workspaceId.toString())
                .resolve(invoiceId.toString())
                .normalize();
    }

    private boolean isPdf(byte[] bytes, String originalName) {
        if (bytes.length < 5) return false;
        // %PDF-
        return bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46 && bytes[4] == 0x2D;
    }

    private boolean isPng(byte[] bytes, String originalName) {
        if (bytes.length < 8) return false;
        // 89 50 4E 47 0D 0A 1A 0A
        return (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
    }

    private boolean isJpeg(byte[] bytes, String originalName) {
        if (bytes.length < 3) return false;
        // FF D8 FF
        return (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
    }

    private void deleteRegularFiles(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Files.deleteIfExists(file);
            }
        }
    }

    private String hash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible.", ex);
        }
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }

    public record AttachmentResource(byte[] content, String contentType, String etag, Instant lastModified, String filename) {
    }
}
