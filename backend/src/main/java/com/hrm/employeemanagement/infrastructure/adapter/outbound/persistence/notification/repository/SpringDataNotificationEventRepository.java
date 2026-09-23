package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationEventJpaEntity;

public interface SpringDataNotificationEventRepository extends JpaRepository<NotificationEventJpaEntity, Long> {

    Optional<NotificationEventJpaEntity> findBySourceEventKey(String sourceEventKey);

    @Modifying
    @Query("UPDATE NotificationEventJpaEntity e SET e.message = CASE WHEN e.message = '' THEN :item ELSE concat(concat(e.message, '\n'), :item) END WHERE e.id = :eventId")
    int appendDigestItem(@Param("eventId") Long eventId, @Param("item") String item);

    @Modifying
    @Query("DELETE FROM NotificationEventJpaEntity e WHERE e.createdAt < :cutoff AND NOT EXISTS (SELECT 1 FROM NotificationRecipientJpaEntity r WHERE r.notificationEventId = e.id)")
    int deleteOrphanEventsOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
