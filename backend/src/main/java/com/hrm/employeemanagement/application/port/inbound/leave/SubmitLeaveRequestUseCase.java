package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveRequestResult;
import com.hrm.employeemanagement.application.dto.leave.SubmitLeaveRequestCommand;

public interface SubmitLeaveRequestUseCase {
    LeaveRequestResult submitLeaveRequest(SubmitLeaveRequestCommand command);
}
