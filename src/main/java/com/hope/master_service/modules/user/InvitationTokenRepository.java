package com.hope.master_service.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationTokenRepository extends JpaRepository<InvitationTokenEntity, Long> {

    Optional<InvitationTokenEntity> findByToken(UUID token);

    @Modifying
    @Query("UPDATE InvitationTokenEntity t SET t.used = true WHERE t.user.id = :userId AND t.used = false")
    void invalidateAllTokensForUser(Long userId);
}
