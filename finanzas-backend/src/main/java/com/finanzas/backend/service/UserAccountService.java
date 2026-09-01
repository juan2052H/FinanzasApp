package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.AccountDtos;
import com.finanzas.backend.domain.RefreshTokenEntity;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.UserSettingsEntity;
import com.finanzas.backend.domain.WorkspaceEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.repo.UserRepository;
import com.finanzas.backend.repo.UserSettingsRepository;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import com.finanzas.backend.repo.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserAccountService {
    private final UserRepository users;
    private final UserSettingsRepository settings;
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final RefreshTokenService refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatars;
    private final AuditLogService auditLogs;

    public UserAccountService(UserRepository users,
                              UserSettingsRepository settings,
                              WorkspaceRepository workspaces,
                              WorkspaceMemberRepository members,
                              RefreshTokenService refreshTokens,
                              PasswordEncoder passwordEncoder,
                              AvatarStorageService avatars,
                              AuditLogService auditLogs) {
        this.users = users;
        this.settings = settings;
        this.workspaces = workspaces;
        this.members = members;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.avatars = avatars;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        UserEntity user = requireActiveUser(userId);
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", Instant.now().toString());
        export.put("profile", profile(user));
        export.put("settings", settings.findById(userId).map(this::settings).orElseGet(() -> new LinkedHashMap<String, Object>()));
        export.put("sessions", sessions(userId));
        export.put("workspaces", workspaces(userId));
        return export;
    }

    @Transactional
    public void delete(UUID userId, AccountDtos.DeleteAccountRequest request) {
        UserEntity user = requireActiveUser(userId);
        String confirmed = UserEntity.normalizeEmail(request.confirmEmail());
        if (!user.getEmail().equals(confirmed)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "El correo de confirmacion no coincide.");
        }
        if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
            String password = request.password() == null ? "" : request.password();
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La contrasena no es valida.");
            }
        }
        ensureCanDelete(userId);
        refreshTokens.revokeAll(userId);
        removeOwnedSingleMemberWorkspaces(userId);
        removeNonOwnedMemberships(userId);
        settings.deleteById(userId);
        try {
            avatars.delete(userId);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No fue posible eliminar el avatar.", ex);
        }
        user.deleteAccount(Instant.now());
        auditLogs.record(null, userId, "USER_ACCOUNT_DELETED", "User", userId, Map.of("mode", "ANONYMIZED"));
    }

    private UserEntity requireActiveUser(UUID userId) {
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
        if (user.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta fue eliminada.");
        }
        return user;
    }

    private void ensureCanDelete(UUID userId) {
        for (WorkspaceEntity workspace : workspaces.findByOwnerId(userId)) {
            int memberCount = members.findByIdWorkspaceId(workspace.getId()).size();
            if (memberCount > 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Transfiere la propiedad o elimina miembros antes de borrar esta cuenta.");
            }
        }
    }

    private void removeOwnedSingleMemberWorkspaces(UUID userId) {
        for (WorkspaceEntity workspace : workspaces.findByOwnerId(userId)) {
            workspaces.delete(workspace);
        }
    }

    private void removeNonOwnedMemberships(UUID userId) {
        for (WorkspaceMemberEntity membership : members.findByIdUserId(userId)) {
            WorkspaceEntity workspace = workspaces.findById(membership.getWorkspaceId()).orElse(null);
            if (workspace == null || !userId.equals(workspace.getOwnerId())) {
                members.delete(membership);
            }
        }
    }

    private Map<String, Object> profile(UserEntity user) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", user.getId());
        value.put("nombre", user.getNombre());
        value.put("apellido", user.getApellido());
        value.put("email", user.getEmail());
        value.put("ciudad", user.getCiudad());
        value.put("pais", user.getPais());
        value.put("moneda", user.getMoneda());
        value.put("locale", user.getLocale());
        value.put("tipoCuenta", user.getTipoCuenta());
        value.put("emailVerified", user.isEmailVerified());
        return value;
    }

    private Map<String, Object> settings(UserSettingsEntity entity) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("theme", entity.getTheme().name());
        value.put("locale", entity.getLocale());
        value.put("timeZone", entity.getTimeZone());
        value.put("moneyFormat", entity.getMoneyFormat().name());
        value.put("notifPresupuesto", entity.isNotifPresupuesto());
        value.put("notifMetas", entity.isNotifMetas());
        value.put("notifConsejos", entity.isNotifConsejos());
        value.put("version", entity.getVersion());
        return value;
    }

    private List<Map<String, Object>> sessions(UUID userId) {
        List<Map<String, Object>> value = new ArrayList<>();
        for (RefreshTokenEntity token : refreshTokens.listActive(userId)) {
            Map<String, Object> session = new LinkedHashMap<>();
            session.put("id", token.getId());
            session.put("createdAt", token.getCreatedAt());
            session.put("lastUsedAt", token.getLastUsedAt());
            session.put("expiresAt", token.getExpiresAt());
            value.add(session);
        }
        return value;
    }

    private List<Map<String, Object>> workspaces(UUID userId) {
        List<Map<String, Object>> value = new ArrayList<>();
        for (WorkspaceMemberEntity membership : members.findByIdUserId(userId)) {
            WorkspaceEntity workspace = workspaces.findById(membership.getWorkspaceId()).orElse(null);
            if (workspace == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", workspace.getId());
            row.put("nombre", workspace.getNombre());
            row.put("tipo", workspace.getTipo().name());
            row.put("role", membership.getRole().name());
            row.put("ownerId", workspace.getOwnerId());
            row.put("memberCount", members.findByIdWorkspaceId(workspace.getId()).size());
            value.add(row);
        }
        return value;
    }
}
