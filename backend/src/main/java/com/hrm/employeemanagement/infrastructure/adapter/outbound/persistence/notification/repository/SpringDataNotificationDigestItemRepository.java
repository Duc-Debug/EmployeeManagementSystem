package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationDigestItemJpaEntity;

public interface SpringDataNotificationDigestItemRepository
        extends JpaRepository<NotificationDigestItemJpaEntity, Long> {
}
