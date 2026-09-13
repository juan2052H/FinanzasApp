package com.finanzas.data;

import com.finanzas.model.GastoHogar;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class PersistenceService {
    private static final Logger LOGGER = Logger.getLogger(PersistenceService.class.getName());
    private static final String DATA_DIR_PROPERTY = "finanzas.data.dir";
    private static final String DATA_DIR_ENV = "FINANZAS_DATA_DIR";
    private static final long MAX_PROFILE_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final long MAX_BACKUP_ENTRY_BYTES = 50L * 1024L * 1024L;
    private static final long MAX_BACKUP_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final int MAX_BACKUP_ENTRIES = 2048;
    private static final int AVATAR_SIZE = 256;

    private PersistenceService() {
    }

    static final class AppState implements Serializable {
        private static final long serialVersionUID = 1L;

        final Map<String, DataManager.UserProfile> profiles;
        final String currentUser;
        final List<GastoHogar> gastosHogar;
        final List<String> miembrosHogar;

        AppState(Map<String, DataManager.UserProfile> profiles, String currentUser, List<GastoHogar> gastosHogar, List<String> miembrosHogar) {
            this.profiles = new HashMap<String, DataManager.UserProfile>(profiles);
            this.currentUser = null;
            this.gastosHogar = new ArrayList<GastoHogar>(gastosHogar == null ? new ArrayList<GastoHogar>() : gastosHogar);
            this.miembrosHogar = new ArrayList<String>(miembrosHogar == null ? new ArrayList<String>() : miembrosHogar);
        }
    }

    static AppState load() throws IOException, ClassNotFoundException {
        Path stateFile = stateFile();
        if (!Files.exists(stateFile)) {
            Path legacyStateFile = legacyStateFile();
            if (!stateFile.equals(legacyStateFile) && Files.exists(legacyStateFile)) {
                return readState(legacyStateFile);
            }
            return null;
        }
        return readState(stateFile);
    }

    static void save(AppState state) throws IOException {
        Path stateFile = stateFile();
        Files.createDirectories(stateFile.getParent());
        try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(stateFile.toFile()))) {
            output.writeObject(state);
        }
    }

    static AppState readState(Path stateFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(stateFile.toFile()))) {
            input.setObjectInputFilter(PersistenceService::allowSerializedClass);
            return (AppState) input.readObject();
        }
    }

    static Path dataDir() {
        String configured = System.getProperty(DATA_DIR_PROPERTY);
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getenv(DATA_DIR_ENV);
        }
        if (configured == null || configured.trim().isEmpty()) {
            configured = Paths.get(System.getProperty("user.home"), ".finanzasapp").toString();
        }
        return Paths.get(configured).toAbsolutePath().normalize();
    }

    private static Path legacyStateFile() {
        return Paths.get("data", "app-state.bin").toAbsolutePath().normalize();
    }

    private static Path stateFile() {
        return dataDir().resolve("app-state.bin");
    }

    private static Path profileImagesDir() {
        return dataDir().resolve("profile-images");
    }

    private static Path invoiceAttachmentsDir() {
        return dataDir().resolve("invoice-attachments");
    }

    static String storeInvoiceAttachment(String userKey, String invoiceId, File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.isFile()) {
            throw new IOException("El archivo seleccionado no existe.");
        }
        if (sourceFile.length() > MAX_BACKUP_ENTRY_BYTES) {
            throw new IOException("El adjunto supera el limite permitido.");
        }
        Path userDir = invoiceAttachmentsDir().resolve(sanitizeFileName(userKey));
        Files.createDirectories(userDir);
        String extension = fileExtension(sourceFile.getName());
        Path target = userDir.resolve(sanitizeFileName(invoiceId) + extension).toAbsolutePath().normalize();
        if (!target.startsWith(invoiceAttachmentsDir())) {
            throw new IOException("No fue posible resolver una ruta segura para el adjunto.");
        }
        Files.copy(sourceFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    static byte[] readInvoiceAttachment(String attachmentPath) throws IOException {
        if (attachmentPath == null || attachmentPath.trim().isEmpty()) {
            throw new IOException("Esta factura no tiene adjunto.");
        }
        Path path = Paths.get(attachmentPath).toAbsolutePath().normalize();
        if (!path.startsWith(invoiceAttachmentsDir()) || !Files.isRegularFile(path)) {
            throw new IOException("El adjunto ya no esta disponible.");
        }
        return Files.readAllBytes(path);
    }

    static void deleteInvoiceAttachment(String attachmentPath) {
        if (attachmentPath == null || attachmentPath.trim().isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(attachmentPath).toAbsolutePath().normalize();
            if (path.startsWith(invoiceAttachmentsDir())) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No fue posible eliminar el adjunto de factura: " + attachmentPath, ex);
        }
    }

    static File createBackup(File destination, AppState state) throws IOException {
        File target = ensureZipExtension(destination);
        Files.createDirectories(dataDir());
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(target))) {
            zip.putNextEntry(new ZipEntry("app-state.bin"));
            zip.write(serializeState(state));
            zip.closeEntry();

            Path imagesDir = profileImagesDir();
            if (Files.exists(imagesDir)) {
                try (Stream<Path> files = Files.walk(imagesDir)) {
                    files.filter(Files::isRegularFile).forEach(path -> writeImageEntry(zip, path));
                }
            }
        }
        return target;
    }

    static AppState restoreBackup(File source) throws IOException, ClassNotFoundException {
        AppState restoredState = null;
        Path imagesDir = profileImagesDir();
        Files.createDirectories(imagesDir);
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(source))) {
            ZipEntry entry;
            int entryCount = 0;
            while ((entry = zip.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_BACKUP_ENTRIES) {
                    throw new IOException("El respaldo tiene demasiados archivos.");
                }
                if ("app-state.bin".equals(entry.getName())) {
                    restoredState = deserializeState(readAllBytes(zip, MAX_BACKUP_ENTRY_BYTES));
                } else if (entry.getName().startsWith("profile-images/") && !entry.isDirectory()) {
                    Path entryPath = Paths.get(entry.getName()).normalize();
                    if (entryPath.getNameCount() < 2) {
                        throw new IOException("Ruta de imagen invalida en el respaldo: " + entry.getName());
                    }
                    Path relative = entryPath.subpath(1, entryPath.getNameCount());
                    Path target = imagesDir.resolve(relative).normalize();
                    if (!target.startsWith(imagesDir)) {
                        throw new IOException("Ruta de imagen no segura en el respaldo: " + entry.getName());
                    }
                    Files.createDirectories(target.getParent());
                    copyWithLimit(zip, target, MAX_BACKUP_IMAGE_BYTES);
                }
                zip.closeEntry();
            }
        }
        if (restoredState != null) {
            save(restoredState);
        }
        return restoredState;
    }

    static String storeProfileImage(String userKey, File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.isFile()) {
            throw new IOException("El archivo seleccionado no existe.");
        }
        if (sourceFile.length() > MAX_PROFILE_IMAGE_BYTES) {
            throw new IOException("La imagen supera el limite de 5 MB.");
        }
        String extension = fileExtension(sourceFile.getName()).toLowerCase(Locale.ROOT);
        if (!".png".equals(extension) && !".jpg".equals(extension) && !".jpeg".equals(extension)) {
            throw new IOException("Formato no permitido. Usa PNG, JPG o JPEG.");
        }

        BufferedImage original = ImageIO.read(sourceFile);
        if (original == null || original.getWidth() <= 0 || original.getHeight() <= 0) {
            throw new IOException("El archivo no contiene una imagen valida.");
        }

        BufferedImage thumbnail = createSquareThumbnail(original, AVATAR_SIZE);
        Path userDir = profileImagesDir().resolve(sanitizeFileName(userKey));
        Files.createDirectories(userDir);
        Path target = userDir.resolve("avatar.png").toAbsolutePath().normalize();
        if (!target.startsWith(profileImagesDir())) {
            throw new IOException("No fue posible resolver una ruta segura para la imagen.");
        }
        if (!ImageIO.write(thumbnail, "png", target.toFile())) {
            throw new IOException("No fue posible escribir la miniatura de perfil.");
        }
        return target.toString();
    }

    static String storeRemoteProfileImage(String userKey, byte[] imageBytes, String etag) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("El backend no devolvio imagen de avatar.");
        }
        if (imageBytes.length > MAX_PROFILE_IMAGE_BYTES) {
            throw new IOException("La imagen remota supera el limite de 5 MB.");
        }
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null || original.getWidth() <= 0 || original.getHeight() <= 0) {
            throw new IOException("El avatar remoto no contiene una imagen valida.");
        }
        BufferedImage thumbnail = createSquareThumbnail(original, AVATAR_SIZE);
        Path userDir = profileImagesDir().resolve(sanitizeFileName(userKey));
        Files.createDirectories(userDir);
        deleteCachedRemoteAvatars(userDir);
        String version = sanitizeFileName((etag == null || etag.trim().isEmpty() ? "latest" : etag).replace("\"", ""));
        Path target = userDir.resolve("remote-avatar-" + version + ".png").toAbsolutePath().normalize();
        if (!target.startsWith(profileImagesDir())) {
            throw new IOException("No fue posible resolver una ruta segura para el avatar remoto.");
        }
        if (!ImageIO.write(thumbnail, "png", target.toFile())) {
            throw new IOException("No fue posible escribir la cache del avatar remoto.");
        }
        return target.toString();
    }

    static void deleteProfileImage(String imagePath) throws IOException {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return;
        }
        Path path = Paths.get(imagePath);
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath();
        }
        path = path.normalize();
        Path imagesDir = profileImagesDir();
        if (Files.exists(path) && path.startsWith(imagesDir)) {
            Files.delete(path);
        }
    }

    static String normalizeStoredProfileImagePath(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return "";
        }
        Path path = Paths.get(imagePath);
        if (!path.isAbsolute()) {
            path = path.toAbsolutePath();
        }
        path = path.normalize();
        return Files.exists(path) ? path.toString() : imagePath;
    }

    private static BufferedImage createSquareThumbnail(BufferedImage original, int size) {
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

    private static String sanitizeFileName(String value) {
        return value == null ? "usuario" : value.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    private static String fileExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return index >= 0 ? filename.substring(index) : ".png";
    }

    private static void deleteCachedRemoteAvatars(Path userDir) throws IOException {
        if (!Files.exists(userDir)) {
            return;
        }
        try (Stream<Path> files = Files.list(userDir)) {
            for (Path path : files.filter(Files::isRegularFile).collect(Collectors.toList())) {
                if (path.getFileName().toString().startsWith("remote-avatar-")) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static File ensureZipExtension(File file) {
        String path = file.getAbsolutePath();
        return path.toLowerCase().endsWith(".zip") ? file : new File(path + ".zip");
    }

    private static byte[] serializeState(AppState state) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(buffer)) {
            output.writeObject(state);
        }
        return buffer.toByteArray();
    }

    private static AppState deserializeState(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            input.setObjectInputFilter(PersistenceService::allowSerializedClass);
            return (AppState) input.readObject();
        }
    }

    private static byte[] readAllBytes(ZipInputStream zip, long maxBytes) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        long total = 0L;
        int read;
        while ((read = zip.read(chunk)) != -1) {
            total += read;
            if (total > maxBytes) {
                throw new IOException("Una entrada del respaldo supera el limite permitido.");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static void copyWithLimit(ZipInputStream zip, Path target, long maxBytes) throws IOException {
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        long total = 0L;
        try (FileOutputStream output = new FileOutputStream(temporary.toFile())) {
            byte[] chunk = new byte[4096];
            int read;
            while ((read = zip.read(chunk)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IOException("Una imagen del respaldo supera el limite permitido.");
                }
                output.write(chunk, 0, read);
            }
        } catch (IOException ex) {
            Files.deleteIfExists(temporary);
            throw ex;
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static ObjectInputFilter.Status allowSerializedClass(ObjectInputFilter.FilterInfo info) {
        if (info.depth() > 40 || info.references() > 100000 || info.streamBytes() > MAX_BACKUP_ENTRY_BYTES) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (type.isPrimitive() || type.isEnum()) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        String name = type.getName();
        String packageName = type.getPackageName();
        // Package-name equality on purpose (not startsWith): a prefix match on
        // "java.lang."/"java.util." would also let through subpackages like
        // java.lang.invoke (home of the SerializedLambda deserialization gadget)
        // and java.util.concurrent, which this filter must never allow.
        if (name.startsWith("com.finanzas.")
                || "java.lang".equals(packageName)
                || "java.util".equals(packageName)
                || "java.time".equals(packageName)
                || "java.math.BigDecimal".equals(name)
                || "java.math.BigInteger".equals(name)) {
            return ObjectInputFilter.Status.ALLOWED;
        }
        return ObjectInputFilter.Status.REJECTED;
    }

    private static void writeImageEntry(ZipOutputStream zip, Path path) {
        try {
            Path imagesDir = profileImagesDir();
            String relative = imagesDir.relativize(path).toString().replace('\\', '/');
            zip.putNextEntry(new ZipEntry("profile-images/" + relative));
            Files.copy(path, zip);
            zip.closeEntry();
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No fue posible incluir una imagen en el respaldo: " + path, ex);
        }
    }
}
