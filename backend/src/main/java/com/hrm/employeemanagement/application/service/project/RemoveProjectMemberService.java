package com.hrm.employeemanagement.application.service.project;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.project.RemoveProjectMemberCommand;
import com.hrm.employeemanagement.application.port.inbound.project.RemoveProjectMemberUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectMemberPort;
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
import com.hrm.employeemanagement.domain.exception.project.MemberHasActiveTasksException;
import com.hrm.employeemanagement.domain.exception.project.ProjectMemberNotFoundException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.task.ProjectClosedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class RemoveProjectMemberService implements RemoveProjectMemberUseCase {

    private final LoadProjectPort loadProjectPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final SaveProjectMemberPort saveProjectMemberPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public RemoveProjectMemberService(
            LoadProjectPort loadProjectPort,
            LoadProjectMemberPort loadProjectMemberPort,
            SaveProjectMemberPort saveProjectMemberPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadProjectMemberPort = Objects.requireNonNull(loadProjectMemberPort, "LoadProjectMemberPort must not be null");
        this.saveProjectMemberPort = Objects.requireNonNull(saveProjectMemberPort, "SaveProjectMemberPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public void removeProjectMember(RemoveProjectMemberCommand command) {
        if (command == null || command.projectId() == null || command.employeeId() == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) và mã nhân viên (employeeId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_UPDATE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findByIdForUpdate(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        if (!canUpdateProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_REMOVE_PROJECT_MEMBER");
            throw new PermissionDeniedException(PermissionCode.PROJECT_UPDATE);
        }

        if (project.getStatus() == ProjectStatus.CLOSED) {
            throw new ProjectClosedException(project.getIdValue());
        }

        if (project.getManagerId() != null && Objects.equals(project.getManagerId().value(), command.employeeId())) {
            throw new InvalidProjectDataException("Không thể xóa Quản lý dự án (PM) khỏi danh sách thành viên qua chức năng này");
        }

        if (!loadProjectMemberPort.existsMember(command.projectId(), command.employeeId())) {
            throw new ProjectMemberNotFoundException(command.employeeId(), command.projectId());
        }

        if (loadProjectMemberPort.hasActiveTasks(command.projectId(), command.employeeId())) {
            throw new MemberHasActiveTasksException(command.employeeId(), command.projectId());
        }

        saveProjectMemberPort.removeMember(command.projectId(), command.employeeId());

        saveAuditLogPort.save(AuditLog.create(currentUserId, "REMOVE_PROJECT_MEMBER", "project_members", command.projectId()));
    }

    private boolean canUpdateProject(User currentUser, Long currentUserId, Project project) {
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
                        "project_members",
                        projectId,
                        null,
                        "permission=PROJECT_UPDATE;dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }
}
