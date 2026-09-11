package com.hrm.employeemanagement.application.service.project;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.project.demand.EstimateResourceDemandCommand;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectResourceDemandSummaryResult;
import com.hrm.employeemanagement.application.dto.project.demand.ProjectRoleResult;
import com.hrm.employeemanagement.application.dto.project.demand.RoleResourceDemandResult;
import com.hrm.employeemanagement.application.dto.project.demand.WeeklyDemandItemResult;
import com.hrm.employeemanagement.application.port.inbound.project.DeleteProjectResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.EstimateResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectResourceDemandUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectRolesUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectRolePort;
import com.hrm.employeemanagement.application.port.outbound.project.SaveProjectResourceDemandPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.InvalidProjectDataException;
import com.hrm.employeemanagement.domain.exception.project.ProjectDateNotConfiguredException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.role.RoleNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemand;
import com.hrm.employeemanagement.domain.project.demand.ProjectResourceDemandPolicy;
import com.hrm.employeemanagement.domain.project.demand.ProjectRole;
import com.hrm.employeemanagement.domain.project.demand.ProjectRoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ProjectResourceDemandService implements
        EstimateResourceDemandUseCase,
        GetProjectResourceDemandUseCase,
        DeleteProjectResourceDemandUseCase,
        GetProjectRolesUseCase {

    private final LoadProjectPort loadProjectPort;
    private final LoadProjectRolePort loadProjectRolePort;
    private final LoadProjectResourceDemandPort loadDemandPort;
    private final SaveProjectResourceDemandPort saveDemandPort;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort;
    private final AuthorizationService authorizationService;

    public ProjectResourceDemandService(
            LoadProjectPort loadProjectPort,
            LoadProjectRolePort loadProjectRolePort,
            LoadProjectResourceDemandPort loadDemandPort,
            SaveProjectResourceDemandPort saveDemandPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            SaveAuditLogPort saveAuditLogPort,
            SaveAuditLogInNewTransactionPort saveDeniedAuditLogPort,
            AuthorizationService authorizationService) {
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadProjectRolePort = Objects.requireNonNull(loadProjectRolePort, "LoadProjectRolePort must not be null");
        this.loadDemandPort = Objects.requireNonNull(loadDemandPort, "LoadProjectResourceDemandPort must not be null");
        this.saveDemandPort = Objects.requireNonNull(saveDemandPort, "SaveProjectResourceDemandPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveAuditLogPort = Objects.requireNonNull(saveAuditLogPort, "SaveAuditLogPort must not be null");
        this.saveDeniedAuditLogPort = Objects.requireNonNull(saveDeniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public ProjectResourceDemandSummaryResult estimateDemand(EstimateResourceDemandCommand command) {
        if (command == null || command.projectId() == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }
        if (command.roleId() == null) {
            throw new InvalidProjectDataException("Vai trò chuyên môn (roleId) không được để trống");
        }
        ProjectResourceDemandPolicy.validateRequiredHours(command.hoursPerWeek());

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(command.projectId()))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + command.projectId()));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new InvalidProjectDataException("Chỉ có thể ước lượng nhu cầu nhân sự cho dự án đang ở trạng thái hoạt động");
        }

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_DEMAND_ESTIMATE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE);
        }

        if (project.getStartDate() == null || project.getEndDate() == null) {
            throw ProjectDateNotConfiguredException.missingDates(project.getIdValue());
        }

        ProjectRole role = loadProjectRolePort.findById(new ProjectRoleId(command.roleId()))
                .orElseThrow(() -> new RoleNotFoundException("Không tìm thấy vai trò chuyên môn với ID: " + command.roleId()));

        List<YearWeek> projectWeeks = ProjectResourceDemandPolicy.calculateProjectWeeks(
                project.getStartDate(), project.getEndDate());
        Set<YearWeek> validProjectWeeksSet = Set.copyOf(projectWeeks);

        List<ProjectResourceDemand> existingRoleDemands = loadDemandPort.findByProjectIdAndRoleId(
                project.getId(), role.getId());

        Map<YearWeek, ProjectResourceDemand> existingMap = existingRoleDemands.stream()
                .collect(Collectors.toMap(ProjectResourceDemand::getYearWeek, d -> d, (a, b) -> a));

        List<ProjectResourceDemand> demandsToSave = new ArrayList<>();
        for (YearWeek yw : projectWeeks) {
            ProjectResourceDemand existing = existingMap.get(yw);
            if (existing != null) {
                existing.updateRequiredHours(command.hoursPerWeek());
                demandsToSave.add(existing);
            } else {
                demandsToSave.add(ProjectResourceDemand.createNew(
                        project.getId(), role.getId(), yw, command.hoursPerWeek()));
            }
        }

        // Dọn dẹp các demand cũ của vai trò này nằm ngoài khoảng thời gian dự án (stale weeks)
        List<ProjectResourceDemand> staleDemands = existingRoleDemands.stream()
                .filter(d -> !validProjectWeeksSet.contains(d.getYearWeek()))
                .toList();

        if (!staleDemands.isEmpty()) {
            saveDemandPort.deleteAll(staleDemands);
        }

        saveDemandPort.saveAll(demandsToSave);

        // NCL-03-CN-007-TC-04: Ghi nhận nhật ký kiểm toán (Audit Log)
        saveAuditLogPort.save(AuditLog.create(
                currentUserId,
                "ESTIMATE_RESOURCE_DEMAND",
                "project_resource_demands",
                project.getIdValue()
        ));

        return buildSummaryResult(project);
    }

    @Override
    public ProjectResourceDemandSummaryResult getProjectResourceDemands(Long projectId) {
        if (projectId == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_READ);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_DEMAND_READ");
            throw new PermissionDeniedException(PermissionCode.PROJECT_RESOURCE_DEMAND_READ);
        }

        return buildSummaryResult(project);
    }

    @Override
    public ProjectResourceDemandSummaryResult deleteDemand(Long projectId, Long roleId) {
        if (projectId == null) {
            throw new InvalidProjectDataException("Mã dự án (projectId) không được để trống");
        }
        if (roleId == null) {
            throw new InvalidProjectDataException("Vai trò chuyên môn (roleId) không được để trống");
        }

        Long currentUserId = authorizationService.require(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE);
        User currentUser = loadCurrentUserOrThrow(currentUserId);

        Project project = loadProjectPort.findById(new ProjectId(projectId))
                .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new InvalidProjectDataException("Chỉ có thể xóa ước lượng nhu cầu nhân sự cho dự án đang ở trạng thái hoạt động");
        }

        if (!canAccessProject(currentUser, currentUserId, project)) {
            saveDeniedAudit(currentUserId, currentUser, project.getIdValue(), "OUTSIDE_DATA_SCOPE_DEMAND_DELETE");
            throw new PermissionDeniedException(PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE);
        }

        ProjectRole role = loadProjectRolePort.findById(new ProjectRoleId(roleId))
                .orElseThrow(() -> new RoleNotFoundException("Không tìm thấy vai trò chuyên môn với ID: " + roleId));

        List<ProjectResourceDemand> existingRoleDemands = loadDemandPort.findByProjectIdAndRoleId(
                project.getId(), role.getId());

        if (!existingRoleDemands.isEmpty()) {
            saveDemandPort.deleteAll(existingRoleDemands);
        }

        saveAuditLogPort.save(AuditLog.create(
                currentUserId,
                "DELETE_RESOURCE_DEMAND",
                "project_resource_demands",
                project.getIdValue()
        ));

        return buildSummaryResult(project);
    }

    @Override
    public List<ProjectRoleResult> getProjectRoles(boolean includeInactive) {
        authorizationService.requireAny(
                PermissionCode.PROJECT_READ,
                PermissionCode.PROJECT_RESOURCE_DEMAND_READ,
                PermissionCode.PROJECT_RESOURCE_DEMAND_ESTIMATE
        );
        List<ProjectRole> roles = includeInactive
                ? loadProjectRolePort.findAll()
                : loadProjectRolePort.findAllActive();
        return roles.stream()
                .map(r -> new ProjectRoleResult(r.getIdValue(), r.getCode(), r.getName(), r.getDescription()))
                .toList();
    }

    @Override
    public List<ProjectRoleResult> getProjectRoles() {
        return getProjectRoles(false);
    }

    // ==================== HELPER METHODS ====================

    private ProjectResourceDemandSummaryResult buildSummaryResult(Project project) {
        List<ProjectResourceDemand> allDemands = loadDemandPort.findByProjectId(project.getId());

        // Lọc các bản ghi nhu cầu thuộc các tuần hợp lệ theo ngày của dự án
        List<ProjectResourceDemand> activeDemands = allDemands;
        if (project.getStartDate() != null && project.getEndDate() != null) {
            List<YearWeek> validWeeks = ProjectResourceDemandPolicy.calculateProjectWeeks(
                    project.getStartDate(), project.getEndDate());
            Set<YearWeek> validWeeksSet = Set.copyOf(validWeeks);
            activeDemands = allDemands.stream()
                    .filter(d -> validWeeksSet.contains(d.getYearWeek()))
                    .toList();
        }

        List<ProjectRole> allRoles = loadProjectRolePort.findAll();
        Map<Long, ProjectRole> roleMap = allRoles.stream()
                .filter(r -> r.getId() != null)
                .collect(Collectors.toMap(ProjectRole::getIdValue, r -> r, (a, b) -> a));

        Map<Long, List<ProjectResourceDemand>> groupedByRole = activeDemands.stream()
                .collect(Collectors.groupingBy(ProjectResourceDemand::getRoleIdValue));

        List<RoleResourceDemandResult> roleResults = new ArrayList<>();
        BigDecimal totalDemandHours = BigDecimal.ZERO;

        for (Map.Entry<Long, List<ProjectResourceDemand>> entry : groupedByRole.entrySet()) {
            Long roleId = entry.getKey();
            List<ProjectResourceDemand> demands = entry.getValue();

            ProjectRole role = roleMap.get(roleId);
            String roleCode = role != null ? role.getCode() : "ROLE_" + roleId;
            String roleName = role != null ? role.getName() : "Vai trò " + roleId;

            demands.sort(Comparator.comparingInt(ProjectResourceDemand::getYear)
                    .thenComparingInt(ProjectResourceDemand::getWeekNumber));

            BigDecimal totalRoleHours = demands.stream()
                    .map(ProjectResourceDemand::getRequiredHours)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalDemandHours = totalDemandHours.add(totalRoleHours);

            List<WeeklyDemandItemResult> weeklyItems = demands.stream()
                    .map(d -> new WeeklyDemandItemResult(
                            d.getYear(),
                            d.getWeekNumber(),
                            d.getYearWeek().getStartDate(),
                            d.getYearWeek().getEndDate(),
                            d.getRequiredHours()))
                    .toList();

            roleResults.add(new RoleResourceDemandResult(
                    roleId,
                    roleCode,
                    roleName,
                    totalRoleHours,
                    weeklyItems));
        }

        roleResults.sort(Comparator.comparing(RoleResourceDemandResult::roleId));

        boolean exceeds = ProjectResourceDemandPolicy.isExceedingBudget(
                totalDemandHours, project.getEstimatedHours());

        String warningMessage = exceeds
                ? String.format(
                        "Nhu cầu nhân sự vượt quá quy mô dự án đã khai báo (Tổng nhu cầu: %s giờ / Quy mô dự án: %s giờ)",
                        totalDemandHours, project.getEstimatedHours())
                : null;

        return new ProjectResourceDemandSummaryResult(
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                project.getEstimatedHours(),
                totalDemandHours,
                exceeds,
                warningMessage,
                roleResults);
    }

    private boolean canAccessProject(User currentUser, Long currentUserId, Project project) {
        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case ORGANIZATION_BRANCH ->
                currentUser.getScopeOrgUnitId() != null
                        && loadProjectPort.existsInOrgUnitBranch(project.getIdValue(), currentUser.getScopeOrgUnitId());
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
                        "project_resource_demands",
                        projectId,
                        null,
                        "permission=PROJECT_RESOURCE_DEMAND;dataScope=" + currentUser.getDataScope() + ";reason=" + reason));
    }
}