package com.hope.master_service.modules.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long>, JpaSpecificationExecutor<OrganizationEntity> {

    Optional<OrganizationEntity> findByUuid(UUID uuid);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndUuidNot(String name, UUID uuid);

    Page<OrganizationEntity> findByArchiveFalse(Pageable pageable);

    Page<OrganizationEntity> findByArchiveTrue(Pageable pageable);
}
