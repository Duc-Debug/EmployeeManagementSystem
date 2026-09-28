package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationPreferenceJpaEntity;

@Repository
public interface SpringDataNotificationPreferenceRepository extends JpaRepository<NotificationPreferenceJpaEntity, Long> {
    Optional<NotificationPreferenceJpaEntity> findByUserId(Long userId);
}
