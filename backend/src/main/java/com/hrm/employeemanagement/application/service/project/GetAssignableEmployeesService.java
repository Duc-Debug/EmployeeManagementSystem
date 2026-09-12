package com.hrm.employeemanagement.application.service.project;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.project.ProjectMemberResult;
import com.hrm.employeemanagement.application.port.inbound.project.GetAssignableEmployeesUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.project.ProjectMemberRole;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class GetAssignableEmployeesService implements GetAssignableEmployeesUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final LoadUserPort loadUserPort;
    private final AuthorizationService authorizationService;

    public GetAssignableEmployeesService(
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadUserPort loadUserPort,
            AuthorizationService authorizationService) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.authorizationService = authorizationService;
    }

    public GetAssignableEmployeesService(
            LoadEmployeePort loadEmployeePort,
            LoadOrgUnitPort loadOrgUnitPort,
            LoadUserPort loadUserPort) {
        this(loadEmployeePort, loadOrgUnitPort, loadUserPort, null);
    }

    @Override
    public List<ProjectMemberResult> getAssignableEmployees() {
        return getAssignableEmployees(null);
    }

    @Override
    public List<ProjectMemberResult> getAssignableEmployees(java.time.LocalDate startDate) {
        User currentUser = null;
        if (authorizationService != null) {
            Long currentUserId = authorizationService.require(PermissionCode.PROJECT_WBS_MANAGE);
            currentUser = loadUserPort.findById(new UserId(currentUserId))
                    .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng hiện tại với ID: " + currentUserId));
        }
        List<Employee> activeEmployees = findEmployeesInScope(currentUser);
        if (activeEmployees.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> orgUnitIds = activeEmployees.stream()
                .map(Employee::getOrgUnitId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UserId> userIds = activeEmployees.stream()
                .map(Employee::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> orgNames = loadOrgUnitPort.findAllByIdIn(List.copyOf(orgUnitIds)).stream()
                .filter(orgUnit -> orgUnit.getId() != null && orgUnit.getUnitName() != null)
                .collect(Collectors.toMap(orgUnit -> orgUnit.getId().getValue(), orgUnit -> orgUnit.getUnitName()));

        Map<Long, User> users = loadUserPort.findAllByIdIn(List.copyOf(userIds)).stream()
                .filter(user -> user.getId() != null && user.getId().value() != null)
                .collect(Collectors.toMap(user -> user.getId().value(), user -> user));

        return activeEmployees.stream()
                // 1. Chỉ lấy nhân sự còn hoạt động (ACTIVE)
                .filter(emp -> emp.getStatus() == null || emp.getStatus() == EmployeeStatus.ACTIVE)
                // 2. Lọc theo Data Scope của người dùng hiện tại
                // 3. Loại trừ Quản trị viên hệ thống (VT-06) - Giao việc chỉ dành cho nhân sự thực thi dự án
                .filter(emp -> {
                    if (emp.getUserId() == null) return true;
                    User u = users.get(emp.getUserId().value());
                    if (u != null && u.getRole() != null && u.getRole().isSystemAdmin()) {
                        return false;
                    }
                    return true;
                })
                // 4. Loại trừ nhân sự có ngày kết thúc hợp đồng trước khoảng thời gian được xét
                .filter(emp -> {
                    if (startDate != null && emp.getContractEndDate() != null) {
                        return !emp.getContractEndDate().isBefore(startDate);
                    }
                    return true;
                })
                .map(emp -> {
                    User u = emp.getUserId() != null ? users.get(emp.getUserId().value()) : null;
                    String email = u != null ? u.getEmail() : null;
                    return new ProjectMemberResult(
                            emp.getIdValue(),
                            emp.getEmployeeCode(),
                            emp.getFullName(),
                            email,
                            emp.getOrgUnitId(),
                            emp.getProfessionalRole() != null && !emp.getProfessionalRole().isBlank()
                                    ? emp.getProfessionalRole()
                                    : (emp.getOrgUnitId() != null ? orgNames.get(emp.getOrgUnitId()) : null),
                            ProjectMemberRole.MEMBER,
                            emp.getStatus() != null ? emp.getStatus().name() : "ACTIVE",
                            emp.getContractEndDate()
                    );
                })
                .toList();
    }

    private List<Employee> findEmployeesInScope(User currentUser) {
        if (currentUser == null
                || currentUser.getDataScope() != com.hrm.employeemanagement.domain.authorization.DataScope.ORGANIZATION_BRANCH) {
            return loadEmployeePort.findAllActive();
        }
        Long scopeOrgUnitId = currentUser.getScopeOrgUnitId();
        if (scopeOrgUnitId == null) {
            return List.of();
        }
        return loadOrgUnitPort.findById(new OrgUnitId(scopeOrgUnitId))
                .map(scope -> loadOrgUnitPort.findSubTree(scope.getTreePath()).stream()
                        .map(orgUnit -> orgUnit.getId() != null ? orgUnit.getId().getValue() : null)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .filter(ids -> !ids.isEmpty())
                .map(loadEmployeePort::findActiveByOrgUnitIds)
                .orElseGet(List::of);
    }
}
