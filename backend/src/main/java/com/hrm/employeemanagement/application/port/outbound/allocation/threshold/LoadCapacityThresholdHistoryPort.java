package com.hrm.employeemanagement.application.port.outbound.allocation.threshold;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdHistoryResult;

/**
 * Outbound port đọc lịch sử kiểm toán của cấu hình ngưỡng năng lực (TC-04).
 */
public interface LoadCapacityThresholdHistoryPort {

    List<CapacityThresholdHistoryResult> loadHistory(String tableName, Long recordId);
}
