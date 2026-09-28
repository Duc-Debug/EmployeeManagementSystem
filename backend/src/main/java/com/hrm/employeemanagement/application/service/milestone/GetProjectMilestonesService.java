package com.hrm.employeemanagement.application.service.milestone;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.port.inbound.milestone.GetProjectMilestonesUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
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
import com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.milestone.MilestoneStatus;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.task.TaskStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class GetProjectMilestonesService implements GetProjectMilestonesUseCase {

    private final LoadMilestonePort loadMilestonePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public GetProjectMilestonesService(
            LoadMilestonePort loadMilestonePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadMilestonePort = Objects.requireNonNull(loadMilestonePort, "LoadMilestonePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public List<MilestoneResult> getProjectMilestones(Long projectId) {
        if (projectId == null) {
            throw new InvalidMilestoneDataException("Mã dự án (projectId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_READ);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, projectId, "OUTSIDE_DATA_SCOPE_PROJECT_MILESTONES_READ");
            throw new PermissionDeniedException(PermissionCode.PROJECT_READ);
        }

        List<Milestone> milestones = loadMilestonePort.findAllByProjectId(new ProjectId(projectId));
        List<Task> tasks = loadTaskPort.findAllByProjectId(new ProjectId(projectId));

        Set<Long> completedTaskIds = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .map(Task::getIdValue)
                .collect(Collectors.toSet());

        LocalDate now = LocalDate.now();

        return milestones.stream()
                .map(milestone -> {
                    List<Long> linkedIds = milestone.getLinkedTaskIds().stream()
                            .map(TaskId::value)
                            .toList();

                    int total = linkedIds.size();
                    int completed = 0;
                    for (Long id : linkedIds) {
                        if (completedTaskIds.contains(id)) {
                            completed++;
                        }
                    }

                    boolean allDone = total > 0 && completed == total;
                    MilestoneStatus status = milestone.evaluateStatus(now, allDone);
                    long delayDays = milestone.calculateDelayDays(now, allDone);

                    return new MilestoneResult(
                            milestone.getIdValue(),
                            milestone.getProjectIdValue(),
                            milestone.getName(),
                            milestone.getDescription(),
                            milestone.getPlannedDate(),
                            milestone.getActualDate(),
                            status,
                            delayDays,
                            total,
                            completed,
                            linkedIds,
                            milestone.getCreatedByValue(),
                            milestone.getCreatedAt(),
                            milestone.getUpdatedAt(),
                            milestone.getVersion());
                })
                .toList();
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
