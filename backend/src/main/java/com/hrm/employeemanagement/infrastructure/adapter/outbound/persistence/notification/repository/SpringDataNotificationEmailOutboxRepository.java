package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEmailOutboxJpaEntity;

public interface SpringDataNotificationEmailOutboxRepository
        extends JpaRepository<NotificationEmailOutboxJpaEntity, Long> {
    Optional<NotificationEmailOutboxJpaEntity>
            findFirstByRecipientUserIdAndAvailableAtAndDigestFrequencyAndDeliveredAtIsNull(
                    Long recipientUserId, LocalDateTime availableAt, String digestFrequency);

    List<NotificationEmailOutboxJpaEntity>
            findTop100ByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByAvailableAtAsc(LocalDateTime now);
}
