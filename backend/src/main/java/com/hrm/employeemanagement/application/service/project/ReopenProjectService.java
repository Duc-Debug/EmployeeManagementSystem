package com.hrm.employeemanagement.application.service.project;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.project.ReopenProjectCommand;
import com.hrm.employeemanagement.application.port.inbound.project.ReopenProjectUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ReopenProjectService implements ReopenProjectUseCase {

    private final LoadProjectPort loadProjectPort;
    private final SaveProjectPort saveProjectPort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public ReopenProjectService(
            LoadProjectPort loadProjectPort,
            SaveProjectPort saveProjectPort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.saveProjectPort = Objects.requireNonNull(saveProjectPort, "SaveProjectPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public ProjectResult reopenProject(ReopenProjectCommand command) {
        if (command == null || command.projectId() == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_REOPEN);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        // Quyền mở lại dự án CHỈ dành riêng cho Ban giám đốc (VT-01)
        boolean isExecutive = currentUser.getRole() != null && currentUser.getRole().getCode() == RoleCode.VT_01;
        if (!isExecutive) {
            saveDeniedAudit(currentUserId, currentUser, command.projectId(), "ONLY_EXECUTIVE_CAN_REOPEN");
            throw new PermissionDeniedException(PermissionCode.PROJECT_REOPEN);
        }

        if (command.reopenReason() == null || command.reopenReason().trim().length() < 10) {
            throw new InvalidProjectDataException("Lý do mở lại dự án bắt buộc phải có ít nhất 10 ký tự");
        }

        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        project.reopen(new UserId(currentUserId), command.reopenReason());
        Project savedProject = saveProjectPort.save(project);

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "REOPEN_PROJECT",
                "projects",
                savedProject.getIdValue(),
                null,
                "status=ACTIVE;reason=" + command.reopenReason().trim()));

        return mapToProjectResult(savedProject);
    }

    private User loadCurrentUserOrThrow(Long currentUserId) {
        return loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại với ID: " + currentUserId));
    }

    private void saveDeniedAudit(Long currentUserId, User currentUser, Long projectId, String reason) {
        saveDeniedAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PROJECT_ACCESS_DENIED",
                "projects",
                projectId,
                null,
                "permission=PROJECT_REOPEN;role=" + (currentUser.getRole() != null ? currentUser.getRole().getCode() : "null")
                        + ";dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
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
                project.getUpdatedAt(),
                project.getClosureReason(),
                project.getClosedAt(),
                project.getClosedByValue(),
                project.getReopenReason(),
                project.getReopenedAt(),
                project.getReopenedByValue());
    }
}
