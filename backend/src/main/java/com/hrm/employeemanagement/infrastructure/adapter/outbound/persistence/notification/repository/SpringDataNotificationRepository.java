package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationJpaEntity;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT n FROM NotificationJpaEntity n " +
            "WHERE n.recipientId = :recipientId AND n.createdAt <= CURRENT_TIMESTAMP ORDER BY n.createdAt DESC")
    List<NotificationJpaEntity> findReleasedByRecipientId(
            @org.springframework.data.repository.query.Param("recipientId") Long recipientId);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.isRead = true WHERE n.recipientId = :recipientId AND n.isRead = false")
    void markAllAsReadByRecipientId(@Param("recipientId") Long recipientId);

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.type = 'ALLOCATION_CHANGED' ORDER BY n.createdAt DESC")
    org.springframework.data.domain.Page<NotificationJpaEntity> findAllAllocationNotifications(
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.type = 'ALLOCATION_CHANGED' AND n.targetId IN :projectIds ORDER BY n.createdAt DESC")
    org.springframework.data.domain.Page<NotificationJpaEntity> findAllocationNotificationsByProjectIds(
            @Param("projectIds") List<Long> projectIds,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.type = 'ALLOCATION_CHANGED' AND n.recipientId = :recipientId AND n.targetId IN :projectIds ORDER BY n.createdAt DESC")
    org.springframework.data.domain.Page<NotificationJpaEntity> findByRecipientIdAndProjectIds(
            @Param("recipientId") Long recipientId,
            @Param("projectIds") List<Long> projectIds,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.type = 'ALLOCATION_CHANGED' AND n.recipientId = :recipientId ORDER BY n.createdAt DESC")
    org.springframework.data.domain.Page<NotificationJpaEntity> findByRecipientId(
            @Param("recipientId") Long recipientId,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT COUNT(n) FROM NotificationJpaEntity n WHERE n.type = 'ALLOCATION_CHANGED'")
    long countAllAllocationNotifications();

    @Query("SELECT COUNT(n) FROM NotificationJpaEntity n WHERE n.type = 'ALLOCATION_CHANGED' AND n.targetId IN :projectIds")
    long countAllocationNotificationsByProjectIds(@Param("projectIds") List<Long> projectIds);

    @Query("SELECT COUNT(n) FROM NotificationJpaEntity n WHERE n.type = 'ALLOCATION_CHANGED' AND n.recipientId = :recipientId AND n.targetId IN :projectIds")
    long countByRecipientIdAndProjectIds(
            @Param("recipientId") Long recipientId,
            @Param("projectIds") List<Long> projectIds
    );

    @Query("SELECT COUNT(n) FROM NotificationJpaEntity n WHERE n.type = 'ALLOCATION_CHANGED' AND n.recipientId = :recipientId")
    long countByRecipientId(@Param("recipientId") Long recipientId);
}

