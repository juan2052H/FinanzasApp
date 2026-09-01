package com.finanzas.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class AvatarStorageService {
    private static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_DIMENSION = 4096;
    private static final int AVATAR_SIZE = 256;
    private final Path storageRoot;

    public AvatarStorageService(@Value("${finanzas.storage.root}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    public String store(UUID userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una imagen.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen supera 5 MB.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".png") && !name.endsWith(".jpg") && !name.endsWith(".jpeg")) {
            throw new IllegalArgumentException("Usa PNG, JPG o JPEG.");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !"image/png".equalsIgnoreCase(contentType)
                && !"image/jpeg".equalsIgnoreCase(contentType)) {
            throw new IllegalArgumentException("El MIME declarado no corresponde a PNG/JPEG.");
        }

        byte[] bytes = file.getBytes();
        AvatarImage image = decode(bytes);
        BufferedImage thumbnail = squareThumbnail(image.image(), AVATAR_SIZE);

        Path userDir = userAvatarDir(userId);
        if (!userDir.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Ruta de almacenamiento no segura.");
        }
        Files.createDirectories(userDir);
        deleteRegularFiles(userDir);
        Path target = userDir.resolve("avatar.png").normalize();
        if (!target.startsWith(userDir)) {
            throw new IllegalArgumentException("Ruta de avatar no segura.");
        }
        if (!ImageIO.write(thumbnail, "png", target.toFile())) {
            throw new IOException("No fue posible normalizar el avatar.");
        }
        return publicRef(userId);
    }

    public AvatarResource load(UUID userId, String ifNoneMatch) throws IOException {
        Path target = userAvatarDir(userId).resolve("avatar.png").normalize();
        if (!target.startsWith(storageRoot) || !Files.isRegularFile(target)) {
            return null;
        }
        byte[] content = Files.readAllBytes(target);
        String etag = quote(hash(content));
        if (etag.equals(ifNoneMatch)) {
            return new AvatarResource(content, etag, Files.getLastModifiedTime(target).toInstant(), true);
        }
        return new AvatarResource(content, etag, Files.getLastModifiedTime(target).toInstant(), false);
    }

    public void delete(UUID userId) throws IOException {
        deleteRegularFiles(userAvatarDir(userId));
    }

    public static String publicRef(UUID userId) {
        return userId == null ? "" : "avatars/" + userId + "/avatar.png";
    }

    private Path userAvatarDir(UUID userId) {
        return storageRoot.resolve("avatars").resolve(userId.toString()).normalize();
    }

    private AvatarImage decode(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IllegalArgumentException("El archivo no contiene una imagen valida.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("El archivo no contiene una imagen PNG/JPEG valida.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!"png".equals(format) && !"jpeg".equals(format) && !"jpg".equals(format)) {
                    throw new IllegalArgumentException("Solo se aceptan imagenes PNG o JPEG reales.");
                }
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION) {
                    throw new IllegalArgumentException("La imagen excede las dimensiones permitidas.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("No fue posible decodificar la imagen.");
                }
                return new AvatarImage(decoded, format);
            } finally {
                reader.dispose();
            }
        }
    }

    private BufferedImage squareThumbnail(BufferedImage original, int size) {
        int square = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - square) / 2;
        int y = (original.getHeight() - square) / 2;
        BufferedImage thumbnail = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = thumbnail.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(original, 0, 0, size, size, x, y, x + square, y + square, null);
        g2.dispose();
        return thumbnail;
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

    private record AvatarImage(BufferedImage image, String format) {
    }

    public record AvatarResource(byte[] content, String etag, Instant lastModified, boolean notModified) {
    }
}
