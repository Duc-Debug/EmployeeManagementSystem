package com.hrm.employeemanagement.infrastructure.adapter.outbound.timesheet;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.project.PendingTimesheetSummary;
import com.hrm.employeemanagement.application.port.outbound.project.CheckUnapprovedTimesheetsPort;
import com.hrm.employeemanagement.domain.project.ProjectId;

@Component
public class DefaultCheckUnapprovedTimesheetsAdapter implements CheckUnapprovedTimesheetsPort {

    @Override
    public List<PendingTimesheetSummary> findPendingTimesheetsByProjectId(ProjectId projectId) {
        // Mặc định trả về rỗng vì Epic 9 (Timesheet) chưa triển khai
        // Khi Epic 9 sẵn sàng, adapter này sẽ kết nối với TimesheetRepository
        return Collections.emptyList();
    }
}
