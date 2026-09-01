package com.finanzas.backend.service;

import com.finanzas.backend.api.dto.UserSettingsDtos;
import com.finanzas.backend.domain.AuthProvider;
import com.finanzas.backend.domain.UserEntity;
import com.finanzas.backend.domain.UserSettingsEntity;
import com.finanzas.backend.repo.UserSettingsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSettingsServiceTest {
    private final UserSettingsRepository repository = mock(UserSettingsRepository.class);
    private final UserSettingsService service = new UserSettingsService(repository);

    @Test
    void createsDefaultSettingsWhenMissing() {
        UserEntity user = user("Ana", "ana@example.com");

        when(repository.findById(user.getId())).thenReturn(Optional.empty());
        when(repository.save(any(UserSettingsEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSettingsDtos.UserSettingsResponse response = service.get(user);

        assertEquals("LIGHT", response.theme());
        assertEquals("es-CO", response.locale());
        assertEquals("America/Bogota", response.timeZone());
        assertTrue(response.notifPresupuesto());
        verify(repository).save(any(UserSettingsEntity.class));
    }

    @Test
    void updatesThemeLocaleTimeZoneAndNotifications() {
        UserEntity user = user("Ana", "ana@example.com");
        UserSettingsEntity settings = new UserSettingsEntity(user.getId(), "es-CO");

        when(repository.findById(user.getId())).thenReturn(Optional.of(settings));

        UserSettingsDtos.UserSettingsResponse response = service.update(user,
                new UserSettingsDtos.UserSettingsPatchRequest(
                        "dark",
                        "en_US",
                        "UTC",
                        "CODE_SUFFIX",
                        false,
                        true,
                        false));

        assertEquals("DARK", response.theme());
        assertEquals("en-US", response.locale());
        assertEquals("UTC", response.timeZone());
        assertEquals("CODE_SUFFIX", response.moneyFormat());
        assertFalse(response.notifPresupuesto());
        assertTrue(response.notifMetas());
        assertFalse(response.notifConsejos());
        assertEquals("en-US", user.getLocale());
    }

    @Test
    void rejectsInvalidTimeZone() {
        UserEntity user = user("Ana", "ana@example.com");
        UserSettingsEntity settings = new UserSettingsEntity(user.getId(), "es-CO");
        when(repository.findById(user.getId())).thenReturn(Optional.of(settings));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.update(user,
                new UserSettingsDtos.UserSettingsPatchRequest(null, null, "Bogota-local", null, null, null, null)));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
    }

    private UserEntity user(String name, String email) {
        UserEntity user = new UserEntity(name, "", email, "hash", "COP", "PERSONAL", AuthProvider.PASSWORD);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        return user;
    }
}
