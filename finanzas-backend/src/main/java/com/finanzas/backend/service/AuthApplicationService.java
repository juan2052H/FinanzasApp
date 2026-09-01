package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.AuthDtos;
import com.finanzas.backend.domain.AccountTokenType;
import com.finanzas.backend.domain.AuthProvider;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.WorkspaceEntity;
import com.finanzas.backend.domain.WorkspaceMemberEntity;
import com.finanzas.backend.domain.WorkspaceRole;
import com.finanzas.backend.domain.WorkspaceType;
import com.finanzas.backend.repo.UserRepository;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import com.finanzas.backend.repo.WorkspaceRepository;
import com.finanzas.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AuthApplicationService {
    private static final Logger LOGGER = Logger.getLogger(AuthApplicationService.class.getName());

    private final UserRepository users;
    private final WorkspaceRepository workspaces;
    private final WorkspaceMemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;
    private final LoginRateLimiter rateLimiter;
    private final DefaultCategoryService defaultCategories;
    private final AuditLogService auditLogs;
    private final AccountTokenService accountTokens;
    private final EmailDeliveryService emailDelivery;
    private final String googleClientId;
    private final String publicBaseUrl;
    private final RestClient restClient = RestClient.create();

    public AuthApplicationService(UserRepository users,
                                  WorkspaceRepository workspaces,
                                  WorkspaceMemberRepository members,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService,
                                  RefreshTokenService refreshTokens,
                                  LoginRateLimiter rateLimiter,
                                  DefaultCategoryService defaultCategories,
                                  AuditLogService auditLogs,
                                  AccountTokenService accountTokens,
                                  EmailDeliveryService emailDelivery,
                                  @Value("${finanzas.google.client-id:}") String googleClientId,
                                  @Value("${finanzas.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.users = users;
        this.workspaces = workspaces;
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
        this.rateLimiter = rateLimiter;
        this.defaultCategories = defaultCategories;
        this.auditLogs = auditLogs;
        this.accountTokens = accountTokens;
        this.emailDelivery = emailDelivery;
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
        this.publicBaseUrl = trimTrailingSlash(publicBaseUrl == null ? "http://localhost:8080" : publicBaseUrl.trim());
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = UserEntity.normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        }
        UserEntity user = new UserEntity(
                request.nombre(),
                request.apellido(),
                email,
                passwordEncoder.encode(request.password()),
                request.moneda(),
                request.tipoCuenta(),
                AuthProvider.PASSWORD);
        users.save(user);

        WorkspaceType workspaceType = parseWorkspaceType(request.tipoCuenta());
        WorkspaceEntity workspace = workspaces.save(new WorkspaceEntity("Mi espacio", workspaceType, user.getId()));
        members.save(new WorkspaceMemberEntity(workspace.getId(), user.getId(), WorkspaceRole.OWNER));
        defaultCategories.seed(workspace.getId());
        auditLogs.record(workspace.getId(), user.getId(), "USER_REGISTERED", "User", user.getId(), Map.of(
                "provider", AuthProvider.PASSWORD.name(),
                "workspaceType", workspaceType.name()));
        trySendEmailVerification(user);
        return issue(user);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        String email = UserEntity.normalizeEmail(request.email());
        if (rateLimiter.isBlocked(email)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos fallidos.");
        }
        UserEntity user = users.findByEmail(email)
                .orElseThrow(() -> {
                    rateLimiter.recordFailure(email);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.");
                });
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas.");
        }
        rateLimiter.recordSuccess(email);
        return issue(user);
    }

    @Transactional
    public void requestEmailVerification(AuthDtos.EmailRequest request) {
        String email = UserEntity.normalizeEmail(request.email());
        users.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendEmailVerification);
    }

    @Transactional
    public AuthDtos.UserResponse confirmEmail(AuthDtos.TokenRequest request) {
        UUID userId = accountTokens.consume(request.token(), AccountTokenType.EMAIL_VERIFICATION);
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
        user.markEmailVerified(Instant.now());
        return toUserResponse(user);
    }

    @Transactional
    public void requestPasswordReset(AuthDtos.EmailRequest request) {
        String email = UserEntity.normalizeEmail(request.email());
        users.findByEmail(email).ifPresent(this::sendPasswordReset);
    }

    @Transactional
    public void confirmPasswordReset(AuthDtos.PasswordResetConfirmRequest request) {
        UUID userId = accountTokens.consume(request.token(), AccountTokenType.PASSWORD_RESET);
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        refreshTokens.revokeAll(userId);
        rateLimiter.recordSuccess(user.getEmail());
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(String refreshToken) {
        UUID userId = refreshTokens.consume(refreshToken);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalido.");
        }
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado."));
        return issue(user);
    }

    public void logout(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }

    @Transactional
    public AuthDtos.AuthResponse google(AuthDtos.GoogleRequest request) {
        if (googleClientId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Google Sign-In no esta configurado en este entorno.");
        }
        String idToken = exchangeGoogleCode(request);
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation("https://accounts.google.com");
        Jwt jwt = decoder.decode(idToken);
        if (!jwt.getAudience().contains(googleClientId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El token de Google no corresponde a este cliente.");
        }
        boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
        String email = UserEntity.normalizeEmail(jwt.getClaimAsString("email"));
        String name = jwt.getClaimAsString("given_name");
        String family = jwt.getClaimAsString("family_name");

        UserEntity user = users.findByEmail(email)
                .map(existing -> {
                    existing.linkGoogle();
                    return existing;
                })
                .orElseGet(() -> users.save(new UserEntity(
                        name == null || name.isBlank() ? email : name,
                        family,
                        email,
                        null,
                        "COP",
                        "PERSONAL",
                        AuthProvider.GOOGLE)));
        if (emailVerified) {
            user.markEmailVerified(Instant.now());
        }
        ensurePersonalWorkspace(user);
        return issue(user);
    }

    private String exchangeGoogleCode(AuthDtos.GoogleRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", googleClientId);
        form.add("code", request.authorizationCode());
        form.add("code_verifier", request.codeVerifier());
        form.add("redirect_uri", request.redirectUri());
        Map<?, ?> response = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);
        if (response == null || response.get("id_token") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google no devolvio un id_token valido.");
        }
        return String.valueOf(response.get("id_token"));
    }

    private AuthDtos.AuthResponse issue(UserEntity user) {
        return new AuthDtos.AuthResponse(
                jwtService.createAccessToken(user.getId(), user.getEmail()),
                refreshTokens.issue(user.getId()),
                toUserResponse(user));
    }

    private void ensurePersonalWorkspace(UserEntity user) {
        if (members.findByIdUserId(user.getId()).isEmpty()) {
            WorkspaceEntity workspace = workspaces.save(new WorkspaceEntity("Mi espacio", WorkspaceType.PERSONAL, user.getId()));
            members.save(new WorkspaceMemberEntity(workspace.getId(), user.getId(), WorkspaceRole.OWNER));
            defaultCategories.seed(workspace.getId());
            auditLogs.record(workspace.getId(), user.getId(), "WORKSPACE_CREATED", "Workspace", workspace.getId(), Map.of(
                    "provider", AuthProvider.GOOGLE.name(),
                    "workspaceType", WorkspaceType.PERSONAL.name()));
        }
    }

    private void trySendEmailVerification(UserEntity user) {
        try {
            sendEmailVerification(user);
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Cuenta registrada, pero no fue posible emitir email de verificacion.", ex);
        }
    }

    private void sendEmailVerification(UserEntity user) {
        String token = accountTokens.issue(user.getId(), AccountTokenType.EMAIL_VERIFICATION);
        String link = publicBaseUrl + "/verify-email?token=" + token;
        emailDelivery.sendAccountEmail(
                user.getEmail(),
                "Confirma tu correo en FinanzasApp",
                "Confirma tu correo en FinanzasApp:" + System.lineSeparator()
                        + link + System.lineSeparator()
                        + "Este enlace vence segun la politica de seguridad configurada.");
    }

    private void sendPasswordReset(UserEntity user) {
        String token = accountTokens.issue(user.getId(), AccountTokenType.PASSWORD_RESET);
        String link = publicBaseUrl + "/reset-password?token=" + token;
        emailDelivery.sendAccountEmail(
                user.getEmail(),
                "Restablece tu contrasena en FinanzasApp",
                "Restablece tu contrasena en FinanzasApp:" + System.lineSeparator()
                        + link + System.lineSeparator()
                        + "Si no solicitaste este cambio, ignora este mensaje.");
    }

    private String trimTrailingSlash(String value) {
        String result = value == null || value.trim().isEmpty() ? "http://localhost:8080" : value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private WorkspaceType parseWorkspaceType(String tipoCuenta) {
        if (tipoCuenta == null) {
            return WorkspaceType.PERSONAL;
        }
        String value = tipoCuenta.trim().toUpperCase(Locale.ROOT);
        if ("HOGAR".equals(value) || "HOUSEHOLD".equals(value)) {
            return WorkspaceType.HOUSEHOLD;
        }
        if ("NEGOCIO".equals(value) || "BUSINESS".equals(value)) {
            return WorkspaceType.BUSINESS;
        }
        return WorkspaceType.PERSONAL;
    }

    public static AuthDtos.UserResponse toUserResponse(UserEntity user) {
        return new AuthDtos.UserResponse(
                user.getId(),
                user.getNombre(),
                user.getApellido(),
                user.getEmail(),
                user.getCiudad(),
                user.getPais(),
                user.getMoneda(),
                user.getLocale(),
                user.getTipoCuenta(),
                user.getAvatarRef() == null || user.getAvatarRef().isBlank() ? "" : AvatarStorageService.publicRef(user.getId()),
                user.isEmailVerified());
    }
}
