package com.hrm.employeemanagement.application.service.milestone;

import java.util.Objects;

import com.hrm.employeemanagement.application.port.inbound.milestone.DeleteMilestoneUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.milestone.DeleteMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.milestone.LoadMilestonePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.milestone.InvalidMilestoneDataException;
import com.hrm.employeemanagement.domain.exception.milestone.MilestoneNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.milestone.Milestone;
import com.hrm.employeemanagement.domain.milestone.MilestoneId;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class DeleteMilestoneService implements DeleteMilestoneUseCase {

    private final LoadMilestonePort loadMilestonePort;
    private final DeleteMilestonePort deleteMilestonePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public DeleteMilestoneService(
            LoadMilestonePort loadMilestonePort,
            DeleteMilestonePort deleteMilestonePort,
            LoadProjectPort loadProjectPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadMilestonePort = Objects.requireNonNull(loadMilestonePort, "LoadMilestonePort must not be null");
        this.deleteMilestonePort = Objects.requireNonNull(deleteMilestonePort, "DeleteMilestonePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public void deleteMilestone(Long projectId, Long milestoneId) {
        if (projectId == null || milestoneId == null) {
            throw new InvalidMilestoneDataException("Mã dự án và mã mốc tiến độ không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_MILESTONE_MANAGE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (!canManageMilestones(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_MILESTONE_MANAGE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_MILESTONE_MANAGE);
        }

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ProjectClosedException(project.getIdValue());
        }

        Milestone milestone = loadMilestonePort.findById(new MilestoneId(milestoneId))
                .orElseThrow(() -> new MilestoneNotFoundException(milestoneId));

        if (!Objects.equals(milestone.getProjectIdValue(), project.getIdValue())) {
            throw new InvalidMilestoneDataException("Mốc tiến độ không thuộc dự án này");
        }

        deleteMilestonePort.deleteById(milestone.getId());

        // Audit log (TC-04)
        saveAuditLogPort.save(AuditLog.create(currentUserId, "DELETE_MILESTONE", "project_milestones", milestone.getIdValue()));
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
}
