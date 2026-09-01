package com.finanzas.backend.api;

import com.finanzas.backend.api.dto.AuthDtos;
import com.finanzas.backend.service.AuthApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthApplicationService auth;

    public AuthController(AuthApplicationService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return auth.login(request);
    }

    @PostMapping("/refresh")
    public AuthDtos.AuthResponse refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return auth.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AuthDtos.LogoutRequest request) {
        auth.logout(request.refreshToken());
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(Authentication authentication, @Valid @RequestBody AuthDtos.PasswordChangeRequest request) {
        auth.changePassword(CurrentUser.id(authentication), request);
    }

    @GetMapping("/sessions")
    public List<AuthDtos.SessionResponse> sessions(Authentication authentication) {
        return auth.listSessions(CurrentUser.id(authentication));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(Authentication authentication, @PathVariable UUID sessionId) {
        auth.revokeSession(CurrentUser.id(authentication), sessionId);
    }

    @DeleteMapping("/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeAllSessions(Authentication authentication) {
        auth.revokeAllSessions(CurrentUser.id(authentication));
    }

    @PostMapping("/email/verification/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestEmailVerification(@Valid @RequestBody AuthDtos.EmailRequest request) {
        auth.requestEmailVerification(request);
    }

    @PostMapping("/email/verification/confirm")
    public AuthDtos.UserResponse confirmEmail(@Valid @RequestBody AuthDtos.TokenRequest request) {
        return auth.confirmEmail(request);
    }

    @PostMapping("/password/reset/request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestPasswordReset(@Valid @RequestBody AuthDtos.EmailRequest request) {
        auth.requestPasswordReset(request);
    }

    @PostMapping("/password/reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPasswordReset(@Valid @RequestBody AuthDtos.PasswordResetConfirmRequest request) {
        auth.confirmPasswordReset(request);
    }

    @PostMapping("/google")
    public AuthDtos.AuthResponse google(@Valid @RequestBody AuthDtos.GoogleRequest request) {
        return auth.google(request);
    }
}
