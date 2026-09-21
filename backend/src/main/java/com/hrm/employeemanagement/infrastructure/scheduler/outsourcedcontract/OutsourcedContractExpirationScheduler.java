package com.hrm.employeemanagement.infrastructure.scheduler.outsourcedcontract;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.outsourcedcontract.ScanOutsourcedContractsResult;
import com.hrm.employeemanagement.application.port.inbound.outsourcedcontract.ScanOutsourcedContractExpirationsUseCase;

/**
 * Tác vụ nền quét rà soát tự động thời hạn các hợp đồng thuê ngoài định kỳ hàng ngày (06:00 AM)
 * và phát hiện các phân bổ vắt qua ngày hết hạn theo quy tắc QTN-21.
 */
@Component
public class OutsourcedContractExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutsourcedContractExpirationScheduler.class);

    private final ScanOutsourcedContractExpirationsUseCase scanOutsourcedContractExpirationsUseCase;

    public OutsourcedContractExpirationScheduler(
            ScanOutsourcedContractExpirationsUseCase scanOutsourcedContractExpirationsUseCase
    ) {
        this.scanOutsourcedContractExpirationsUseCase = Objects.requireNonNull(
                scanOutsourcedContractExpirationsUseCase, "scanOutsourcedContractExpirationsUseCase must not be null"
        );
    }

    /**
     * Chạy định kỳ lúc 06:00 sáng hàng ngày.
     */
    @Scheduled(cron = "${app.scheduler.outsourced-contract-scan.cron:0 0 6 * * *}")
    public void runDailyOutsourcedContractScan() {
        log.info("[SCHEDULED] Bắt đầu tác vụ rà soát thời hạn hợp đồng thuê ngoài tự động hàng ngày...");
        try {
            ScanOutsourcedContractsResult result = scanOutsourcedContractExpirationsUseCase.execute(false);
            log.info("[SCHEDULED] Kết thúc rà soát: {}", result.details());
        } catch (Exception e) {
            log.error("[SCHEDULED] Lỗi trong quá trình rà soát thời hạn hợp đồng thuê ngoài tự động: ", e);
        }
    }
}
