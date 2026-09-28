package com.hrm.employeemanagement.application.port.inbound.allocation.threshold;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdHistoryResult;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;

/**
 * Inbound port truy vấn lịch sử kiểm toán thay đổi cấu hình ngưỡng (TC-04).
 */
public interface GetCapacityThresholdHistoryUseCase {

    List<CapacityThresholdHistoryResult> getHistory(CapacityThresholdScope scopeType, Long orgUnitId);
}
