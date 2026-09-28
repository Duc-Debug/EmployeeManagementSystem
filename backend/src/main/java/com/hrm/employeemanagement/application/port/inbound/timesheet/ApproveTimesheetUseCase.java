package com.hrm.employeemanagement.application.port.inbound.timesheet;

import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;

import java.util.List;

public interface ApproveTimesheetUseCase {
    ApprovalResult approveEntry(Long entryId, Long version);
    ApprovalResult rejectEntry(Long entryId, Long version, String reason);

    record ApprovalResult(
            WorkLogResult entry,
            List<String> warnings
    ) {}
}
