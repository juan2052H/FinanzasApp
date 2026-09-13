package com.finanzas.backend.repo;

import com.finanzas.backend.domain.TaxConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxConfigurationRepository extends JpaRepository<TaxConfigurationEntity, UUID> {
    Optional<TaxConfigurationEntity> findByWorkspaceIdAndTaxYear(UUID workspaceId, int taxYear);
    List<TaxConfigurationEntity> findByWorkspaceIdOrderByTaxYearDesc(UUID workspaceId);
}
