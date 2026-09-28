package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.notification.entity.NotificationJpaEntity;

/**
 * Spring Data JPA Repository chuyên biệt kiểm tra trùng thông báo nhắc hạn (NCL-11-CN-004).
 * Độc lập, không sửa đổi code của các repository khác.
 */
@Repository
public interface SpringDataTaskDueReminderNotificationRepository extends JpaRepository<NotificationJpaEntity, Long> {

    boolean existsByRecipientIdAndTypeAndTargetId(Long recipientId, String type, Long targetId);

    boolean existsByRecipientIdAndTypeAndTargetIdAndContentContaining(Long recipientId, String type, Long targetId, String content);
}
