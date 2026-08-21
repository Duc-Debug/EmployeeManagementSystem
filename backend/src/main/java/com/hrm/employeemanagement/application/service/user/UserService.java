package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.port.inbound.user.CreateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.GetUserListUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ToggleUserStatusUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.UpdateUserRoleUseCase;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.*;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure Java 100% Application Service (No Spring Annotations allowed).
 * Implements Inbound Use Case ports and interacts with Outbound Ports.
 */
public class UserService implements CreateUserUseCase, ToggleUserStatusUseCase, UpdateUserRoleUseCase, GetUserListUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadRolePort loadRolePort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveEmployeePort saveEmployeePort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final PasswordEncoderPort passwordEncoder;

    public UserService(LoadUserPort loadUserPort,
                       SaveUserPort saveUserPort,
                       LoadRolePort loadRolePort,
                       LoadEmployeePort loadEmployeePort,
                       SaveEmployeePort saveEmployeePort,
                       SaveAuditLogPort saveAuditLogPort,
                       PasswordEncoderPort passwordEncoder) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.loadRolePort = loadRolePort;
        this.loadEmployeePort = loadEmployeePort;
        this.saveEmployeePort = saveEmployeePort;
        this.saveAuditLogPort = saveAuditLogPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResult createUser(CreateUserCommand command, Long currentAdminId) {
        if (loadUserPort.existsByUsername(command.username())) {
            throw new DuplicateUsernameException("Tên đăng nhập đã tồn tại: " + command.username());
        }

        RoleCode roleCode = RoleCode.fromCode(command.roleCode());
        Role role = loadRolePort.findByCode(roleCode)
                .orElseGet(() -> loadRolePort.save(new Role(null, roleCode, roleCode.getName())));

        String encodedPassword = passwordEncoder.encode(command.password());
        User user = User.createNew(command.username(), encodedPassword, role, null);
        User savedUser = saveUserPort.save(user);

        // Link Employee profile
        Employee employee = Employee.createNew(savedUser.getId(), command.departmentId(), command.employeeCode(), command.fullName());
        Employee savedEmployee = saveEmployeePort.save(employee);

        savedUser.setEmployeeId(savedEmployee.getId());
        saveUserPort.save(savedUser);

        // Audit logging
        saveAuditLogPort.save(AuditLog.create(currentAdminId, "CREATE_USER", "users", savedUser.getIdValue()));

        return mapToUserResult(savedUser, savedEmployee);
    }

    @Override
    public UserResult toggleUserStatus(Long userId, boolean lock, Long currentAdminId) {
        UserId uId = new UserId(userId);
        User user = loadUserPort.findById(uId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        long activeAdminCount = loadUserPort.countActiveAdmins();
        UserId adminId = currentAdminId != null ? new UserId(currentAdminId) : null;

        if (lock) {
            user.lock(adminId, activeAdminCount);
            saveAuditLogPort.save(AuditLog.create(currentAdminId, "LOCK_USER", "users", user.getIdValue()));
        } else {
            user.unlock();
            saveAuditLogPort.save(AuditLog.create(currentAdminId, "UNLOCK_USER", "users", user.getIdValue()));
        }

        User updatedUser = saveUserPort.save(user);
        Employee employee = loadEmployeePort.findByUserId(updatedUser.getId()).orElse(null);
        return mapToUserResult(updatedUser, employee);
    }

    @Override
    public UserResult updateUserRole(UpdateUserRoleCommand command, Long currentAdminId) {
        UserId uId = new UserId(command.userId());
        User user = loadUserPort.findById(uId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + command.userId()));

        RoleCode newRoleCode = RoleCode.fromCode(command.roleCode());
        Role newRole = loadRolePort.findByCode(newRoleCode)
                .orElseGet(() -> loadRolePort.save(new Role(null, newRoleCode, newRoleCode.getName())));

        long activeAdminCount = loadUserPort.countActiveAdmins();
        user.changeRole(newRole, activeAdminCount);

        User updatedUser = saveUserPort.save(user);

        Employee employee = loadEmployeePort.findByUserId(updatedUser.getId()).orElse(null);
        if (employee != null && command.departmentId() != null) {
            employee.setDepartmentId(command.departmentId());
            saveEmployeePort.save(employee);
        }

        saveAuditLogPort.save(AuditLog.create(currentAdminId, "UPDATE_ROLE", "users", updatedUser.getIdValue()));

        return mapToUserResult(updatedUser, employee);
    }

    @Override
    public PageResult<UserResult> getUsers(int page, int size) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = Math.min(Math.max(size, 1), 100); // MAX_PAGE_SIZE = 100

        List<User> users = loadUserPort.findAll(validatedPage, validatedSize);
        long totalElements = loadUserPort.count();

        // Batch Resolving: Avoid N+1 Query Problem by using WHERE user_id IN (...)
        List<UserId> userIds = users.stream().map(User::getId).filter(java.util.Objects::nonNull).toList();
        Map<UserId, Employee> employeeMap = loadEmployeePort.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(Employee::getUserId, emp -> emp, (e1, e2) -> e1));

        List<UserResult> userResults = users.stream()
                .map(user -> mapToUserResult(user, employeeMap.get(user.getId())))
                .toList();

        return new PageResult<>(userResults, validatedPage, validatedSize, totalElements);
    }

    @Override
    public UserResult getUserById(Long id) {
        UserId uId = new UserId(id);
        User user = loadUserPort.findById(uId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + id));
        Employee employee = loadEmployeePort.findByUserId(user.getId()).orElse(null);
        return mapToUserResult(user, employee);
    }

    private UserResult mapToUserResult(User user, Employee employee) {
        return new UserResult(
                user.getIdValue(),
                user.getUsername(),
                user.getRole().getCode().getCode(),
                user.getRole().getName(),
                user.getStatus(),
                employee != null ? employee.getIdValue() : null,
                employee != null ? employee.getFullName() : null,
                employee != null ? employee.getDepartmentId() : null,
                employee != null ? "Department #" + employee.getDepartmentId() : null
        );
    }
}
