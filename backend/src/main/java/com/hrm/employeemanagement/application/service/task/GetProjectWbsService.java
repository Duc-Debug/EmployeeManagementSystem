package com.hrm.employeemanagement.application.service.task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.task.TaskNodeResult;
import com.hrm.employeemanagement.application.port.inbound.task.GetProjectWbsUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class GetProjectWbsService implements GetProjectWbsUseCase {

    private final LoadTaskPort loadTaskPort;
    private final LoadProjectPort loadProjectPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public GetProjectWbsService(
            LoadTaskPort loadTaskPort,
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort,
                "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService,
                "AuthorizationService must not be null");
    }

    @Override
    public List<TaskNodeResult> getProjectWbs(Long projectId) {
        if (projectId == null) {
            throw new InvalidTaskDataException("Mã dự án (projectId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_READ);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, projectId, "OUTSIDE_DATA_SCOPE_WBS_READ");
            throw new PermissionDeniedException(PermissionCode.PROJECT_READ);
        }

        List<Task> tasks = loadTaskPort.findAllByProjectId(new ProjectId(projectId));
        return buildTree(tasks);
    }

    private List<TaskNodeResult> buildTree(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }

        List<Task> sortedTasks = tasks.stream()
                .sorted(Comparator.comparingInt(Task::getSortOrder)
                        .thenComparing(t -> t.getIdValue() != null ? t.getIdValue() : 0L))
                .toList();

        Map<Long, TaskNodeResult> nodeMap = new LinkedHashMap<>();
        for (Task task : sortedTasks) {
            TaskNodeResult node = new TaskNodeResult(
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
                    task.getBudgetHours(),
                    task.calculateBurnedPercentage(),
                    task.getBudgetBurnStatus(),
                    task.isOverBudget(),
                    task.getStatus(),
                    task.getSortOrder(),
                    task.getCreatedByValue(),
                    task.getCreatedAt(),
                    task.getUpdatedAt(),
                    task.getVersion(),
                    new ArrayList<>());
            nodeMap.put(task.getIdValue(), node);
        }

        List<TaskNodeResult> rootNodes = new ArrayList<>();
        for (Task task : sortedTasks) {
            TaskNodeResult currentNode = nodeMap.get(task.getIdValue());
            if (task.getParentIdValue() == null) {
                rootNodes.add(currentNode);
            } else {
                TaskNodeResult parentNode = nodeMap.get(task.getParentIdValue());
                if (parentNode != null) {
                    parentNode.children().add(currentNode);
                } else {
                    rootNodes.add(currentNode);
                }
            }
        }

        return rootNodes;
    }

    private boolean canAccessProject(User currentUser, Long currentUserId, Project project) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                loadProjectPort.existsInOrgUnitBranch(project.getIdValue(), currentUser.getScopeOrgUnitId());
            case SELF -> {
                Long employeeId = loadEmployeePort.findByUserId(new UserId(currentUserId))
                        .map(Employee::getIdValue)
                        .orElse(null);
                yield employeeId != null && (project.isManagedBy(new EmployeeId(employeeId))
                        || loadProjectPort.existsMember(project.getIdValue(), employeeId));
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
                        "permission=PROJECT_READ;dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }
}
