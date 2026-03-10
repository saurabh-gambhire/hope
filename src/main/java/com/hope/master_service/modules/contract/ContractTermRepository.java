package com.hope.master_service.modules.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractTermRepository extends JpaRepository<ContractTermEntity, Long> {

    List<ContractTermEntity> findByContractIdOrderByStartDateAsc(Long contractId);
}
