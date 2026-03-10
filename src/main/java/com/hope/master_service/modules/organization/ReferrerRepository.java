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
public interface ReferrerRepository extends JpaRepository<ReferrerEntity, Long>, JpaSpecificationExecutor<ReferrerEntity> {

    Optional<ReferrerEntity> findByUuid(UUID uuid);

    Page<ReferrerEntity> findByOrganizationIdAndArchiveFalse(Long organizationId, Pageable pageable);

    List<ReferrerEntity> findByOrganizationIdAndArchiveFalseAndActiveTrue(Long organizationId);

    long countByOrganizationIdAndArchiveFalseAndActiveTrue(Long organizationId);
}
