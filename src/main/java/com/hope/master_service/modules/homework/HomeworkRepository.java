package com.hope.master_service.modules.homework;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HomeworkRepository extends JpaRepository<HomeworkEntity, Long>, JpaSpecificationExecutor<HomeworkEntity> {

    Optional<HomeworkEntity> findByUuid(UUID uuid);

    boolean existsByNameIgnoreCaseAndSubOrganizationIdAndContractId(String name, Long subOrganizationId, Long contractId);

    boolean existsByNameIgnoreCaseAndSubOrganizationIdAndContractIdAndUuidNot(String name, Long subOrganizationId, Long contractId, UUID uuid);
}
