package com.finanzas.backend.service;

import com.finanzas.backend.domain.RefreshTokenEntity;
import com.finanzas.backend.repo.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokens;
    private final long ttlSeconds;

    public RefreshTokenService(RefreshTokenRepository refreshTokens,
                               @Value("${finanzas.jwt.refresh-token-days}") long refreshTokenDays) {
        this.refreshTokens = refreshTokens;
        this.ttlSeconds = Math.max(1, refreshTokenDays) * 24L * 60L * 60L;
    }

    @Transactional
    public String issue(UUID userId) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshTokenEntity(userId, hash(token), Instant.now().plusSeconds(ttlSeconds)));
        return token;
    }

    @Transactional
    public UUID consume(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Instant now = Instant.now();
        RefreshTokenEntity record = refreshTokens.findByTokenHashAndRevokedAtIsNull(hash(token)).orElse(null);
        if (record == null) {
            return null;
        }
        if (record.isExpired(now)) {
            record.revoke(now);
            return null;
        }
        record.markUsed(now);
        record.revoke(now);
        return record.getUserId();
    }

    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHashAndRevokedAtIsNull(hash(token))
                .ifPresent(record -> record.revoke(Instant.now()));
    }

    @Transactional
    public void revokeAll(UUID userId) {
        Instant now = Instant.now();
        refreshTokens.findByUserIdAndRevokedAtIsNull(userId).forEach(token -> token.revoke(now));
    }

    @Transactional(readOnly = true)
    public List<RefreshTokenEntity> listActive(UUID userId) {
        Instant now = Instant.now();
        return refreshTokens.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .filter(token -> !token.isExpired(now))
                .sorted(Comparator.comparing(RefreshTokenEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional
    public boolean revokeById(UUID userId, UUID tokenId) {
        RefreshTokenEntity token = refreshTokens.findByIdAndUserIdAndRevokedAtIsNull(tokenId, userId).orElse(null);
        if (token == null) {
            return false;
        }
        token.revoke(Instant.now());
        return true;
    }

    @Transactional
    public long deleteExpired(Instant olderThan) {
        return refreshTokens.deleteByExpiresAtBefore(olderThan);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible.", e);
        }
    }
}
