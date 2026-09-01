package com.finanzas.backend.service;

import com.finanzas.backend.domain.RefreshTokenEntity;
import com.finanzas.backend.repo.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {
    @Test
    void tokenIsStoredHashedAndRevokedAfterConsume() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository, 14);
        UUID userId = UUID.randomUUID();

        String token = service.issue(userId);
        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(repository).save(captor.capture());
        RefreshTokenEntity stored = captor.getValue();

        assertNotEquals(token, stored.getTokenHash());
        when(repository.findByTokenHashAndRevokedAtIsNull(stored.getTokenHash())).thenReturn(Optional.of(stored));

        assertEquals(userId, service.consume(token));
        assertNotNull(stored.getLastUsedAt());
        assertNotNull(stored.getRevokedAt());
    }

    @Test
    void listsAndRevokesActiveSessionsById() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        RefreshTokenService service = new RefreshTokenService(repository, 14);
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        RefreshTokenEntity token = new RefreshTokenEntity(userId, "hash", Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(token, "id", tokenId);
        ReflectionTestUtils.setField(token, "createdAt", Instant.now());

        when(repository.findByUserIdAndRevokedAtIsNullOrderByCreatedAtDesc(userId)).thenReturn(List.of(token));
        when(repository.findByIdAndUserIdAndRevokedAtIsNull(tokenId, userId)).thenReturn(Optional.of(token));

        assertEquals(1, service.listActive(userId).size());
        assertTrue(service.revokeById(userId, tokenId));
        assertNotNull(token.getRevokedAt());
    }
}
