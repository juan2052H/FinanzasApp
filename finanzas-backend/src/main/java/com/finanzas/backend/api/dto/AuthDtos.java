package com.finanzas.backend.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank String nombre,
            String apellido,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8) String password,
            String moneda,
            String tipoCuenta) {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(@NotBlank String refreshToken) {
    }

    public record GoogleRequest(@NotBlank String authorizationCode, @NotBlank String codeVerifier, @NotBlank String redirectUri) {
    }

    public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
    }

    public record UserResponse(UUID id, String nombre, String apellido, String email, String moneda, String locale, String tipoCuenta, String avatarRef) {
    }

    public record UserPatchRequest(@NotBlank String nombre, String apellido, @Email @NotBlank String email, String moneda, String locale) {
    }
}
