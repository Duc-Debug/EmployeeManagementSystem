package com.hrm.employeemanagement.domain.scenario;

import java.math.BigDecimal;

/**
 * Record lưu trữ chi tiết tình trạng thiếu hụt nhu cầu kịch bản theo từng tuần
 * khi năng lực khả dụng còn lại của nhân sự không đáp ứng đủ.
 */
public record UnfulfilledDemandDetail(
        Long demandId,
        String demandName,
        int year,
        int weekNumber,
        BigDecimal requestedHours,
        BigDecimal appliedHours,
        BigDecimal unfulfilledHours
) {}

