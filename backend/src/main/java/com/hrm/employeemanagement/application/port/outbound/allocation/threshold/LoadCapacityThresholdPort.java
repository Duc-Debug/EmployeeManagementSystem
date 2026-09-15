package com.hrm.employeemanagement.application.port.outbound.allocation.threshold;

import java.util.Optional;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;
import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdScope;

/**
 * Outbound port tra cứu cấu hình ngưỡng năng lực từ cơ sở dữ liệu.
 */
public interface LoadCapacityThresholdPort {

    Optional<CapacityThresholdConfig> findByScope(CapacityThresholdScope scopeType, Long orgUnitId);

    Optional<CapacityThresholdConfig> findByScopeKey(String scopeKey);
}
