package com.hrm.employeemanagement.infrastructure.adapter.outbound.expense;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.project.PendingExpenseSummary;
import com.hrm.employeemanagement.application.port.outbound.project.CheckUnapprovedExpensesPort;
import com.hrm.employeemanagement.domain.project.ProjectId;

@Component
public class DefaultCheckUnapprovedExpensesAdapter implements CheckUnapprovedExpensesPort {

    @Override
    public List<PendingExpenseSummary> findPendingExpensesByProjectId(ProjectId projectId) {
        // Mặc định trả về rỗng vì Module Chi phí chưa triển khai
        return Collections.emptyList();
    }
}
