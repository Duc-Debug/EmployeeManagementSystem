package com.hrm.employeemanagement.application.service.milestone;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.milestone.CreateMilestoneCommand;
import com.hrm.employeemanagement.application.dto.milestone.MilestoneResult;
import com.hrm.employeemanagement.application.port.inbound.milestone.CreateMilestoneUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.milestone.SaveMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.milestone.DuplicateMilestoneNameException;
import com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException;
import com.hrm.employeemanagement.domain.exception.milestone.ProjectHasNoWbsException;
import com.hrm.employeemanagement.domain.exception.milestone.TaskNotInProjectException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.task.TaskNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.milestone.MilestoneStatus;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class CreateMilestoneService implements CreateMilestoneUseCase {

    private final LoadMilestonePort loadMilestonePort;
    private final SaveMilestonePort saveMilestonePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadTaskPort loadTaskPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public CreateMilestoneService(
            LoadMilestonePort loadMilestonePort,
            SaveMilestonePort saveMilestonePort,
            LoadProjectPort loadProjectPort,
            LoadTaskPort loadTaskPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadMilestonePort = Objects.requireNonNull(loadMilestonePort, "LoadMilestonePort must not be null");
        this.saveMilestonePort = Objects.requireNonNull(saveMilestonePort, "SaveMilestonePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadTaskPort = Objects.requireNonNull(loadTaskPort, "LoadTaskPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public MilestoneResult createMilestone(CreateMilestoneCommand command) {
        if (command == null || command.projectId() == null) {
            throw new InvalidMilestoneDataException("Mã dự án (projectId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        if (!canManageMilestones(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_MILESTONE_MANAGE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_MILESTONE_MANAGE);
        }

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ProjectClosedException(project.getIdValue());
        }

        // Precondition (TC-01): Dự án đã có cây công việc (WBS)
        List<Task> existingTasks = loadTaskPort.findAllByProjectId(project.getId());
        if (existingTasks == null || existingTasks.isEmpty()) {
            throw new ProjectHasNoWbsException(project.getIdValue());
        }

        // Validate unique milestone name within project
        if (loadMilestonePort.existsByProjectIdAndName(project.getId(), command.name().trim())) {
            throw new DuplicateMilestoneNameException(command.name().trim());
        }

        // Validate linked tasks belong to this project
        Set<TaskId> taskIds = new HashSet<>();
        if (command.linkedTaskIds() != null && !command.linkedTaskIds().isEmpty()) {
            Set<Long> projectTaskIds = existingTasks.stream()
                    .map(Task::getIdValue)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (Long tid : command.linkedTaskIds()) {
                if (!projectTaskIds.contains(tid)) {
                    throw new TaskNotInProjectException(tid, project.getIdValue());
                }
                taskIds.add(new TaskId(tid));
            }
        }

        Milestone milestone = Milestone.createNew(
                project.getId(),
                command.name(),
                command.description(),
                command.plannedDate(),
                taskIds,
                new UserId(currentUserId));

        Milestone savedMilestone = saveMilestonePort.save(milestone);

        // Audit Log (TC-04)
        saveAuditLogPort.save(AuditLog.create(currentUserId, "CREATE_MILESTONE", "project_milestones", savedMilestone.getIdValue()));

        return mapToResult(savedMilestone, existingTasks);
    }

    private boolean canManageMilestones(User currentUser, Long currentUserId, Project project) {
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
                        "permission=PROJECT_MILESTONE_MANAGE;dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }

    private MilestoneResult mapToResult(Milestone milestone, List<Task> tasks) {
        List<Long> linkedIds = milestone.getLinkedTaskIds().stream()
                .map(TaskId::value)
                .toList();

        int total = linkedIds.size();
        int completed = 0;
        if (tasks != null && !linkedIds.isEmpty()) {
            Set<Long> completedTaskIds = tasks.stream()
                    .filter(t -> t.getStatus() == com.hrm.employeemanagement.domain.task.TaskStatus.DONE)
                    .map(Task::getIdValue)
                    .collect(Collectors.toSet());
            for (Long id : linkedIds) {
                if (completedTaskIds.contains(id)) {
                    completed++;
                }
            }
        }

        boolean allDone = total > 0 && completed == total;
        MilestoneStatus status = milestone.evaluateStatus(LocalDate.now(), allDone);
        long delayDays = milestone.calculateDelayDays(LocalDate.now(), allDone);

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
    }
}
