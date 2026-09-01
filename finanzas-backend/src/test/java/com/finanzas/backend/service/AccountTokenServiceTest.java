package com.finanzas.backend.service;

import com.finanzas.backend.domain.AccountTokenEntity;
import com.finanzas.backend.domain.AccountTokenType;
import com.finanzas.backend.repo.AccountTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountTokenServiceTest {
    @Test
    void tokenIsStoredHashedAndConsumedOnce() {
        AccountTokenRepository repository = mock(AccountTokenRepository.class);
        AccountTokenService service = new AccountTokenService(repository, 48, 30);
        UUID userId = UUID.randomUUID();

        String rawToken = service.issue(userId, AccountTokenType.EMAIL_VERIFICATION);
        ArgumentCaptor<AccountTokenEntity> captor = ArgumentCaptor.forClass(AccountTokenEntity.class);
        verify(repository).save(captor.capture());
        AccountTokenEntity stored = captor.getValue();

        assertEquals(userId, stored.getUserId());
        assertEquals(AccountTokenType.EMAIL_VERIFICATION, stored.getType());
        assertNotEquals(rawToken, stored.getTokenHash());
        when(repository.findByTokenHashAndTypeAndConsumedAtIsNull(stored.getTokenHash(), AccountTokenType.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(stored));

        assertEquals(userId, service.consume(rawToken, AccountTokenType.EMAIL_VERIFICATION));
        assertNotNull(stored.getConsumedAt());
    }

    @Test
    void expiredTokenIsRejectedAndConsumed() {
        AccountTokenRepository repository = mock(AccountTokenRepository.class);
        AccountTokenService service = new AccountTokenService(repository, 48, 30);
        UUID userId = UUID.randomUUID();

        String rawToken = service.issue(userId, AccountTokenType.PASSWORD_RESET);
        ArgumentCaptor<AccountTokenEntity> captor = ArgumentCaptor.forClass(AccountTokenEntity.class);
        verify(repository).save(captor.capture());
        AccountTokenEntity stored = captor.getValue();
        ReflectionTestUtils.setField(stored, "expiresAt", Instant.now().minusSeconds(5));
        when(repository.findByTokenHashAndTypeAndConsumedAtIsNull(stored.getTokenHash(), AccountTokenType.PASSWORD_RESET))
                .thenReturn(Optional.of(stored));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.consume(rawToken, AccountTokenType.PASSWORD_RESET));

        assertEquals(HttpStatus.GONE, error.getStatusCode());
        assertNotNull(stored.getConsumedAt());
    }
}
