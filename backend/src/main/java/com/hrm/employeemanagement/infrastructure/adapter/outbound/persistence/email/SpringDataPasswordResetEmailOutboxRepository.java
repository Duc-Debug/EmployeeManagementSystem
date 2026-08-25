package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.email;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface SpringDataPasswordResetEmailOutboxRepository
        extends JpaRepository<PasswordResetEmailOutboxJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetEmailOutboxJpaEntity>
            findFirstByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(Instant now);
}
