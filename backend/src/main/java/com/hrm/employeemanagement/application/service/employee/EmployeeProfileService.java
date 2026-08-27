package com.hrm.employeemanagement.application.service.employee;

import java.util.Objects;

import com.hrm.employeemanagement.application.dto.employee.CreateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.dto.employee.EmployeeProfileResult;
import com.hrm.employeemanagement.application.dto.employee.UpdateEmployeeProfileCommand;
import com.hrm.employeemanagement.application.port.inbound.employee.CreateEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.employee.GetEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.employee.UpdateEmployeeProfileUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeNotFoundException;
import com.hrm.employeemanagement.domain.exception.employee.EmployeeVersionConflictException;
import com.hrm.employeemanagement.domain.exception.employee.InvalidEmployeeDataException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class EmployeeProfileService implements CreateEmployeeProfileUseCase,
        UpdateEmployeeProfileUseCase, GetEmployeeProfileUseCase {

    private final LoadEmployeePort loadEmployeePort;
    private final SaveEmployeePort saveEmployeePort;
    private final LoadUserPort loadUserPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final AuthorizationService authorizationService;

    public EmployeeProfileService(LoadEmployeePort loadEmployeePort,
                                  SaveEmployeePort saveEmployeePort,
                                  LoadUserPort loadUserPort,
                                  LoadOrgUnitPort loadOrgUnitPort,
                                  AuthorizationService authorizationService) {
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.saveEmployeePort = Objects.requireNonNull(saveEmployeePort, "SaveEmployeePort must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadOrgUnitPort = Objects.requireNonNull(loadOrgUnitPort, "LoadOrgUnitPort must not be null");
        this.authorizationService = Objects.requireNonNull(
                authorizationService, "AuthorizationService must not be null");
    }

    @Override
    public EmployeeProfileResult execute(CreateEmployeeProfileCommand command) {
        User currentUser = requireCurrentUser(PermissionCode.EMPLOYEE_UPDATE);
        requireOrgUnitInScope(currentUser, command.orgUnitId(), null, PermissionCode.EMPLOYEE_UPDATE);
        requireActiveOrgUnit(command.orgUnitId());

        UserId userId = new UserId(command.userId());
        if (loadUserPort.findById(userId).isEmpty()) {
            throw new UserNotFoundException("Không tìm thấy người dùng với ID: " + command.userId());
        }
        if (loadEmployeePort.findByUserId(userId).isPresent()) {
            throw new InvalidEmployeeDataException(
                    "Tài khoản người dùng đã được liên kết với một hồ sơ nhân sự khác");
        }
        if (loadEmployeePort.existsByEmployeeCode(command.employeeCode())) {
            throw new InvalidEmployeeDataException(
                    "Mã nhân viên '" + command.employeeCode() + "' đã tồn tại");
        }

        Employee employee = Employee.createNewProfile(
                userId, command.orgUnitId(), command.employeeCode(), command.fullName(),
                command.professionalRole(), command.startDate(), command.contractEndDate(),
                command.standardHoursPerWeek());
        return EmployeeProfileResult.fromDomain(saveEmployeePort.save(employee));
    }

    @Override
    public EmployeeProfileResult execute(UpdateEmployeeProfileCommand command) {
        User currentUser = requireCurrentUser(PermissionCode.EMPLOYEE_UPDATE);
        Employee employee = loadEmployeePort.findById(new EmployeeId(command.employeeId()))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự"));

        if (!Objects.equals(command.version(), employee.getVersion())) {
            throw new EmployeeVersionConflictException(
                    "Hồ sơ nhân sự đã được cập nhật bởi người dùng khác. Vui lòng tải lại dữ liệu và thử lại.");
        }

        requireEmployeeInScope(currentUser, employee, PermissionCode.EMPLOYEE_UPDATE);
        requireOrgUnitInScope(currentUser, command.orgUnitId(), employee, PermissionCode.EMPLOYEE_UPDATE);
        requireActiveOrgUnit(command.orgUnitId());
        employee.updateProfile(command.fullName(), command.orgUnitId(), command.professionalRole(),
                command.startDate(), command.contractEndDate(), command.standardHoursPerWeek());
        return EmployeeProfileResult.fromDomain(saveEmployeePort.save(employee));
    }

    @Override
    public EmployeeProfileResult getById(Long employeeId) {
        User currentUser = requireCurrentUser(PermissionCode.EMPLOYEE_READ);
        Employee employee = loadEmployeePort.findById(new EmployeeId(employeeId))
                .orElseThrow(() -> new EmployeeNotFoundException("Không tìm thấy hồ sơ nhân sự"));
        requireEmployeeInScope(currentUser, employee, PermissionCode.EMPLOYEE_READ);
        return EmployeeProfileResult.fromDomain(employee);
    }

    @Override
    public EmployeeProfileResult getByUserId(Long userId) {
        User currentUser = requireCurrentUser(PermissionCode.EMPLOYEE_READ);
        Employee employee = loadEmployeePort.findByUserId(new UserId(userId))
                .orElseThrow(() -> new EmployeeNotFoundException(
                        "Tài khoản chưa được khởi tạo hồ sơ nhân sự"));
        requireEmployeeInScope(currentUser, employee, PermissionCode.EMPLOYEE_READ);
        return EmployeeProfileResult.fromDomain(employee);
    }

    private User requireCurrentUser(PermissionCode permission) {
        Long currentUserId = authorizationService.require(permission);
        return loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException(
                        "Không tìm thấy người dùng hiện tại với ID: " + currentUserId));
    }

    private void requireEmployeeInScope(User currentUser, Employee employee, PermissionCode permission) {
        boolean allowed = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUser.getIdValue().equals(employee.getUserIdValue());
            case ORGANIZATION_BRANCH -> employee.getOrgUnitId() != null
                    && currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(
                            employee.getOrgUnitId(), currentUser.getScopeOrgUnitId());
        };
        if (!allowed) {
            throw new PermissionDeniedException(permission);
        }
    }

    private void requireOrgUnitInScope(User currentUser, Long orgUnitId,
                                       Employee existingEmployee, PermissionCode permission) {
        boolean allowed = orgUnitId != null && switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> existingEmployee != null
                    && currentUser.getIdValue().equals(existingEmployee.getUserIdValue())
                    && orgUnitId.equals(existingEmployee.getOrgUnitId());
            case ORGANIZATION_BRANCH -> currentUser.getScopeOrgUnitId() != null
                    && loadOrgUnitPort.existsInOrgUnitBranch(
                            orgUnitId, currentUser.getScopeOrgUnitId());
        };
        if (!allowed) {
            throw new PermissionDeniedException(permission);
        }
    }

    private void requireActiveOrgUnit(Long orgUnitId) {
        boolean active = orgUnitId != null
                && loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))
                .map(orgUnit -> orgUnit.getStatus() == OrgUnitStatus.ACTIVE)
                .orElse(false);
        if (!active) {
            throw new InvalidEmployeeDataException(
                    "Đơn vị tổ chức không tồn tại hoặc không còn hoạt động");
        }
    }
}
