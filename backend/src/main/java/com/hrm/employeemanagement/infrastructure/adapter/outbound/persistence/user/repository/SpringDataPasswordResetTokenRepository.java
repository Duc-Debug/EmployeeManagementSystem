package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.PasswordResetTokenJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenJpaEntity, Long> {
    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PasswordResetTokenJpaEntity p WHERE p.tokenHash = :tokenHash")
    Optional<PasswordResetTokenJpaEntity> findByTokenHashWithLock(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("UPDATE PasswordResetTokenJpaEntity p SET p.used = true WHERE p.userId = :userId AND p.used = false")
    void invalidateActiveTokensByUserId(@Param("userId") Long userId);
}
