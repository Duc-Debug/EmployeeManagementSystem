package com.hrm.employeemanagement.application.service.user;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.model.audit.AuditLog;
import com.hrm.employeemanagement.domain.model.employee.Employee;
import com.hrm.employeemanagement.domain.model.role.Role;
import com.hrm.employeemanagement.domain.model.role.RoleCode;
import com.hrm.employeemanagement.domain.model.user.User;
import com.hrm.employeemanagement.domain.repository.user.AuditLogRepository;
import com.hrm.employeemanagement.domain.repository.user.EmployeeRepository;
import com.hrm.employeemanagement.domain.repository.user.RoleRepository;
import com.hrm.employeemanagement.domain.repository.user.UserRepository;
import com.hrm.employeemanagement.port.in.user.CreateUserUseCase;
import com.hrm.employeemanagement.port.in.user.GetUserListUseCase;
import com.hrm.employeemanagement.port.in.user.ToggleUserStatusUseCase;
import com.hrm.employeemanagement.port.in.user.UpdateUserRoleUseCase;
import com.hrm.employeemanagement.port.out.user.PasswordEncoderPort;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure Java 100% Application Service (No Spring Annotations allowed).
 * Implements Use Case ports. Bean registration & transactions are handled in Infrastructure.
 */
public class UserService implements CreateUserUseCase, ToggleUserStatusUseCase, UpdateUserRoleUseCase, GetUserListUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoderPort passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       EmployeeRepository employeeRepository,
                       AuditLogRepository auditLogRepository,
                       PasswordEncoderPort passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResult createUser(CreateUserCommand command, Long currentAdminId) {
        if (userRepository.existsByUsername(command.username())) {
            throw new DuplicateUsernameException("Tên đăng nhập đã tồn tại: " + command.username());
        }

        RoleCode roleCode = RoleCode.fromCode(command.roleCode());
        Role role = roleRepository.findByCode(roleCode)
                .orElseGet(() -> roleRepository.save(new Role(null, roleCode, roleCode.getName())));

        String encodedPassword = passwordEncoder.encode(command.password());
        User user = User.createNew(command.username(), encodedPassword, role, null);
        User savedUser = userRepository.save(user);

        // Link Employee profile
        Employee employee = Employee.createNew(savedUser.getId(), command.departmentId(), command.employeeCode(), command.fullName());
        Employee savedEmployee = employeeRepository.save(employee);

        savedUser.setEmployeeId(savedEmployee.getId());
        userRepository.save(savedUser);

        // Audit logging
        auditLogRepository.save(AuditLog.create(currentAdminId, "CREATE_USER", "users", savedUser.getId()));

        return mapToUserResult(savedUser, savedEmployee);
    }

    @Override
    public UserResult toggleUserStatus(Long userId, boolean lock, Long currentAdminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        long activeAdminCount = userRepository.countActiveAdmins();

        if (lock) {
            user.lock(currentAdminId, activeAdminCount);
            auditLogRepository.save(AuditLog.create(currentAdminId, "LOCK_USER", "users", user.getId()));
        } else {
            user.unlock();
            auditLogRepository.save(AuditLog.create(currentAdminId, "UNLOCK_USER", "users", user.getId()));
        }

        User updatedUser = userRepository.save(user);
        Employee employee = employeeRepository.findByUserId(updatedUser.getId()).orElse(null);
        return mapToUserResult(updatedUser, employee);
    }

    @Override
    public UserResult updateUserRole(UpdateUserRoleCommand command, Long currentAdminId) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + command.userId()));

        RoleCode newRoleCode = RoleCode.fromCode(command.roleCode());
        Role newRole = roleRepository.findByCode(newRoleCode)
                .orElseGet(() -> roleRepository.save(new Role(null, newRoleCode, newRoleCode.getName())));

        long activeAdminCount = userRepository.countActiveAdmins();
        user.changeRole(newRole, activeAdminCount);

        User updatedUser = userRepository.save(user);

        Employee employee = employeeRepository.findByUserId(updatedUser.getId()).orElse(null);
        if (employee != null && command.departmentId() != null) {
            employee.setDepartmentId(command.departmentId());
            employeeRepository.save(employee);
        }

        auditLogRepository.save(AuditLog.create(currentAdminId, "UPDATE_ROLE", "users", updatedUser.getId()));

        return mapToUserResult(updatedUser, employee);
    }

    @Override
    public PageResult<UserResult> getUsers(int page, int size) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = Math.min(Math.max(size, 1), 100); // MAX_PAGE_SIZE = 100

        List<User> users = userRepository.findAll(validatedPage, validatedSize);
        long totalElements = userRepository.count();

        // Batch Resolving: Avoid N+1 Query Problem by using WHERE user_id IN (...)
        List<Long> userIds = users.stream().map(User::getId).toList();
        Map<Long, Employee> employeeMap = employeeRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(Employee::getUserId, emp -> emp, (e1, e2) -> e1));

        List<UserResult> userResults = users.stream()
                .map(user -> mapToUserResult(user, employeeMap.get(user.getId())))
                .toList();

        return new PageResult<>(userResults, validatedPage, validatedSize, totalElements);
    }

    @Override
    public UserResult getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + id));
        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);
        return mapToUserResult(user, employee);
    }

    private UserResult mapToUserResult(User user, Employee employee) {
        return new UserResult(
                user.getId(),
                user.getUsername(),
                user.getRole().getCode().getCode(),
                user.getRole().getName(),
                user.getStatus(),
                employee != null ? employee.getId() : null,
                employee != null ? employee.getFullName() : null,
                employee != null ? employee.getDepartmentId() : null,
                employee != null ? "Department #" + employee.getDepartmentId() : null
        );
    }
}
