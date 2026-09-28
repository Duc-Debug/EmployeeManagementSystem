package com.hrm.employeemanagement.application.port.inbound.dashboard.capacity;

import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardQuery;
import com.hrm.employeemanagement.application.dto.dashboard.capacity.CapacityDashboardResult;

/**
 * Inbound port cho Bảng điều khiển năng lực (NCL-10-CN-001).
 */
public interface GetCapacityDashboardUseCase {
    CapacityDashboardResult execute(CapacityDashboardQuery query);
}
