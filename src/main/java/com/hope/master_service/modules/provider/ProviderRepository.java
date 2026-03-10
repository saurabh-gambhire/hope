package com.hope.master_service.modules.provider;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderEntity, Long>, JpaSpecificationExecutor<ProviderEntity> {

    Optional<ProviderEntity> findByUuid(UUID uuid);

    Optional<ProviderEntity> findByUserEmail(String email);

    Optional<ProviderEntity> findByNpi(Long npi);

    Page<ProviderEntity> findByUserArchiveFalse(Pageable pageable);

    boolean existsByNpi(Long npi);
}
