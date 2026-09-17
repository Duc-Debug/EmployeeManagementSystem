package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationRecipientJpaEntity;

public interface SpringDataNotificationRecipientRepository extends JpaRepository<NotificationRecipientJpaEntity, Long> {

    Optional<NotificationRecipientJpaEntity> findByNotificationEventIdAndRecipientUserId(Long notificationEventId, Long recipientUserId);

    long countByRecipientUserIdAndIsDeletedFalseAndIsReadFalse(Long recipientUserId);

    @Query("SELECT r FROM NotificationRecipientJpaEntity r JOIN NotificationEventJpaEntity e ON r.notificationEventId = e.id " +
            "WHERE r.recipientUserId = :userId AND r.isDeleted = false " +
            "AND (:readFilter IS NULL OR r.isRead = :readFilter) " +
            "AND (:level IS NULL OR e.level = :level) " +
            "ORDER BY r.createdAt DESC")
    Page<NotificationRecipientJpaEntity> findByFilters(
            @Param("userId") Long userId,
            @Param("readFilter") Boolean readFilter,
            @Param("level") String level,
            Pageable pageable
    );

    @Query("SELECT COUNT(r) FROM NotificationRecipientJpaEntity r JOIN NotificationEventJpaEntity e ON r.notificationEventId = e.id " +
            "WHERE r.recipientUserId = :userId AND r.isDeleted = false " +
            "AND (:readFilter IS NULL OR r.isRead = :readFilter) " +
            "AND (:level IS NULL OR e.level = :level)")
    long countByFilters(
            @Param("userId") Long userId,
            @Param("readFilter") Boolean readFilter,
            @Param("level") String level
    );

    @Modifying
    @Query("UPDATE NotificationRecipientJpaEntity r SET r.isRead = true, r.readAt = :now " +
            "WHERE r.recipientUserId = :userId AND r.isDeleted = false AND r.isRead = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM NotificationRecipientJpaEntity r WHERE r.notificationEventId IN " +
            "(SELECT e.id FROM NotificationEventJpaEntity e WHERE e.createdAt < :cutoff)")
    int deleteRecipientsByEventCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
