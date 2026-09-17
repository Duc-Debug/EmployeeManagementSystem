package com.hrm.employeemanagement.application.service.project;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.project.ApproveProjectUseCase;
import com.hrm.employeemanagement.application.port.inbound.reservation.AutoProcessProjectReservationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
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
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ApproveProjectService implements ApproveProjectUseCase {

    private final LoadProjectPort loadProjectPort;
    private final SaveProjectPort saveProjectPort;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;
    private final AutoProcessProjectReservationsUseCase autoProcessUseCase;

    public ApproveProjectService(
            LoadProjectPort loadProjectPort,
            SaveProjectPort saveProjectPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService,
            AutoProcessProjectReservationsUseCase autoProcessUseCase) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.saveProjectPort = Objects.requireNonNull(saveProjectPort, "SaveProjectPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.autoProcessUseCase = autoProcessUseCase;
    }

    @Override
    public ProjectResult approveProject(Long projectId) {
        if (projectId == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_UPDATE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_APPROVE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_UPDATE);
        }

        project.approve(new UserId(currentUserId));
        Project savedProject = saveProjectPort.save(project);

        if (autoProcessUseCase != null) {
            autoProcessUseCase.autoConvertForProject(projectId);
        }

        saveAuditLogPort.save(AuditLog.create(currentUserId, "APPROVE_PROJECT", "projects", savedProject.getIdValue()));

        return mapToProjectResult(savedProject);
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
                        "permission=PROJECT_UPDATE;dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }

    private ProjectResult mapToProjectResult(Project project) {
        return new ProjectResult(
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                project.getOrgUnitId(),
                project.getManagerIdValue(),
                project.getStartDate(),
                project.getEndDate(),
                project.getEstimatedHours(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedByValue(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
