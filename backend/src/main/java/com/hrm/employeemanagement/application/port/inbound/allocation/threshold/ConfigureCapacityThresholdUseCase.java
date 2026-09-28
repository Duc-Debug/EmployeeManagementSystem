package com.hrm.employeemanagement.application.port.inbound.allocation.threshold;

import com.hrm.employeemanagement.application.dto.allocation.threshold.CapacityThresholdResult;
import com.hrm.employeemanagement.application.dto.allocation.threshold.ConfigureCapacityThresholdCommand;

/**
 * Inbound port cho Use Case cấu hình ngưỡng cảnh báo quá tải và nhàn rỗi (NCL-07-CN-004).
 */
public interface ConfigureCapacityThresholdUseCase {

    CapacityThresholdResult configureThreshold(ConfigureCapacityThresholdCommand command);
}
