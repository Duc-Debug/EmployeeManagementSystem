package com.hrm.employeemanagement.application.port.inbound.allocation.threshold;

import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdResult;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;

/**
 * Inbound port truy vấn cấu hình ngưỡng hiệu lực hiện hành.
 */
public interface GetCapacityThresholdUseCase {

    CapacityThresholdResult getEffectiveThreshold(CapacityThresholdScope scopeType, Long orgUnitId);
}
