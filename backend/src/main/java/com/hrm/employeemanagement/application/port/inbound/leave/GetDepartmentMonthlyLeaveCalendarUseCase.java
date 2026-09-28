package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.DepartmentMonthlyLeaveCalendarResult;
import com.hrm.employeemanagement.application.dto.leave.GetDepartmentMonthlyLeaveCalendarQuery;

/**
 * Input Port: Xem lịch nghỉ của bộ phận theo tháng (NCL-05-CN-006).
 */
public interface GetDepartmentMonthlyLeaveCalendarUseCase {

    DepartmentMonthlyLeaveCalendarResult execute(GetDepartmentMonthlyLeaveCalendarQuery query);
}
