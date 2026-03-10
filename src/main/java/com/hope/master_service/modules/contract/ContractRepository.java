package com.hope.master_service.modules.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<ContractEntity, Long>, JpaSpecificationExecutor<ContractEntity> {

    Optional<ContractEntity> findByUuid(UUID uuid);

    boolean existsByIdentifierIgnoreCase(String identifier);

    boolean existsByIdentifierIgnoreCaseAndUuidNot(String identifier, UUID uuid);

    List<ContractEntity> findByOrganizationIdAndArchiveFalseAndActiveTrue(Long organizationId);

    boolean existsByIdAndSubOrganizations_SubOrganization_Id(Long contractId, Long subOrganizationId);
}
