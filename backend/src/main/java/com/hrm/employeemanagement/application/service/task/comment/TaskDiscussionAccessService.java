package com.hrm.employeemanagement.application.service.task.comment;

import java.util.Objects;

import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/** Enforces object-level access to task discussions at the application boundary. */
public class TaskDiscussionAccessService {
    private final AuthorizationService authorizationService;
    private final LoadTaskPort loadTaskPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;

    public TaskDiscussionAccessService(AuthorizationService authorizationService, LoadTaskPort loadTaskPort,
            LoadProjectPort loadProjectPort, LoadUserPort loadUserPort, LoadEmployeePort loadEmployeePort) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort);
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort);
        this.loadUserPort = Objects.requireNonNull(loadUserPort);
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort);
    }

    public Task requireAccess(Long taskId, PermissionCode permission) {
        Long currentUserId = authorizationService.require(permission);
        Task task = loadTaskPort.findById(TaskId.of(taskId))
                .orElseThrow(() -> new TaskNotFoundException(taskId));
        Project project = loadProjectPort.findById(new ProjectId(task.getProjectId().value()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án của công việc: " + taskId));
        User user = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng hiện tại"));
        if (!canAccess(user, currentUserId, project)) {
            throw new PermissionDeniedException(permission);
        }
        return task;
    }

    public Task requireDeleteAccess(Long taskId, Long authorId, Long requestingUserId) {
        Task task = requireAccess(taskId, PermissionCode.TASK_DISCUSSION_DELETE);
        if (!Objects.equals(authorId, requestingUserId)) {
            if (!authorizationService.hasPermission(PermissionCode.TASK_DISCUSSION_MANAGE)) {
                throw new PermissionDeniedException(PermissionCode.TASK_DISCUSSION_MANAGE);
            }
        }
        return task;
    }

    public boolean canUserAccess(User user, Task task) {
        if (user == null || task == null || task.getProjectId() == null) {
            return false;
        }
        Project project = loadProjectPort.findById(new ProjectId(task.getProjectId().value())).orElse(null);
        if (project == null) {
            return false;
        }
        return canAccess(user, user.getIdValue(), project);
    }

    public boolean canUserAccess(User user, Long taskId) {
        if (user == null || taskId == null) {
            return false;
        }
        Task task = loadTaskPort.findById(TaskId.of(taskId)).orElse(null);
        if (task == null) {
            return false;
        }
        return canUserAccess(user, task);
    }

    private boolean canAccess(User user, Long userId, Project project) {
        return switch (user.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                    loadProjectPort.existsInOrgUnitBranch(project.getIdValue(), user.getScopeOrgUnitId());
            case SELF -> {
                Long employeeId = loadEmployeePort.findByUserId(new UserId(userId))
                        .map(Employee::getIdValue).orElse(null);
                yield employeeId != null && (project.isManagedBy(new EmployeeId(employeeId))
                        || loadProjectPort.existsMember(project.getIdValue(), employeeId));
            }
        };
    }
}

