package com.finanzas.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvatarStorageServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void validPngIsNormalizedToPortableSquareAvatarWithCacheHeaders() throws Exception {
        AvatarStorageService storage = storage();
        UUID userId = UUID.randomUUID();

        String key = storage.store(userId, image("avatar.png", "image/png", "png", 96, 48, Color.BLUE));
        AvatarStorageService.AvatarResource loaded = storage.load(userId, null);

        assertEquals("avatars/" + userId + "/avatar.png", key);
        assertNotNull(loaded);
        assertFalse(loaded.notModified());
        assertTrue(loaded.etag().startsWith("\""));
        assertTrue(loaded.etag().endsWith("\""));
        BufferedImage normalized = ImageIO.read(new ByteArrayInputStream(loaded.content()));
        assertEquals(256, normalized.getWidth());
        assertEquals(256, normalized.getHeight());

        AvatarStorageService.AvatarResource cached = storage.load(userId, loaded.etag());
        assertNotNull(cached);
        assertTrue(cached.notModified());
    }

    @Test
    void validJpegIsAcceptedAndStoredAsPng() throws Exception {
        AvatarStorageService storage = storage();
        UUID userId = UUID.randomUUID();

        storage.store(userId, image("avatar.jpg", "image/jpeg", "jpg", 60, 90, Color.RED));
        AvatarStorageService.AvatarResource loaded = storage.load(userId, null);

        assertNotNull(loaded);
        BufferedImage normalized = ImageIO.read(new ByteArrayInputStream(loaded.content()));
        assertEquals(256, normalized.getWidth());
        assertEquals(256, normalized.getHeight());
    }

    @Test
    void gifRenamedToJpgIsRejectedByRealFormat() throws Exception {
        AvatarStorageService storage = storage();

        MockMultipartFile gifNamedJpg = image("avatar.jpg", "image/jpeg", "gif", 32, 32, Color.GREEN);

        assertThrows(IllegalArgumentException.class, () -> storage.store(UUID.randomUUID(), gifNamedJpg));
    }

    @Test
    void corruptFileIsRejected() {
        AvatarStorageService storage = storage();
        MockMultipartFile corrupt = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "not an image".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> storage.store(UUID.randomUUID(), corrupt));
    }

    @Test
    void oversizedFileIsRejectedBeforeDecode() {
        AvatarStorageService storage = storage();
        byte[] payload = new byte[(5 * 1024 * 1024) + 1];
        MockMultipartFile oversized = new MockMultipartFile("file", "avatar.png", "image/png", payload);

        assertThrows(IllegalArgumentException.class, () -> storage.store(UUID.randomUUID(), oversized));
    }

    @Test
    void extremeDimensionsAreRejectedBeforeFullRead() throws Exception {
        AvatarStorageService storage = storage();
        MockMultipartFile extreme = image("avatar.png", "image/png", "png", 4097, 1, Color.BLACK);

        assertThrows(IllegalArgumentException.class, () -> storage.store(UUID.randomUUID(), extreme));
    }

    @Test
    void replacementRemovesPreviousFilesAndChangesEtag() throws Exception {
        AvatarStorageService storage = storage();
        UUID userId = UUID.randomUUID();

        storage.store(userId, image("avatar.png", "image/png", "png", 64, 64, Color.BLUE));
        String firstEtag = storage.load(userId, null).etag();
        Path legacy = tempDir.resolve("avatars").resolve(userId.toString()).resolve("legacy.jpg");
        Files.writeString(legacy, "legacy");

        storage.store(userId, image("avatar.png", "image/png", "png", 64, 64, Color.RED));

        assertNotEquals(firstEtag, storage.load(userId, null).etag());
        assertFalse(Files.exists(legacy));
    }

    @Test
    void deleteAndIsolationAreScopedToAuthenticatedUser() throws Exception {
        AvatarStorageService storage = storage();
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        storage.store(owner, image("avatar.png", "image/png", "png", 64, 64, Color.BLUE));

        assertNull(storage.load(other, null));
        assertNotNull(storage.load(owner, null));

        storage.delete(owner);

        assertNull(storage.load(owner, null));
    }

    private AvatarStorageService storage() {
        return new AvatarStorageService(tempDir.toString());
    }

    private MockMultipartFile image(String fileName, String contentType, String format, int width, int height, Color color)
            throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(color);
        g2.fillRect(0, 0, width, height);
        g2.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, format, output));
        return new MockMultipartFile("file", fileName, contentType, output.toByteArray());
    }
}
