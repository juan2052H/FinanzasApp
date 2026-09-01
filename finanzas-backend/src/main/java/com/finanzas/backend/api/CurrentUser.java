package com.finanzas.backend.api;

import com.finanzas.backend.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;

import java.util.UUID;

final class CurrentUser {
    private CurrentUser() {
    }

    static UUID id(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Sesion invalida.");
        }
        return user.userId();
    }
}
