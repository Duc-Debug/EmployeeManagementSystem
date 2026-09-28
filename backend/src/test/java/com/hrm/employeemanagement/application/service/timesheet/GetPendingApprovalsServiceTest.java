package com.hrm.employeemanagement.application.service.timesheet;

import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.timesheet.TimesheetStatus;
import com.hrm.employeemanagement.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class GetPendingApprovalsServiceTest {
    @Test
    void providesBudgetAndCombinedPendingHoursBeforeAnyApproval() {
        var entries = mock(LoadTimesheetEntryPort.class);
        var projects = mock(LoadProjectPort.class);
        var tasks = mock(LoadTaskPort.class);
        var employees = mock(LoadEmployeePort.class);
        var auth = mock(AuthorizationService.class);
        var pm = mock(Employee.class);
        when(auth.require(PermissionCode.WORK_LOG_APPROVE)).thenReturn(7L);
        when(employees.findByUserId(new UserId(7L))).thenReturn(Optional.of(pm));
        when(pm.getId()).thenReturn(new EmployeeId(90L));
        var first = entry(1L, "5");
        var second = entry(2L, "7");
        when(entries.findPendingApprovals(new EmployeeId(90L))).thenReturn(List.of(first, second));
        var task = mock(Task.class);
        when(task.getIdValue()).thenReturn(50L);
        when(task.getBudgetHours()).thenReturn(new BigDecimal("100"));
        when(task.getActualHours()).thenReturn(new BigDecimal("70"));
        when(tasks.findAllById(anyList())).thenReturn(List.of(task));

        var result = new GetPendingApprovalsService(entries, projects, tasks, employees, auth).getPendingApprovals();

        assertThat(result).hasSize(2).allSatisfy(row -> {
            assertThat(row.taskBudgetHours()).isEqualByComparingTo("100");
            assertThat(row.taskActualHours()).isEqualByComparingTo("70");
            assertThat(row.taskPendingHours()).isEqualByComparingTo("12");
        });
        verify(entries).findPendingApprovals(new EmployeeId(90L));
        verify(first, never()).approve();
        verify(second, never()).approve();
    }

    private TimesheetEntry entry(long id, String hours) {
        var entry = mock(TimesheetEntry.class);
        when(entry.getIdValue()).thenReturn(id);
        when(entry.getEmployeeId()).thenReturn(new EmployeeId(21L));
        when(entry.getEmployeeIdValue()).thenReturn(21L);
        when(entry.getProjectIdValue()).thenReturn(10L);
        when(entry.getTaskIdValue()).thenReturn(50L);
        when(entry.getStatus()).thenReturn(TimesheetStatus.SUBMITTED);
        when(entry.getHours()).thenReturn(new BigDecimal(hours));
        return entry;
    }
}
