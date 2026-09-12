package com.hrm.employeemanagement.application.service.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.task.MyTaskResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetMyTasksUseCase;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskAssignmentPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskAssignment;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.User;

public class GetMyTasksService implements GetMyTasksUseCase {

    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadTaskAssignmentPort loadTaskAssignmentPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadProjectPort loadProjectPort;

    public GetMyTasksService(
            GetAuthenticatedUserPort authenticatedUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadTaskAssignmentPort loadTaskAssignmentPort,
            LoadTaskPort loadTaskPort,
            LoadProjectPort loadProjectPort) {
        this.authenticatedUserPort = Objects.requireNonNull(authenticatedUserPort, "GetAuthenticatedUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadTaskAssignmentPort = Objects.requireNonNull(loadTaskAssignmentPort, "LoadTaskAssignmentPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
    }

    @Override
    public List<MyTaskResult> getMyTasks() {
        User currentUser = authenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Không tìm thấy người dùng đã xác thực");
        }

        Employee employee = loadEmployeePort.findByUserId(currentUser.getId()).orElse(null);
        if (employee == null) {
            return Collections.emptyList();
        }

        List<TaskAssignment> assignments = loadTaskAssignmentPort.findByEmployeeId(employee.getId());
        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }

        List<TaskId> taskIds = assignments.stream()
                .map(TaskAssignment::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<TaskId, Task> taskMap = loadTaskPort.findAllById(taskIds).stream()
                .collect(Collectors.toMap(Task::getId, t -> t, (a, b) -> a));

        List<ProjectId> projectIds = taskMap.values().stream()
                .map(Task::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<ProjectId, Project> projectMap = loadProjectPort.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, p -> p, (a, b) -> a));

        List<MyTaskResult> results = new ArrayList<>();
        for (TaskAssignment assignment : assignments) {
            Task task = taskMap.get(assignment.getTaskId());
            if (task == null) {
                continue;
            }

            Project project = projectMap.get(task.getProjectId());

            results.add(new MyTaskResult(
                    task.getIdValue(),
                    project != null ? project.getIdValue() : null,
                    project != null ? project.getProjectCode() : null,
                    project != null ? project.getProjectName() : null,
                    task.getTaskCode(),
                    task.getName(),
                    task.getStatus(),
                    task.getEstimatedHours(),
                    task.getActualHours(),
                    task.getPlannedStartDate(),
                    task.getPlannedEndDate(),
                    assignment.isPrimary()
            ));
        }

        return results;
    }
}

