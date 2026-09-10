package com.hrm.employeemanagement.application.port.outbound.project;

import java.util.List;

import com.hrm.employeemanagement.application.dto.project.PendingExpenseSummary;
import com.hrm.employeemanagement.domain.project.ProjectId;

public interface CheckUnapprovedExpensesPort {
    List<PendingExpenseSummary> findPendingExpensesByProjectId(ProjectId projectId);
}
