package com.hrm.employeemanagement.application.service.task;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.task.dependency.CreateTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.DeleteTaskDependencyCommand;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyGraphResult;
import com.hrm.employeemanagement.application.dto.task.dependency.TaskDependencyResult;
import com.hrm.employeemanagement.application.port.inbound.task.CreateTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.DeleteTaskDependencyUseCase;
import com.hrm.employeemanagement.application.port.inbound.task.GetTaskDependenciesUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.DeleteTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.task.SaveTaskDependencyPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependency;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependencyPolicy;
import com.hrm.employeemanagement.domain.task.dependency.TaskDependencyType;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class TaskDependencyService implements
        CreateTaskDependencyUseCase,
        DeleteTaskDependencyUseCase,
        GetTaskDependenciesUseCase {

    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadTaskDependencyPort loadDependencyPort;
    private final SaveTaskDependencyPort saveDependencyPort;
    private final DeleteTaskDependencyPort deleteDependencyPort;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public TaskDependencyService(
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadTaskDependencyPort loadDependencyPort,
            SaveTaskDependencyPort saveDependencyPort,
            DeleteTaskDependencyPort deleteDependencyPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadDependencyPort = Objects.requireNonNull(loadDependencyPort, "LoadTaskDependencyPort must not be null");
        this.saveDependencyPort = Objects.requireNonNull(saveDependencyPort, "SaveTaskDependencyPort must not be null");
        this.deleteDependencyPort = Objects.requireNonNull(deleteDependencyPort, "DeleteTaskDependencyPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public TaskDependencyResult createDependency(CreateTaskDependencyCommand command) {
        if (command == null || command.projectId() == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }
        if (command.predecessorId() == null || command.successorId() == null) {
            throw new InvalidTaskDataException("Công việc tiền đề và công việc phụ thuộc không được để trống");
        }

        // TC-03: Phân quyền & Kiểm tra Data Scope
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_TASK_DEPENDENCY_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_TASK_DEPENDENCY_CREATE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_TASK_DEPENDENCY_MANAGE);
        }

        TaskId predecessorId = new TaskId(command.predecessorId());
        TaskId successorId = new TaskId(command.successorId());

        if (predecessorId.equals(successorId)) {
            throw new InvalidTaskDataException("Công việc không thể tự phụ thuộc vào chính mình");
        }

        Task predecessorTask = loadTaskPort.findById(predecessorId)
                .orElseThrow(() -> new TaskNotFoundException(command.predecessorId()));
        Task successorTask = loadTaskPort.findById(successorId)
                .orElseThrow(() -> new TaskNotFoundException(command.successorId()));

        if (!predecessorTask.getProjectIdValue().equals(project.getIdValue()) ||
            !successorTask.getProjectIdValue().equals(project.getIdValue())) {
            throw new InvalidTaskDataException("Hai công việc phụ thuộc phải thuộc cùng một dự án");
        }

        if (loadDependencyPort.existsByPredecessorIdAndSuccessorId(predecessorId, successorId)) {
            throw new InvalidTaskDataException("Quan hệ phụ thuộc giữa 2 công việc này đã tồn tại");
        }

        // TC-02: Kiểm tra khống chế vòng lặp bằng DFS
        List<TaskDependency> existingDependencies = loadDependencyPort.findByProjectId(project.getId());
        List<Task> allProjectTasks = loadTaskPort.findAllByProjectId(project.getId());
        Map<Long, String> taskNamesMap = allProjectTasks.stream()
                .collect(Collectors.toMap(
                        Task::getIdValue,
                        t -> t.getName() + " (" + (t.getTaskCode() != null ? t.getTaskCode() : "TK-" + t.getIdValue()) + ")"
                ));

        TaskDependencyPolicy.validateNoCycle(existingDependencies, predecessorId, successorId, taskNamesMap);

        TaskDependencyType type = command.dependencyType() != null
                ? TaskDependencyType.valueOf(command.dependencyType())
                : TaskDependencyType.FINISH_TO_START;

        TaskDependency newDependency = TaskDependency.createNew(
                project.getId(),
                predecessorId,
                successorId,
                type,
                command.lagDays(),
                new UserId(currentUserId)
        );

        TaskDependency saved = saveDependencyPort.save(newDependency);

        // TC-04: Lưu nhật ký kiểm toán (AuditLog)
        saveAuditLogPort.save(AuditLog.create(
                currentUserId,
                "CREATE_TASK_DEPENDENCY",
                "task_dependencies",
                saved.getId()
        ));

        return TaskDependencyResult.fromDomain(
                saved,
                predecessorTask.getTaskCode(),
                predecessorTask.getName(),
                successorTask.getTaskCode(),
                successorTask.getName()
        );
    }

    @Override
    public void deleteDependency(DeleteTaskDependencyCommand command) {
        if (command == null || command.projectId() == null || command.dependencyId() == null) {
            throw new InvalidTaskDataException("Thông tin xóa phụ thuộc không hợp lệ");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_TASK_DEPENDENCY_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_TASK_DEPENDENCY_DELETE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_TASK_DEPENDENCY_MANAGE);
        }

        TaskDependency dependency = loadDependencyPort.findById(command.dependencyId())
                .orElseThrow(() -> new InvalidTaskDataException("Không tìm thấy quan hệ phụ thuộc với ID: " + command.dependencyId()));

        if (!dependency.getProjectIdValue().equals(project.getIdValue())) {
            throw new InvalidTaskDataException("Quan hệ phụ thuộc không thuộc dự án chỉ định");
        }

        deleteDependencyPort.deleteById(dependency.getId());

        // TC-04: Ghi log kiểm toán thao tác xóa
        saveAuditLogPort.save(AuditLog.create(
                currentUserId,
                "DELETE_TASK_DEPENDENCY",
                "task_dependencies",
                command.dependencyId()
        ));
    }

    @Override
    public TaskDependencyGraphResult getTaskDependencies(Long projectId) {
        if (projectId == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_TASK_DEPENDENCY_READ);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_TASK_DEPENDENCY_READ");
            throw new PermissionDeniedException(PermissionCode.PROJECT_TASK_DEPENDENCY_READ);
        }

        List<TaskDependency> dependencies = loadDependencyPort.findByProjectId(project.getId());
        List<Task> tasks = loadTaskPort.findAllByProjectId(project.getId());
        Map<Long, Task> taskMap = tasks.stream().collect(Collectors.toMap(Task::getIdValue, t -> t));

        List<TaskDependencyResult> dependencyResults = dependencies.stream()
                .map(dep -> {
                    Task pred = taskMap.get(dep.getPredecessorIdValue());
                    Task succ = taskMap.get(dep.getSuccessorIdValue());
                    return TaskDependencyResult.fromDomain(
                            dep,
                            pred != null ? pred.getTaskCode() : null,
                            pred != null ? pred.getName() : "Task " + dep.getPredecessorIdValue(),
                            succ != null ? succ.getTaskCode() : null,
                            succ != null ? succ.getName() : "Task " + dep.getSuccessorIdValue()
                    );
                })
                .toList();

        return new TaskDependencyGraphResult(
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                dependencyResults
        );
    }

    // Helper methods
    private boolean canAccessProject(User currentUser, Long currentUserId, Project project) {
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
                        "task_dependencies",
                        projectId,
                        null,
                        "permission=PROJECT_TASK_DEPENDENCY;dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }
}
