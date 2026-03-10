package com.hope.master_service.modules.homework;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeworkDocumentVersionRepository extends JpaRepository<HomeworkDocumentVersionEntity, Long> {

    List<HomeworkDocumentVersionEntity> findByHomeworkIdOrderByVersionDesc(Long homeworkId);
}
