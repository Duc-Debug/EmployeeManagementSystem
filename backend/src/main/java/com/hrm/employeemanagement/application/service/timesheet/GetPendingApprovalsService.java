package com.hrm.employeemanagement.application.service.timesheet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.timesheet.WorkLogResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetPendingApprovalsUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.timesheet.LoadTimesheetEntryPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.timesheet.TimesheetEntry;
import com.hrm.employeemanagement.domain.user.UserId;

public class GetPendingApprovalsService implements GetPendingApprovalsUseCase {

    private final LoadTimesheetEntryPort loadTimesheetEntryPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadEmployeePort loadEmployeePort;
    private final AuthorizationService authorizationService;

    public GetPendingApprovalsService(
            LoadTimesheetEntryPort loadTimesheetEntryPort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadEmployeePort loadEmployeePort,
            AuthorizationService authorizationService) {
        this.loadTimesheetEntryPort = loadTimesheetEntryPort;
        this.loadProjectPort = loadProjectPort;
        this.loadTaskPort = loadTaskPort;
        this.loadEmployeePort = loadEmployeePort;
        this.authorizationService = authorizationService;
    }

    @Override
    public List<WorkLogResult> getPendingApprovals() {
        Long currentUserId = authorizationService.require(PermissionCode.WORK_LOG_APPROVE);
        Employee currentEmployee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân viên (PM)"));

        List<TimesheetEntry> entries = loadTimesheetEntryPort.findPendingApprovals(currentEmployee.getId());

        List<EmployeeId> employeeIds = entries.stream().map(TimesheetEntry::getEmployeeId).distinct().toList();
        Map<Long, Employee> employeeMap = employeeIds.isEmpty() ? Map.of() : loadEmployeePort.findAllByIdIn(employeeIds).stream()
                .collect(Collectors.toMap(emp -> emp.getIdValue(), emp -> emp, (a, b) -> a));

        List<ProjectId> projectIds = entries.stream().map(e -> new ProjectId(e.getProjectIdValue())).distinct().toList();
        Map<Long, Project> projectMap = projectIds.isEmpty() ? Map.of() : loadProjectPort.findAllById(projectIds).stream()
                .collect(Collectors.toMap(p -> p.getIdValue(), p -> p, (a, b) -> a));

        List<TaskId> taskIds = entries.stream().map(e -> new TaskId(e.getTaskIdValue())).distinct().toList();
        Map<Long, Task> taskMap = taskIds.isEmpty() ? Map.of() : loadTaskPort.findAllById(taskIds).stream()
                .collect(Collectors.toMap(t -> t.getIdValue(), t -> t, (a, b) -> a));

        return entries.stream().map(e -> {
            Employee emp = employeeMap.get(e.getEmployeeIdValue());
            String empName = emp != null ? emp.getFullName() : "Unknown";

            Project proj = projectMap.get(e.getProjectIdValue());
            String projCode = proj != null ? proj.getProjectCode() : "-";
            String projName = proj != null ? proj.getProjectName() : "-";

            Task task = taskMap.get(e.getTaskIdValue());
            String taskCode = task != null ? task.getTaskCode() : "-";
            String taskName = task != null ? task.getName() : "-";

            return new WorkLogResult(
                    e.getIdValue(),
                    e.getTimesheetIdValue(),
                    e.getEmployeeIdValue(),
                    empName,
                    e.getProjectIdValue(),
                    projCode,
                    projName,
                    e.getTaskIdValue(),
                    taskCode,
                    taskName,
                    e.getWorkDate(),
                    e.getHours(),
                    e.isBillable(),
                    e.getDescription(),
                    e.getStatus().name(),
                    e.getRejectionReason(),
                    e.getCreatedAt(),
                    e.getUpdatedAt(),
                    e.getVersion()
            );
        }).collect(Collectors.toList());
    }
}
