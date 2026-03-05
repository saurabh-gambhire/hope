package com.hope.master_service.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtpEntity, Long> {

    Optional<PasswordResetOtpEntity> findByResetToken(UUID resetToken);

    @Query("SELECT o FROM PasswordResetOtpEntity o WHERE o.user.id = :userId AND o.used = false AND o.expiresAt > CURRENT_TIMESTAMP ORDER BY o.createdAt DESC")
    Optional<PasswordResetOtpEntity> findLatestActiveOtpByUserId(Long userId);

    @Modifying
    @Query("UPDATE PasswordResetOtpEntity o SET o.used = true WHERE o.user.id = :userId AND o.used = false")
    void invalidateAllOtpsForUser(Long userId);
}
