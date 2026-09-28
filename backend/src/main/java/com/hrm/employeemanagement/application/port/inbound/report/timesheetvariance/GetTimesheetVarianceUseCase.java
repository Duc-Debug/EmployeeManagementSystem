package com.hrm.employeemanagement.application.port.inbound.report.timesheetvariance;

import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceQuery;
import com.hrm.employeemanagement.application.dto.report.timesheetvariance.TimesheetVarianceResult;

/**
 * Inbound Port / Use Case: Đối chiếu giờ phân bổ với giờ thực tế (NCL-09-CN-004).
 */
public interface GetTimesheetVarianceUseCase {

    TimesheetVarianceResult execute(TimesheetVarianceQuery query);
}
