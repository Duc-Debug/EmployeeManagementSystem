package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationAuditLogJpaEntity;

public interface SpringDataNotificationAuditLogRepository extends JpaRepository<NotificationAuditLogJpaEntity, Long> {

    List<NotificationAuditLogJpaEntity> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId);
}
