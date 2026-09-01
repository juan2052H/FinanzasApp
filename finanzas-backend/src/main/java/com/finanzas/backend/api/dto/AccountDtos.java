package com.finanzas.backend.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AccountDtos {
    private AccountDtos() {
    }

    public record DeleteAccountRequest(@Email @NotBlank String confirmEmail, String password) {
    }
}
