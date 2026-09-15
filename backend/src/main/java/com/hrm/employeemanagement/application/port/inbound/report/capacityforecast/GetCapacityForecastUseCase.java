package com.hrm.employeemanagement.application.port.inbound.report.capacityforecast;

import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastQuery;
import com.hrm.employeemanagement.application.dto.report.capacityforecast.CapacityForecastResult;

/**
 * Inbound Use Case lấy báo cáo dự báo năng lực các tuần tương lai (NCL-10-CN-004).
 */
public interface GetCapacityForecastUseCase {
    CapacityForecastResult execute(CapacityForecastQuery query);
}
