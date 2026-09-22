package com.hrm.employeemanagement.infrastructure.adapter.inbound.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.ScanOverloadAndAlertUseCase;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.NotificationDedupConfigRepositoryPort;
import com.hrm.employeemanagement.domain.notification.dedup.NotificationDedupConfig;

/**
 * Tác vụ nền rà soát quá tải định kỳ (TC-01, TC-02, QTN-19).
 * Tần suất quét được điều phối động theo scanIntervalMinutes từ cấu hình hệ thống (5 - 1440 phút).
 */
@Component
@ConditionalOnProperty(name = "app.notification.overload-scan-enabled", havingValue = "true", matchIfMissing = true)
public class OverloadAlertScanJob implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(OverloadAlertScanJob.class);
    private static final int DEFAULT_INTERVAL_MINUTES = 60;

    private final ScanOverloadAndAlertUseCase scanOverloadAndAlertUseCase;
    private final NotificationDedupConfigRepositoryPort configRepositoryPort;

    public OverloadAlertScanJob(
            ScanOverloadAndAlertUseCase scanOverloadAndAlertUseCase,
            NotificationDedupConfigRepositoryPort configRepositoryPort
    ) {
        this.scanOverloadAndAlertUseCase = Objects.requireNonNull(scanOverloadAndAlertUseCase, "scanOverloadAndAlertUseCase must not be null");
        this.configRepositoryPort = Objects.requireNonNull(configRepositoryPort, "configRepositoryPort must not be null");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                this::executeScan,
                triggerContext -> {
                    int intervalMinutes = DEFAULT_INTERVAL_MINUTES;
                    try {
                        NotificationDedupConfig config = configRepositoryPort.loadConfig();
                        if (config != null) {
                            if (!config.isEnabled()) {
                                // Khi cơ chế tắt, kiểm tra lại sau 5 phút để kích hoạt lại nếu Admin bật
                                return Instant.now().plus(Duration.ofMinutes(5));
                            }
                            intervalMinutes = config.getScanIntervalMinutes();
                        }
                    } catch (Exception ex) {
                        log.warn("Không thể tải cấu hình scanIntervalMinutes, sử dụng giá trị mặc định {} phút: {}",
                                DEFAULT_INTERVAL_MINUTES, ex.getMessage());
                    }

                    Instant lastActual = triggerContext.lastActualExecution();
                    Instant base = (lastActual != null) ? lastActual : Instant.now();
                    return base.plus(Duration.ofMinutes(intervalMinutes));
                }
        );
    }

    public void executeScan() {
        log.info("Bắt đầu tác vụ nền OverloadAlertScanJob...");
        try {
            NotificationDedupConfig config = configRepositoryPort.loadConfig();
            if (config != null && !config.isEnabled()) {
                log.info("Cơ chế chống gửi trùng thông báo đang tắt. Bỏ qua tác vụ quét định kỳ.");
                return;
            }

            OverloadScanResult result = scanOverloadAndAlertUseCase.scanCurrentWeek();
            log.info("Hoàn tất OverloadAlertScanJob: tổng={}; quá_tải={}; gửi_mới={}; bỏ_qua_trùng={}; giải_phóng={}",
                    result.totalScanned(), result.overloadedCount(), result.newlyAlertedCount(),
                    result.skippedDedupCount(), result.resolvedCount());
        } catch (Exception e) {
            log.error("Lỗi trong quá trình thực thi OverloadAlertScanJob", e);
        }
    }
}
