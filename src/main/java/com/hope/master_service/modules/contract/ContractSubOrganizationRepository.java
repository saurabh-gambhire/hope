package com.hope.master_service.modules.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractSubOrganizationRepository extends JpaRepository<ContractSubOrganizationEntity, Long> {

    Optional<ContractSubOrganizationEntity> findByUuid(UUID uuid);

    List<ContractSubOrganizationEntity> findBySubOrganizationId(Long subOrganizationId);

    boolean existsByContractIdAndSubOrganizationId(Long contractId, Long subOrganizationId);

    long countByContractId(Long contractId);
}
