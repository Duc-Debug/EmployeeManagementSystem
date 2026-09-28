package com.hrm.employeemanagement.application.port.inbound.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;

import java.util.List;

public interface GetPendingApprovalsUseCase {
    List<WorkLogResult> getPendingApprovals();
}
