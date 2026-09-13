package com.finanzas.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LegacyStateMigrationService {
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public MigrationResult migrateIfNeeded(Path legacyStateFile) throws IOException, ClassNotFoundException {
        if (legacyStateFile == null || !Files.exists(legacyStateFile)) {
            return new MigrationResult(false, false, null, "No existe archivo legacy para migrar.");
        }

        Path target = PersistenceService.dataDir().resolve("app-state.bin");
        if (Files.exists(target)) {
            return new MigrationResult(true, false, null, "Ya existe un estado en el directorio estable; no se sobreescribe.");
        }

        Path backup = backupLegacyFile(legacyStateFile);
        PersistenceService.AppState legacyState = readState(legacyStateFile);
        PersistenceService.AppState safeState = new PersistenceService.AppState(
                legacyState.profiles,
                null,
                legacyState.gastosHogar,
                legacyState.miembrosHogar);
        PersistenceService.save(safeState);
        return new MigrationResult(true, true, backup, "Migracion legacy completada con sesion invalidada.");
    }

    private Path backupLegacyFile(Path legacyStateFile) throws IOException {
        String fileName = legacyStateFile.getFileName().toString();
        Path backup = legacyStateFile.resolveSibling(fileName + ".migration-backup-" + LocalDateTime.now().format(BACKUP_STAMP));
        Files.copy(legacyStateFile, backup, StandardCopyOption.REPLACE_EXISTING);
        return backup;
    }

    private PersistenceService.AppState readState(Path stateFile) throws IOException, ClassNotFoundException {
        return PersistenceService.readState(stateFile);
    }

    public static final class MigrationResult {
        private final boolean attempted;
        private final boolean migrated;
        private final Path backupPath;
        private final String message;

        private MigrationResult(boolean attempted, boolean migrated, Path backupPath, String message) {
            this.attempted = attempted;
            this.migrated = migrated;
            this.backupPath = backupPath;
            this.message = message;
        }

        public boolean isAttempted() {
            return attempted;
        }

        public boolean isMigrated() {
            return migrated;
        }

        public Path getBackupPath() {
            return backupPath;
        }

        public String getMessage() {
            return message;
        }
    }
}
