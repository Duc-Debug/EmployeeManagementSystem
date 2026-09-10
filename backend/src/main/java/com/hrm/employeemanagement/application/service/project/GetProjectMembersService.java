package com.hrm.employeemanagement.application.service.project;

import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectMembersUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectMemberPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
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

public class GetProjectMembersService implements GetProjectMembersUseCase {

    private final LoadProjectPort loadProjectPort;
    private final LoadProjectMemberPort loadProjectMemberPort;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public GetProjectMembersService(
            LoadProjectPort loadProjectPort,
            LoadProjectMemberPort loadProjectMemberPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadProjectMemberPort = Objects.requireNonNull(loadProjectMemberPort, "LoadProjectMemberPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public List<ProjectMemberResult> getProjectMembers(Long projectId) {
        if (projectId == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_READ);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (!canReadProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, projectId, "OUTSIDE_DATA_SCOPE_PROJECT_MEMBERS_READ");
            throw new PermissionDeniedException(PermissionCode.PROJECT_READ);
        }

        return loadProjectMemberPort.findMembersByProjectId(projectId);
    }

    private boolean canReadProject(User currentUser, Long currentUserId, Project project) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                loadProjectPort.existsInOrgUnitBranch(project.getIdValue(), currentUser.getScopeOrgUnitId());
            case SELF -> {
                Long employeeId = loadEmployeePort.findByUserId(new UserId(currentUserId))
                        .map(Employee::getIdValue)
                        .orElse(null);
                yield employeeId != null
                        && (project.isManagedBy(new EmployeeId(employeeId))
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
                        "PROJECT_MEMBERS_ACCESS_DENIED",
                        "project_members",
                        projectId,
                        null,
                        "permission=PROJECT_READ;dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }
}
