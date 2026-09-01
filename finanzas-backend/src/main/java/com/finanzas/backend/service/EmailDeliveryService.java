package com.finanzas.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class EmailDeliveryService {
    private static final Logger LOGGER = Logger.getLogger(EmailDeliveryService.class.getName());

    private final String mode;
    private final Path sinkDir;

    public EmailDeliveryService(@Value("${finanzas.email.mode:file}") String mode,
                                @Value("${finanzas.email.sink-dir:./storage/mail}") String sinkDir) {
        this.mode = mode == null ? "file" : mode.trim().toLowerCase(java.util.Locale.ROOT);
        this.sinkDir = Path.of(sinkDir == null || sinkDir.trim().isEmpty() ? "./storage/mail" : sinkDir.trim())
                .toAbsolutePath()
                .normalize();
    }

    public void sendAccountEmail(String to, String subject, String body) {
        if ("disabled".equals(mode)) {
            LOGGER.info(() -> "Email deshabilitado para " + safe(to) + " asunto=" + safe(subject));
            return;
        }
        if ("log".equals(mode)) {
            LOGGER.info(() -> "Email dev a " + safe(to) + " asunto=" + safe(subject) + System.lineSeparator() + body);
            return;
        }
        writeToFile(to, subject, body);
    }

    private void writeToFile(String to, String subject, String body) {
        try {
            Files.createDirectories(sinkDir);
            String fileName = Instant.now().toString().replace(':', '-')
                    + "-" + UUID.randomUUID() + ".eml";
            String content = "To: " + safe(to) + System.lineSeparator()
                    + "Subject: " + safe(subject) + System.lineSeparator()
                    + "Content-Type: text/plain; charset=utf-8" + System.lineSeparator()
                    + System.lineSeparator()
                    + (body == null ? "" : body);
            Files.writeString(sinkDir.resolve(fileName), content, StandardCharsets.UTF_8);
            LOGGER.info(() -> "Email dev escrito en " + sinkDir.resolve(fileName));
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "No fue posible escribir email dev.", ex);
            throw new IllegalStateException("No fue posible entregar el email.", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
