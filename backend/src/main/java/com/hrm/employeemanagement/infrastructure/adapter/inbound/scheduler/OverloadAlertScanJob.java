package com.hrm.employeemanagement.infrastructure.adapter.inbound.scheduler;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.notification.dedup.OverloadScanResult;
import com.hrm.employeemanagement.application.port.inbound.notification.dedup.ScanOverloadAndAlertUseCase;

/**
 * Tác vụ nền rà soát quá tải định kỳ mỗi giờ (TC-01, TC-02, QTN-19).
 */
@Component
@ConditionalOnProperty(name = "app.notification.overload-scan-enabled", havingValue = "true", matchIfMissing = true)
public class OverloadAlertScanJob {

    private static final Logger log = LoggerFactory.getLogger(OverloadAlertScanJob.class);

    private final ScanOverloadAndAlertUseCase scanOverloadAndAlertUseCase;

    public OverloadAlertScanJob(ScanOverloadAndAlertUseCase scanOverloadAndAlertUseCase) {
        this.scanOverloadAndAlertUseCase = Objects.requireNonNull(scanOverloadAndAlertUseCase, "scanOverloadAndAlertUseCase must not be null");
    }

    @Scheduled(cron = "${app.notification.overload-scan-cron:0 0 * * * ?}")
    public void executeScan() {
        log.info("Bắt đầu tác vụ nền OverloadAlertScanJob...");
        try {
            OverloadScanResult result = scanOverloadAndAlertUseCase.scanCurrentWeek();
            log.info("Hoàn tất OverloadAlertScanJob: tổng={}; quá_tải={}; gửi_mới={}; bỏ_qua_trùng={}; giải_phóng={}",
                    result.totalScanned(), result.overloadedCount(), result.newlyAlertedCount(),
                    result.skippedDedupCount(), result.resolvedCount());
        } catch (Exception e) {
            log.error("Lỗi trong quá trình thực thi OverloadAlertScanJob", e);
        }
    }
}
