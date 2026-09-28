package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationDedupConfigHistoryJpaEntity;

@Repository
public interface SpringDataNotificationDedupConfigHistoryRepository extends JpaRepository<NotificationDedupConfigHistoryJpaEntity, Long> {
}
