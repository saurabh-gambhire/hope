package com.hope.master_service.modules.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubOrganizationRepository extends JpaRepository<SubOrganizationEntity, Long>, JpaSpecificationExecutor<SubOrganizationEntity> {

    Optional<SubOrganizationEntity> findByUuid(UUID uuid);

    Page<SubOrganizationEntity> findByOrganizationIdAndArchiveFalse(Long organizationId, Pageable pageable);

    List<SubOrganizationEntity> findByOrganizationIdAndArchiveFalseAndActiveTrue(Long organizationId);

    boolean existsByNameIgnoreCaseAndOrganizationId(String name, Long organizationId);

    boolean existsByNameIgnoreCaseAndOrganizationIdAndUuidNot(String name, Long organizationId, UUID uuid);

    long countByOrganizationIdAndArchiveFalseAndActiveTrue(Long organizationId);
}
