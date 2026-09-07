package com.hrm.employeemanagement.application.service.task;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.hrm.employeemanagement.application.dto.task.TaskResult;
import com.hrm.employeemanagement.application.dto.task.UpdateTaskCommand;
import com.hrm.employeemanagement.application.port.inbound.task.UpdateTaskUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.AssigneeNotInProjectException;
import com.hrm.employeemanagement.domain.exception.task.CyclicTaskHierarchyException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class UpdateTaskService implements UpdateTaskUseCase {

    private final LoadTaskPort loadTaskPort;
    private final SaveTaskPort saveTaskPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public UpdateTaskService(
            LoadTaskPort loadTaskPort,
            SaveTaskPort saveTaskPort,
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.saveTaskPort = Objects.requireNonNull(saveTaskPort, "SaveTaskPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort,
                "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService,
                "AuthorizationService must not be null");
    }

    @Override
    public TaskResult updateTask(UpdateTaskCommand command) {
        if (command == null || command.projectId() == null || command.taskId() == null) {
            throw new InvalidTaskDataException("Mã dự án (projectId) và mã công việc (taskId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        if (!canManageWbs(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_WBS_MANAGE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_WBS_MANAGE);
        }

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ProjectClosedException(project.getIdValue());
        }

        Task task = loadTaskPort.findById(new TaskId(command.taskId()))
                .orElseThrow(() -> new TaskNotFoundException(command.taskId()));

        if (!Objects.equals(task.getProjectIdValue(), project.getIdValue())) {
            throw new InvalidTaskDataException("Công việc không thuộc dự án này");
        }

        // Cập nhật thông tin chi tiết
        task.updateDetails(command.name(), command.description(), command.estimatedHours(), command.sortOrder());

        // Cập nhật công việc cha và kiểm tra chống chu trình (Cycle Detection)
        TaskId newParentId = command.parentId() != null ? new TaskId(command.parentId()) : null;
        if (!Objects.equals(task.getParentId(), newParentId)) {
            if (newParentId != null) {
                Task parentTask = loadTaskPort.findById(newParentId)
                        .orElseThrow(() -> new TaskNotFoundException(command.parentId()));
                if (!Objects.equals(parentTask.getProjectIdValue(), project.getIdValue())) {
                    throw new InvalidTaskDataException("Công việc cha không thuộc cùng dự án này");
                }
                validateNoCyclicDependency(task.getId(), newParentId);
            }
            task.changeParent(newParentId);
        }

        // Cập nhật người được gán nếu có thay đổi
        EmployeeId newAssigneeId = command.assigneeId() != null ? new EmployeeId(command.assigneeId()) : null;
        if (!Objects.equals(task.getAssigneeId(), newAssigneeId)) {
            if (newAssigneeId != null) {
                Employee assignee = loadEmployeePort.findById(newAssigneeId)
                        .orElseThrow(() -> new InvalidTaskDataException(
                                "Không tìm thấy nhân viên được gán với ID: " + command.assigneeId()));
                if (assignee.getStatus() != EmployeeStatus.ACTIVE) {
                    throw new InvalidTaskDataException("Nhân viên được phân công không ở trạng thái hoạt động");
                }
                boolean isMember = (project.getManagerId() != null
                        && Objects.equals(project.getManagerId().value(), assignee.getIdValue()))
                        || loadProjectPort.existsMember(project.getIdValue(), assignee.getIdValue());
                if (!isMember) {
                    throw new AssigneeNotInProjectException(assignee.getIdValue(), project.getIdValue());
                }
            }
            task.assignTo(newAssigneeId);
        }

        Task savedTask = saveTaskPort.save(task);

        saveAuditLogPort.save(AuditLog.create(currentUserId, "UPDATE_TASK", "tasks", savedTask.getIdValue()));

        return mapToResult(savedTask);
    }

    private void validateNoCyclicDependency(TaskId taskId, TaskId newParentId) {
        if (newParentId == null) {
            return;
        }
        if (Objects.equals(taskId, newParentId)) {
            throw CyclicTaskHierarchyException.forCycle(taskId.value(), newParentId.value());
        }
        TaskId currentParentId = newParentId;
        Set<TaskId> visited = new HashSet<>();
        while (currentParentId != null) {
            if (!visited.add(currentParentId)) {
                throw CyclicTaskHierarchyException.forCycle(taskId.value(), newParentId.value());
            }
            if (Objects.equals(currentParentId, taskId)) {
                throw CyclicTaskHierarchyException.forCycle(taskId.value(), newParentId.value());
            }
            currentParentId = loadTaskPort.findById(currentParentId)
                    .map(Task::getParentId)
                    .orElse(null);
        }
    }

    private boolean canManageWbs(User currentUser, Long currentUserId, Project project) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                loadProjectPort.existsInOrgUnitBranch(project.getIdValue(), currentUser.getScopeOrgUnitId());
            case SELF -> {
                Long employeeId = loadEmployeePort.findByUserId(new UserId(currentUserId))
                        .map(Employee::getIdValue)
                        .orElse(null);
                yield employeeId != null && project.isManagedBy(new EmployeeId(employeeId));
            }
        };
    }

    private User loadCurrentUserOrThrow(Long currentUserId) {
        return loadUserPort.findById(new UserId(currentUserId)).orElseThrow(
                () -> new UserNotFoundException("Không tìm thấy người dùng hiện tại với ID: " + currentUserId));
    }

    private void saveDeniedAudit(Long currentUserId, User currentUser, Long projectId, String reason) {
        saveDeniedAuditLogPort.save(
                AuditLog.createChange(currentUserId,
                        "PROJECT_ACCESS_DENIED",
                        "projects",
                        projectId,
                        null,
                        "permission=PROJECT_WBS_MANAGE;dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }

    private TaskResult mapToResult(Task task) {
        return new TaskResult(
                task.getIdValue(),
                task.getProjectIdValue(),
                task.getParentIdValue(),
                task.getTaskCode(),
                task.getName(),
                task.getDescription(),
                task.getTaskType(),
                task.getAssigneeIdValue(),
                task.getEstimatedHours(),
                task.getActualHours(),
                task.getStatus(),
                task.getSortOrder(),
                task.getCreatedByValue(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getVersion());
    }
}
