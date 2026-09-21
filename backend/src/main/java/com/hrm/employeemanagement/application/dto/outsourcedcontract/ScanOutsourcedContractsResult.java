package com.hrm.employeemanagement.application.dto.outsourcedcontract;

import java.time.LocalDateTime;

/**
 * DTO kết quả chạy tác vụ rà soát thời hạn hợp đồng thuê ngoài.
 */
public record ScanOutsourcedContractsResult(
        LocalDateTime scannedAt,
        int totalScanned,
        int totalExpiringContractsFound,
        int notificationsSent,
        String details
) {}
