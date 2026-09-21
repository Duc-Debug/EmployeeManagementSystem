package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEmailOutboxJpaEntity;

public interface SpringDataNotificationEmailOutboxRepository
        extends JpaRepository<NotificationEmailOutboxJpaEntity, Long> {
    Optional<NotificationEmailOutboxJpaEntity>
            findFirstByRecipientUserIdAndAvailableAtAndDigestFrequencyAndDeliveredAtIsNull(
                    Long recipientUserId, LocalDateTime availableAt, String digestFrequency);

    List<NotificationEmailOutboxJpaEntity>
            findTop100ByDeliveredAtIsNullAndAvailableAtLessThanEqualOrderByAvailableAtAsc(LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationEmailOutboxJpaEntity e SET e.body = concat(concat(e.body, '\n'), :item) WHERE e.id = :id")
    int appendDigestItem(@Param("id") Long id, @Param("item") String item);
}
