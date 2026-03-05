package com.hope.master_service.modules.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, Long> {

    Optional<PatientEntity> findByUuid(UUID uuid);

    Optional<PatientEntity> findByUserEmail(String email);

    Optional<PatientEntity> findByMrn(String mrn);

    Page<PatientEntity> findByUserArchiveFalse(Pageable pageable);
}
