package com.hope.master_service.modules.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByUuid(UUID uuid);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByIamId(String iamId);

    Page<UserEntity> findByArchiveFalse(Pageable pageable);

    boolean existsByEmail(String email);
}
