package com.finanzas.backend.service;

import com.finanzas.backend.domain.AccountTokenEntity;
import com.finanzas.backend.domain.AccountTokenType;
import com.finanzas.backend.repo.AccountTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AccountTokenService {
    private final AccountTokenRepository tokens;
    private final SecureRandom random = new SecureRandom();
    private final long emailVerificationSeconds;
    private final long passwordResetSeconds;

    public AccountTokenService(AccountTokenRepository tokens,
                               @Value("${finanzas.account.email-verification-hours:48}") long emailVerificationHours,
                               @Value("${finanzas.account.password-reset-minutes:30}") long passwordResetMinutes) {
        this.tokens = tokens;
        this.emailVerificationSeconds = Math.max(1, emailVerificationHours) * 60L * 60L;
        this.passwordResetSeconds = Math.max(1, passwordResetMinutes) * 60L;
    }

    @Transactional
    public String issue(UUID userId, AccountTokenType type) {
        Instant now = Instant.now();
        tokens.findByUserIdAndTypeAndConsumedAtIsNull(userId, type)
                .forEach(token -> token.consume(now));
        String rawToken = randomToken();
        tokens.save(new AccountTokenEntity(userId, type, hash(rawToken), now.plusSeconds(ttlSeconds(type))));
        return rawToken;
    }

    @Transactional
    public UUID consume(String rawToken, AccountTokenType type) {
        AccountTokenEntity token = tokens.findByTokenHashAndTypeAndConsumedAtIsNull(hash(rawToken), type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido."));
        Instant now = Instant.now();
        if (token.isExpired(now)) {
            token.consume(now);
            throw new ResponseStatusException(HttpStatus.GONE, "Token expirado.");
        }
        token.consume(now);
        return token.getUserId();
    }

    @Transactional
    public long deleteExpired(Instant olderThan) {
        return tokens.deleteByExpiresAtBefore(olderThan);
    }

    private long ttlSeconds(AccountTokenType type) {
        return type == AccountTokenType.PASSWORD_RESET ? passwordResetSeconds : emailVerificationSeconds;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((rawToken == null ? "" : rawToken).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no esta disponible.", ex);
        }
    }
}
