package com.hrm.employeemanagement.application.service.project;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.project.demand.CreateProjectRoleCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleUsageResult;
import com.hrm.employeemanagement.application.dto.project.demand.UpdateProjectRoleCommand;
import com.hrm.employeemanagement.application.port.inbound.project.ActivateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.CheckProjectRoleUsageUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.CreateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.DeactivateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectRolesUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.UpdateProjectRoleUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.CountProjectRoleUsagePort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SyncEmployeeProfessionalRolePort;
import com.hrm.employeemanagement.application.port.outbound.skill.LoadSkillGroupPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleCodeException;
import com.hrm.employeemanagement.domain.exception.role.DuplicateProjectRoleNameException;
import com.hrm.employeemanagement.domain.exception.role.InvalidProjectRoleDataException;
import com.hrm.employeemanagement.domain.exception.role.RoleNotFoundException;
import com.hrm.employeemanagement.domain.exception.skill.SkillGroupNotFoundException;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.skill.SkillGroup;
import com.hrm.employeemanagement.domain.skill.SkillGroupId;
import com.hrm.employeemanagement.domain.skill.SkillStatus;

public class ProjectRoleManagementService implements
        CreateProjectRoleUseCase,
        UpdateProjectRoleUseCase,
        DeactivateProjectRoleUseCase,
        ActivateProjectRoleUseCase,
        CheckProjectRoleUsageUseCase,
        GetProjectRolesUseCase {

    private final LoadProjectRolePort loadProjectRolePort;
    private final SaveProjectRolePort saveProjectRolePort;
    private final CountProjectRoleUsagePort countUsagePort;
    private final LoadSkillGroupPort loadSkillGroupPort;
    private final AuthorizationService authorizationService;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SyncEmployeeProfessionalRolePort syncEmployeeProfessionalRolePort;

    public ProjectRoleManagementService(
            LoadProjectRolePort loadProjectRolePort,
            SaveProjectRolePort saveProjectRolePort,
            CountProjectRoleUsagePort countUsagePort,
            LoadSkillGroupPort loadSkillGroupPort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort) {
        this(loadProjectRolePort, saveProjectRolePort, countUsagePort, loadSkillGroupPort, authorizationService, saveAuditLogPort, null);
    }

    public ProjectRoleManagementService(
            LoadProjectRolePort loadProjectRolePort,
            SaveProjectRolePort saveProjectRolePort,
            CountProjectRoleUsagePort countUsagePort,
            LoadSkillGroupPort loadSkillGroupPort,
            AuthorizationService authorizationService,
            SaveAuditLogPort saveAuditLogPort,
            SyncEmployeeProfessionalRolePort syncEmployeeProfessionalRolePort) {
        this.loadProjectRolePort = Objects.requireNonNull(loadProjectRolePort, "LoadProjectRolePort must not be null");
        this.saveProjectRolePort = Objects.requireNonNull(saveProjectRolePort, "SaveProjectRolePort must not be null");
        this.countUsagePort = Objects.requireNonNull(countUsagePort, "CountProjectRoleUsagePort must not be null");
        this.loadSkillGroupPort = Objects.requireNonNull(loadSkillGroupPort, "LoadSkillGroupPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.syncEmployeeProfessionalRolePort = syncEmployeeProfessionalRolePort;
    }

    @Override
    public ProjectRoleResult createProjectRole(CreateProjectRoleCommand command) {
        if (command == null) {
            throw new InvalidProjectRoleDataException("Dữ liệu vai trò chuyên môn không được null");
        }
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE);

        String trimmedCode = command.code() != null ? command.code().trim().toUpperCase() : "";
        if (loadProjectRolePort.existsByCodeIgnoreCase(trimmedCode)) {
            throw new DuplicateProjectRoleCodeException(trimmedCode);
        }

        String trimmedName = command.name() != null ? command.name().trim() : "";
        if (loadProjectRolePort.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateProjectRoleNameException(trimmedName);
        }

        SkillGroup group = validateAndGetActiveSkillGroup(command.skillGroupId());

        ProjectRole role = ProjectRole.createNew(trimmedCode, trimmedName, command.description(), command.skillGroupId());
        ProjectRole saved = saveProjectRolePort.save(role);

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PROJECT_ROLE_CREATED",
                "project_roles",
                saved.getIdValue(),
                null,
                "code=" + saved.getCode() + ";name=" + saved.getName() + ";skillGroupId=" + saved.getSkillGroupId()
        ));

        return toResult(saved, group.getName());
    }

    @Override
    public ProjectRoleResult updateProjectRole(UpdateProjectRoleCommand command) {
        if (command == null || command.id() == null) {
            throw new InvalidProjectRoleDataException("ID vai trò chuyên môn không được để trống");
        }
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE);

        ProjectRole role = loadProjectRolePort.findById(new ProjectRoleId(command.id()))
                .orElseThrow(() -> new RoleNotFoundException("Không tìm thấy vai trò chuyên môn với ID: " + command.id()));

        String trimmedName = command.name() != null ? command.name().trim() : "";
        if (loadProjectRolePort.existsByNameIgnoreCaseAndIdNot(trimmedName, command.id())) {
            throw new DuplicateProjectRoleNameException(trimmedName);
        }

        SkillGroup group = validateAndGetActiveSkillGroup(command.skillGroupId());

        String oldName = role.getName();
        String oldValue = "name=" + role.getName() + ";description=" + role.getDescription() + ";skillGroupId=" + role.getSkillGroupId();
        role.updateInfo(trimmedName, command.skillGroupId(), command.description());
        ProjectRole saved = saveProjectRolePort.save(role);

        if (!oldName.equalsIgnoreCase(trimmedName) && syncEmployeeProfessionalRolePort != null) {
            syncEmployeeProfessionalRolePort.syncRoleName(oldName, trimmedName);
        }

        String newValue = "name=" + saved.getName() + ";description=" + saved.getDescription() + ";skillGroupId=" + saved.getSkillGroupId();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PROJECT_ROLE_UPDATED",
                "project_roles",
                saved.getIdValue(),
                oldValue,
                newValue
        ));

        return toResult(saved, group.getName());
    }

    @Override
    public ProjectRoleResult deactivateProjectRole(Long roleId) {
        if (roleId == null) {
            throw new InvalidProjectRoleDataException("ID vai trò chuyên môn không được để trống");
        }
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE);

        ProjectRole role = loadProjectRolePort.findById(new ProjectRoleId(roleId))
                .orElseThrow(() -> new RoleNotFoundException("Không tìm thấy vai trò chuyên môn với ID: " + roleId));

        String oldValue = "status=" + role.getStatus().name();
        role.deactivate();
        ProjectRole saved = saveProjectRolePort.save(role);
        String newValue = "status=" + saved.getStatus().name();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PROJECT_ROLE_DEACTIVATED",
                "project_roles",
                saved.getIdValue(),
                oldValue,
                newValue
        ));

        String groupName = getSkillGroupName(saved.getSkillGroupId());
        return toResult(saved, groupName);
    }

    @Override
    public ProjectRoleResult activateProjectRole(Long roleId) {
        if (roleId == null) {
            throw new InvalidProjectRoleDataException("ID vai trò chuyên môn không được để trống");
        }
        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_ROLE_MANAGE);

        ProjectRole role = loadProjectRolePort.findById(new ProjectRoleId(roleId))
                .orElseThrow(() -> new RoleNotFoundException("Không tìm thấy vai trò chuyên môn với ID: " + roleId));

        // Kiểm tra nhóm kỹ năng liên kết vẫn phải đang hoạt động
        validateAndGetActiveSkillGroup(role.getSkillGroupId());

        String oldValue = "status=" + role.getStatus().name();
        role.activate();
        ProjectRole saved = saveProjectRolePort.save(role);
        String newValue = "status=" + saved.getStatus().name();

        saveAuditLogPort.save(AuditLog.createChange(
                currentUserId,
                "PROJECT_ROLE_ACTIVATED",
                "project_roles",
                saved.getIdValue(),
                oldValue,
                newValue
        ));

        String groupName = getSkillGroupName(saved.getSkillGroupId());
        return toResult(saved, groupName);
    }

    @Override
    public ProjectRoleUsageResult checkUsage(Long roleId) {
        if (roleId == null) {
            throw new InvalidProjectRoleDataException("ID vai trò chuyên môn không được để trống");
        }
        authorizationService.requireAny(PermissionCode.PROJECT_ROLE_MANAGE, PermissionCode.PROJECT_ROLE_READ);

        ProjectRole role = loadProjectRolePort.findById(new ProjectRoleId(roleId))
                .orElseThrow(() -> new RoleNotFoundException("Không tìm thấy vai trò chuyên môn với ID: " + roleId));

        long demandCount = countUsagePort.countDemandsByRoleId(roleId);
        long employeeCount = countUsagePort.countEmployeesByProfessionalRole(role.getName(), role.getCode());
        boolean inUse = demandCount > 0 || employeeCount > 0;

        String warningMessage = null;
        if (inUse) {
            warningMessage = String.format(
                    "Vai trò '%s' đang được dùng trong %d nhu cầu dự án và %d hồ sơ nhân sự. Khi ngừng sử dụng, vai trò sẽ bị ẩn khỏi danh sách chọn mới nhưng lịch sử vẫn được bảo toàn.",
                    role.getName(), demandCount, employeeCount);
        }

        return new ProjectRoleUsageResult(roleId, inUse, demandCount, employeeCount, warningMessage);
    }

    @Override
    public List<ProjectRoleResult> getProjectRoles() {
        return getProjectRoles(false);
    }

    @Override
    public List<ProjectRoleResult> getProjectRoles(boolean includeInactive) {
        authorizationService.requireAny(
                PermissionCode.PROJECT_ROLE_READ,
                PermissionCode.PROJECT_ROLE_MANAGE
        );

        List<ProjectRole> roles = includeInactive
                ? loadProjectRolePort.findAll()
                : loadProjectRolePort.findAllActive();

        Map<Long, String> groupNameMap = loadSkillGroupPort.findAll().stream()
                .collect(Collectors.toMap(g -> g.getId().value(), SkillGroup::getName, (a, b) -> a));

        return roles.stream()
                .map(r -> toResult(r, groupNameMap.get(r.getSkillGroupId())))
                .toList();
    }

    private SkillGroup validateAndGetActiveSkillGroup(Long skillGroupId) {
        if (skillGroupId == null) {
            throw new InvalidProjectRoleDataException("Nhóm kỹ năng không được để trống");
        }
        SkillGroup group = loadSkillGroupPort.findById(new SkillGroupId(skillGroupId))
                .orElseThrow(() -> new SkillGroupNotFoundException("Không tìm thấy nhóm kỹ năng với ID: " + skillGroupId));

        if (group.getStatus() != SkillStatus.ACTIVE) {
            throw new InvalidProjectRoleDataException("Nhóm kỹ năng '" + group.getName() + "' đang ở trạng thái ngừng hoạt động");
        }
        return group;
    }

    private String getSkillGroupName(Long skillGroupId) {
        if (skillGroupId == null) return null;
        return loadSkillGroupPort.findById(new SkillGroupId(skillGroupId))
                .map(SkillGroup::getName)
                .orElse(null);
    }

    private ProjectRoleResult toResult(ProjectRole role, String skillGroupName) {
        return new ProjectRoleResult(
                role.getIdValue(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getSkillGroupId(),
                skillGroupName,
                role.getStatus() != null ? role.getStatus().name() : "ACTIVE"
        );
    }
}
