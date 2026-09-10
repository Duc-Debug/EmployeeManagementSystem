package com.hrm.employeemanagement.application.port.outbound.project;

import java.util.List;

import com.hrm.employeemanagement.application.dto.project.PendingTimesheetSummary;
import com.hrm.employeemanagement.domain.project.ProjectId;

public interface CheckUnapprovedTimesheetsPort {
    List<PendingTimesheetSummary> findPendingTimesheetsByProjectId(ProjectId projectId);
}
