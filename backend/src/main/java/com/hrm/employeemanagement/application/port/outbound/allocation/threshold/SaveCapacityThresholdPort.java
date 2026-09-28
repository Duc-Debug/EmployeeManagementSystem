package com.hrm.employeemanagement.application.port.outbound.allocation.threshold;

import com.hrm.employeemanagement.domain.allocation.threshold.CapacityThresholdConfig;

/**
 * Outbound port lưu trữ bản ghi cấu hình ngưỡng năng lực.
 */
public interface SaveCapacityThresholdPort {

    CapacityThresholdConfig save(CapacityThresholdConfig config);
}
