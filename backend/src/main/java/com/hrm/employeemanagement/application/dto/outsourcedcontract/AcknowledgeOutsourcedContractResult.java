package com.hrm.employeemanagement.application.dto.outsourcedcontract;

import java.time.LocalDateTime;

/**
 * Kết quả xác nhận xử lý cảnh báo hợp đồng thuê ngoài.
 */
public record AcknowledgeOutsourcedContractResult(
        Long employeeId,
        LocalDateTime acknowledgedAt,
        String status,
        String message
) {}
