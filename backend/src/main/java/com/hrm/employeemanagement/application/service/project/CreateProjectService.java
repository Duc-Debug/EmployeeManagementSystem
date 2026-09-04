package com.hrm.employeemanagement.application.service.project;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import com.hrm.employeemanagement.application.dto.project.CreateProjectCommand;
import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
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
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.DuplicateProjectCodeException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class CreateProjectService implements CreateProjectUseCase {
    private final SaveProjectPort saveProjectPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadUserPort loadUserPort;

    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public CreateProjectService(
            SaveProjectPort saveProjectPort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadEmployeePort loadEmployeePort,
            LoadUserPort loadUserPort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.saveProjectPort = Objects.requireNonNull(saveProjectPort, "SaveProjectPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort,
                "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService,
                "AuthorizationService must not be null");
    }

    @Override
    public ProjectResult createProject(CreateProjectCommand command) {
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_CREATE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        requireOrgUnitInDataScope(currentUser, command.orgUnitId(), PermissionCode.PROJECT_CREATE);

        OrgUnit orgUnit = loadActiveOrgUnitOrThrow(command.orgUnitId());

        if (command.managerId() != null) {
            Employee manager = loadEmployeePort.findById(new EmployeeId(command.managerId()))
                    .orElseThrow(() -> new InvalidProjectDataException(
                            "Không tìm thấy nhân viên quản lý dự án với ID: " + command.managerId()));
            if (manager.getStatus() != EmployeeStatus.ACTIVE) {
                throw new InvalidProjectDataException("Nhân viên quản lý dự án không ở trạng thái hoạt động");
            }

            boolean isManagerInOrgUnit = Objects.equals(manager.getOrgUnitId(), command.orgUnitId())
                    || (manager.getOrgUnitId() != null && loadOrgUnitPort.existsInOrgUnitBranch(manager.getOrgUnitId(), command.orgUnitId()));

            if (!isManagerInOrgUnit) {
                throw new InvalidProjectDataException("Người quản lý dự án (PM) phải thuộc đơn vị tổ chức quản lý dự án");
            }
        }

        String generatedCode = generateProjectCode(orgUnit);
        Project project = Project.createNew(
                generatedCode,
                command.projectName(),
                command.orgUnitId(),
                command.managerId() != null ? new EmployeeId(command.managerId()) : null,
                command.startDate(),
                command.endDate(),
                command.estimatedHours(),
                command.description(),
                new UserId(currentUserId));

        Project savedProject = saveProjectPort.save(project);

        saveAuditLogPort.save(AuditLog.create(currentUserId, "CREATE_PROJECT", "projects", savedProject.getIdValue()));
        return mapToProjectResult(savedProject);
    }

    // ==================== HELPER METHODS ====================
    private void requireOrgUnitInDataScope(User currentUser, Long orgUnitId, PermissionCode permission) {
        if (!isOrgUnitInDataScope(currentUser, orgUnitId)) {
            saveDeniedAudit(
                    currentUser.getIdValue(),
                    currentUser,
                    null,
                    "OUTSIDE_DATA_SCOPE_ORG_UNIT_" + orgUnitId);
            throw new PermissionDeniedException(permission);
        }
    }

    private boolean isOrgUnitInDataScope(User currentUser, Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> false;
            case ORGANIZATION_BRANCH -> loadOrgUnitPort.existsInOrgUnitBranch(
                    orgUnitId,
                    currentUser.getScopeOrgUnitId());
        };
    }

    private OrgUnit loadActiveOrgUnitOrThrow(Long orgUnitId) {
        OrgUnit orgUnit = loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))
                .orElseThrow(
                        () -> new InvalidProjectDataException("Không tìm thấy đơn vị tổ chức với ID: " + orgUnitId));
        if (orgUnit.getStatus() != OrgUnitStatus.ACTIVE) {
            throw new InvalidProjectDataException("Đơn vị tổ chức đã bị vô hiệu hóa");
        }
        return orgUnit;
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
                        deniedAuditDetails(currentUser, reason)));
    }

    private String deniedAuditDetails(User currentUser, String reason) {
        return "permission=PROJECT_CREATE"
                + ";dataScope=" + currentUser.getDataScope()
                + ";scopeOrgUnitId=" + currentUser.getScopeOrgUnitId()
                + ";reason=" + reason;
    }

    // ===== Generate project code =======
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

    private String generateProjectCode(OrgUnit orgUnit) {
        String unitCode = (orgUnit.getUnitCode() != null && !orgUnit.getUnitCode().isBlank())
                ? orgUnit.getUnitCode().trim().toUpperCase()
                : "GEN";
        String datePart = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
        String randomHex = String.format("%06X", ThreadLocalRandom.current().nextInt(0x1000000));
        return String.format("PRJ-%s-%s-%s", unitCode, datePart, randomHex);
    }
}
