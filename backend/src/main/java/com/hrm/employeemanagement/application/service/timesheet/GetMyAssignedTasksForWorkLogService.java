package com.hrm.employeemanagement.application.service.timesheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
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

        // 1. Get tasks assigned via TaskAssignment
        List<TaskAssignment> assignments = loadTaskAssignmentPort.findByEmployeeId(employee.getId());
        List<TaskId> assignedTaskIds = assignments.stream()
                .map(TaskAssignment::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Task> assignedTasks = loadTaskPort.findAllById(assignedTaskIds);

        // 2. Get tasks assigned directly via Task.assigneeId
        List<Task> directTasks = loadTaskPort.findByAssigneeId(employee.getId());

        // 3. Combine and deduplicate tasks preserving order
        Map<TaskId, Task> taskMap = new LinkedHashMap<>();
        for (Task task : directTasks) {
            if (task != null && task.getId() != null) {
                taskMap.put(task.getId(), task);
            }
        }
        for (Task task : assignedTasks) {
            if (task != null && task.getId() != null) {
                taskMap.put(task.getId(), task);
            }
        }

        // 4. Batch load projects
        List<ProjectId> projectIds = taskMap.values().stream()
                .map(Task::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Project> projectMap = loadProjectPort.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getIdValue, p -> p, (a, b) -> a));

        List<AssignedTaskOptionResult> results = new ArrayList<>();
        for (Task task : taskMap.values()) {
            if (task.getTaskType() == TaskType.CATEGORY) {
                continue;
            }
            if (task.getStatus() == TaskStatus.CANCELLED) {
                continue;
            }

            Project project = projectMap.get(task.getProjectIdValue());
            if (project == null) {
                continue;
            }
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
