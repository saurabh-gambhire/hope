package com.hope.master_service.modules.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractOfferingRepository extends JpaRepository<ContractOfferingEntity, Long> {

    Optional<ContractOfferingEntity> findByUuid(UUID uuid);

    List<ContractOfferingEntity> findByContractIdAndActiveTrue(Long contractId);

    boolean existsByContractIdAndOfferingNameIgnoreCase(Long contractId, String offeringName);
}
