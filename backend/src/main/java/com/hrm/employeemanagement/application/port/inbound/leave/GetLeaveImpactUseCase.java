package com.hrm.employeemanagement.application.port.inbound.leave;

import com.hrm.employeemanagement.application.dto.leave.LeaveImpactResult;

public interface GetLeaveImpactUseCase {
    LeaveImpactResult getLeaveImpact(Long leaveRequestId);
}
