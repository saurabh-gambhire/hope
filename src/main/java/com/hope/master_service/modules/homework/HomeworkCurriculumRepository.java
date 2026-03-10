package com.hope.master_service.modules.homework;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HomeworkCurriculumRepository extends JpaRepository<HomeworkCurriculumEntity, Long> {

    Optional<HomeworkCurriculumEntity> findByUuid(UUID uuid);

    List<HomeworkCurriculumEntity> findBySubOrganizationIdAndContractIdOrderBySequenceOrder(Long subOrganizationId, Long contractId);

    List<HomeworkCurriculumEntity> findByHomeworkId(Long homeworkId);

    boolean existsBySubOrganizationIdAndContractIdAndHomeworkId(Long subOrganizationId, Long contractId, Long homeworkId);

    long countByHomeworkIdAndContractId(Long homeworkId, Long contractId);

    long countByHomeworkId(Long homeworkId);
}
