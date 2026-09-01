package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.AuthDtos;
import com.finanzas.backend.domain.AccountTokenType;
import com.finanzas.backend.domain.AuthProvider;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.repo.UserRepository;
import com.finanzas.backend.repo.WorkspaceMemberRepository;
import com.finanzas.backend.repo.WorkspaceRepository;
import com.finanzas.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthApplicationServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
    private final WorkspaceMemberRepository members = mock(WorkspaceMemberRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final RefreshTokenService refreshTokens = mock(RefreshTokenService.class);
    private final LoginRateLimiter rateLimiter = mock(LoginRateLimiter.class);
    private final DefaultCategoryService defaultCategories = mock(DefaultCategoryService.class);
    private final AuditLogService auditLogs = mock(AuditLogService.class);
    private final AccountTokenService accountTokens = mock(AccountTokenService.class);
    private final EmailDeliveryService emailDelivery = mock(EmailDeliveryService.class);
    private final AuthApplicationService auth = new AuthApplicationService(
            users,
            workspaces,
            members,
            passwordEncoder,
            jwtService,
            refreshTokens,
            rateLimiter,
            defaultCategories,
            auditLogs,
            accountTokens,
            emailDelivery,
            "google-client",
            "https://finanzas.example.com/");

    @Test
    void emailVerificationSendsLinkAndConfirmMarksUser() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId, "verifica@example.com");

        when(users.findByEmail("verifica@example.com")).thenReturn(Optional.of(user));
        when(accountTokens.issue(userId, AccountTokenType.EMAIL_VERIFICATION)).thenReturn("verify-token");

        auth.requestEmailVerification(new AuthDtos.EmailRequest("VERIFICA@EXAMPLE.COM"));

        verify(emailDelivery).sendAccountEmail(
                eq("verifica@example.com"),
                contains("Confirma"),
                contains("https://finanzas.example.com/verify-email?token=verify-token"));

        when(accountTokens.consume("verify-token", AccountTokenType.EMAIL_VERIFICATION)).thenReturn(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        AuthDtos.UserResponse response = auth.confirmEmail(new AuthDtos.TokenRequest("verify-token"));

        assertTrue(user.isEmailVerified());
        assertTrue(response.emailVerified());
    }

    @Test
    void passwordResetRequestDoesNotRevealUnknownEmail() {
        when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        auth.requestPasswordReset(new AuthDtos.EmailRequest("missing@example.com"));

        verifyNoInteractions(accountTokens, emailDelivery);
    }

    @Test
    void passwordResetConsumesTokenUpdatesPasswordAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId, "reset@example.com");

        when(accountTokens.consume("reset-token", AccountTokenType.PASSWORD_RESET)).thenReturn(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nueva-segura")).thenReturn("encoded-password");

        auth.confirmPasswordReset(new AuthDtos.PasswordResetConfirmRequest("reset-token", "nueva-segura"));

        verify(refreshTokens).revokeAll(userId);
        assertEquals("encoded-password", user.getPasswordHash());
    }

    @Test
    void changePasswordValidatesCurrentPasswordAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        UserEntity user = user(userId, "change@example.com");

        when(users.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("actual", "old-password")).thenReturn(true);
        when(passwordEncoder.encode("nueva-segura")).thenReturn("new-hash");

        auth.changePassword(userId, new AuthDtos.PasswordChangeRequest("actual", "nueva-segura"));

        assertEquals("new-hash", user.getPasswordHash());
        verify(refreshTokens).revokeAll(userId);
        verify(rateLimiter).recordSuccess("change@example.com");
    }

    private UserEntity user(UUID userId, String email) {
        UserEntity user = new UserEntity("Nombre", "Apellido", email, "old-password", "COP", "PERSONAL", AuthProvider.PASSWORD);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
