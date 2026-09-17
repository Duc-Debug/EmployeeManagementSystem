package com.hrm.employeemanagement.infrastructure.adapter.inbound.scheduler;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.inbound.notification.PurgeExpiredNotificationsUseCase;

/**
 * Job định kỳ quét và xóa vĩnh viễn (hard delete) các thông báo cũ quá 45 ngày (TC-05).
 * Chạy hằng ngày vào khung giờ thấp điểm (off-peak: 02:00 AM).
 */
@Component
public class NotificationRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetentionScheduler.class);

    private final PurgeExpiredNotificationsUseCase purgeExpiredNotificationsUseCase;
    private final com.hrm.employeemanagement.infrastructure.adapter.outbound.lock.DbDistributedLockService lockService;

    public NotificationRetentionScheduler(
            PurgeExpiredNotificationsUseCase purgeExpiredNotificationsUseCase,
            com.hrm.employeemanagement.infrastructure.adapter.outbound.lock.DbDistributedLockService lockService
    ) {
        this.purgeExpiredNotificationsUseCase = Objects.requireNonNull(purgeExpiredNotificationsUseCase, "purgeExpiredNotificationsUseCase must not be null");
        this.lockService = Objects.requireNonNull(lockService, "lockService must not be null");
    }

    @Scheduled(cron = "${app.notification.retention-cron:0 0 2 * * ?}")
    public void runRetentionCleanup() {
        lockService.executeWithLock("notification_retention_purge", 0, () -> {
            log.info("Bắt đầu thực thi job dọn dẹp thông báo quá hạn (retention 45 ngày)...");
            try {
                long purgedCount = purgeExpiredNotificationsUseCase.execute();
                log.info("Hoàn tất job dọn dẹp thông báo. Đã xóa vĩnh viễn {} bản ghi quá hạn.", purgedCount);
            } catch (Exception e) {
                log.error("Lỗi khi thực thi job dọn dẹp retention thông báo", e);
            }
        });
    }
}
