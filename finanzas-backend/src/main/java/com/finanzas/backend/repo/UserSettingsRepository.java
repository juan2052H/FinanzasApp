package com.finanzas.backend.repo;

import com.finanzas.backend.domain.UserSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSettingsRepository extends JpaRepository<UserSettingsEntity, UUID> {
}
