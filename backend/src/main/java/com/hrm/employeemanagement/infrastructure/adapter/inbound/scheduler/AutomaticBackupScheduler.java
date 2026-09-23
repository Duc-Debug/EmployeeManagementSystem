package com.hrm.employeemanagement.infrastructure.adapter.inbound.scheduler;

import com.hrm.employeemanagement.application.port.inbound.CreateBackupUseCase;
import com.hrm.employeemanagement.application.port.inbound.GetBackupScheduleUseCase;
import com.hrm.employeemanagement.domain.backup.BackupSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class AutomaticBackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutomaticBackupScheduler.class);

    private final GetBackupScheduleUseCase getBackupScheduleUseCase;
    private final CreateBackupUseCase createBackupUseCase;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isRetentionRunning = new AtomicBoolean(false);

    public AutomaticBackupScheduler(
            GetBackupScheduleUseCase getBackupScheduleUseCase,
            CreateBackupUseCase createBackupUseCase
    ) {
        this.getBackupScheduleUseCase = getBackupScheduleUseCase;
        this.createBackupUseCase = createBackupUseCase;
    }

    @Scheduled(cron = "0 * * * * *") // Chạy mỗi phút kiểm tra lịch sao lưu tự động
    public void checkAndExecuteScheduledBackup() {
        if (!isRunning.compareAndSet(false, true)) {
            log.debug("Tiến trình sao lưu tự động đang chạy, bỏ qua lần kích hoạt này.");
            return;
        }

        try {
            BackupSchedule schedule = getBackupScheduleUseCase.getSchedule();
            if (schedule == null || !schedule.isEnabled() || schedule.getNextRunAt() == null) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(schedule.getNextRunAt())) {
                log.info("Bắt đầu thực thi sao lưu tự động theo lịch (Lịch hẹn: {}, Hiện tại: {})",
                        schedule.getNextRunAt(), now);
                createBackupUseCase.executeAutomaticBackup();
            }
        } catch (Exception e) {
            log.error("Lỗi trong quá trình kiểm tra lịch sao lưu tự động: {}", e.getMessage(), e);
        } finally {
            isRunning.set(false);
        }
    }

    @Scheduled(cron = "0 0 * * * *") // Chạy mỗi giờ độc lập để dọn dẹp các bản sao lưu hết hạn lưu trữ
    public void cleanupExpiredBackups() {
        if (!isRetentionRunning.compareAndSet(false, true)) {
            log.debug("Tiến trình dọn dẹp bản sao lưu hết hạn đang chạy, bỏ qua.");
            return;
        }

        try {
            log.info("Bắt đầu chạy tiến trình định kỳ dọn dẹp các bản sao lưu hết hạn lưu trữ (Retention Policy)");
            createBackupUseCase.applyRetentionPolicy();
        } catch (Exception e) {
            log.error("Lỗi trong quá trình dọn dẹp bản sao lưu hết hạn: {}", e.getMessage(), e);
        } finally {
            isRetentionRunning.set(false);
        }
    }
}
