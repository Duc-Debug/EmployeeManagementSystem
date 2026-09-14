package com.hrm.employeemanagement.application.service.timesheet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.timesheet.AssignedTaskOptionResult;
import com.hrm.employeemanagement.application.port.inbound.timesheet.GetMyAssignedTasksForWorkLogUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskType;
import com.hrm.employeemanagement.domain.user.UserId;

public class GetMyAssignedTasksForWorkLogService implements GetMyAssignedTasksForWorkLogUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final AuthorizationService authorizationService;

    public GetMyAssignedTasksForWorkLogService(
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            AuthorizationService authorizationService) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadTaskAssignmentPort = Objects.requireNonNull(loadTaskAssignmentPort, "LoadTaskAssignmentPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public List<AssignedTaskOptionResult> getMyAssignedTasksForWorkLog() {
        Long currentUserId = authorizationService.require(PermissionCode.WORK_LOG_READ);

        Employee employee = loadEmployeePort.findByUserId(new UserId(currentUserId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy thông tin nhân sự của người dùng hiện tại"));

        List<TaskAssignment> assignments = loadTaskAssignmentPort.findByEmployeeId(employee.getId());
        List<TaskId> assignedTaskIds = assignments.stream()
                .map(TaskAssignment::getTaskId)
                .collect(Collectors.toList());

        List<Task> tasks = loadTaskPort.findAllById(assignedTaskIds);

        List<AssignedTaskOptionResult> results = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getTaskType() == TaskType.CATEGORY) {
                continue;
            }
            if (task.getStatus() == com.hrm.employeemanagement.domain.task.TaskStatus.CANCELLED) {
                continue;
            }

            var projectOpt = loadProjectPort.findById(new ProjectId(task.getProjectIdValue()));
            if (projectOpt.isEmpty()) {
                continue;
            }
            var project = projectOpt.get();
            // QTN-08: Only show tasks from ACTIVE projects (hide CLOSED projects)
            if (project.getStatus() != ProjectStatus.ACTIVE) {
                continue;
            }

            results.add(new AssignedTaskOptionResult(
                    project.getIdValue(),
                    project.getProjectCode(),
                    project.getProjectName(),
                    project.getStatus().name(),
                    task.getIdValue(),
                    task.getTaskCode(),
                    task.getName(),
                    task.getStatus() != null ? task.getStatus().name() : ""
            ));
        }

        return results;
    }
}
