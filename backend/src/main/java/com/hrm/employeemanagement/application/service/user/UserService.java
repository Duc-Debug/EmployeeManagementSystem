package com.hrm.employeemanagement.application.service.user;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.port.inbound.user.CreateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.GetUserListUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ToggleUserStatusUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.UpdateUserRoleUseCase;
import com.hrm.employeemanagement.application.port.outbound.orgunit.LoadOrgUnitPort;
import com.hrm.employeemanagement.application.port.outbound.security.PasswordEncoderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadRolePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveAuditLogPort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.SaveUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.orgunit.OrgUnitNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.DuplicateUsernameException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Pure Java Application Service orchestrating User Management Use Cases.
 * Resolves actual organization unit names semantically and optimizes batch queries for list retrieval.
 */
public class UserService implements
        CreateUserUseCase,
        ToggleUserStatusUseCase,
        UpdateUserRoleUseCase,
        GetUserListUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadRolePort loadRolePort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveEmployeePort saveEmployeePort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final LoadOrgUnitPort loadOrgUnitPort;
    private final PasswordEncoderPort passwordEncoder;
    private final AuthorizationService authorizationService;

    public UserService(
            LoadUserPort loadUserPort,
            SaveUserPort saveUserPort,
            LoadRolePort loadRolePort,
            LoadEmployeePort loadEmployeePort,
            SaveEmployeePort saveEmployeePort,
            SaveAuditLogPort saveAuditLogPort,
            LoadOrgUnitPort loadOrgUnitPort,
            PasswordEncoderPort passwordEncoder,
            AuthorizationService authorizationService
    ) {
        this.loadUserPort = loadUserPort;
        this.saveUserPort = saveUserPort;
        this.loadRolePort = loadRolePort;
        this.loadEmployeePort = loadEmployeePort;
        this.saveEmployeePort = saveEmployeePort;
        this.saveAuditLogPort = saveAuditLogPort;
        this.loadOrgUnitPort = loadOrgUnitPort;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = Objects.requireNonNull(
        authorizationService,
        "AuthorizationService must not be null"
);
    }

    @Override
    public UserResult createUser(CreateUserCommand command) {
            Long currentAdminId = authorizationService.require(
                PermissionCode.USER_CREATE
        );
        if (loadUserPort.existsByUsername(command.username())) {
            throw new DuplicateUsernameException(
                    "Tên đăng nhập '" + command.username() + "' đã tồn tại trong hệ thống"
            );
        }
       

        OrgUnit orgUnit = loadActiveOrgUnitOrThrow(command.orgUnitId());

        RoleCode roleCode = RoleCode.fromCode(command.roleCode());

        Role role = loadRolePort.findByCode(roleCode)
                .orElseGet(() ->
                        loadRolePort.save(
                                new Role(null, roleCode, roleCode.getName())
                        )
                );

        String passwordHash = passwordEncoder.encode(command.password());

        User newUser = User.createNew(
                command.username(),
                passwordHash,
                role,
                null
        );

        User savedUser = saveUserPort.save(newUser);

        // Link Employee profile
        Employee employee = Employee.createNew(
                savedUser.getId(),
                command.orgUnitId(),
                command.employeeCode(),
                command.fullName()
        );

        Employee savedEmployee = saveEmployeePort.save(employee);

        savedUser.linkEmployee(savedEmployee.getId());

        // Audit logging
        saveAuditLogPort.save(
                AuditLog.create(
                        currentAdminId,
                        "CREATE_USER",
                        "users",
                        savedUser.getIdValue()
                )
        );

        String orgUnitName = orgUnit != null
                ? orgUnit.getUnitName()
                : null;

        return mapToUserResult(
                savedUser,
                savedEmployee,
                orgUnitName
        );
    }

    @Override
    public UserResult toggleUserStatus(
            Long userId,
            boolean lock
    ) {
          Long currentAdminId = authorizationService.require(
                PermissionCode.USER_TOGGLE_STATUS
        );

        UserId uId = new UserId(userId);

        User user = loadUserPort.findById(uId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Không tìm thấy người dùng với ID: " + userId
                        )
                );

        // Acquire pessimistic lock on Admin role row to serialize
        // concurrent transactions affecting active admin count
        if (lock && user.isSystemAdmin()) {
            loadRolePort.lockRoleForUpdate(RoleCode.VT_06);
        }

        long activeAdminCount = loadUserPort.countActiveAdmins();

        UserId adminId = currentAdminId != null
                ? new UserId(currentAdminId)
                : null;

        if (lock) {
            user.lock(adminId, activeAdminCount);

            saveAuditLogPort.save(
                    AuditLog.create(
                            currentAdminId,
                            "LOCK_USER",
                            "users",
                            user.getIdValue()
                    )
            );
        } else {
            user.unlock();

            saveAuditLogPort.save(
                    AuditLog.create(
                            currentAdminId,
                            "UNLOCK_USER",
                            "users",
                            user.getIdValue()
                    )
            );
        }

        User updatedUser = saveUserPort.save(user);

        Employee employee = loadEmployeePort
                .findByUserId(updatedUser.getId())
                .orElse(null);

        String orgUnitName = resolveOrgUnitName(employee);

        return mapToUserResult(
                updatedUser,
                employee,
                orgUnitName
        );
    }

   @Override
public UserResult updateUserRole(
        UpdateUserRoleCommand command
) {
    Long currentAdminId = authorizationService.require(
            PermissionCode.USER_UPDATE_ROLE
    );

    UserId uId = new UserId(command.userId());

    User user = loadUserPort.findById(uId)
            .orElseThrow(() ->
                    new UserNotFoundException(
                            "Không tìm thấy người dùng với ID: "
                                    + command.userId()
                    )
            );

    RoleCode newRoleCode =
            RoleCode.fromCode(command.roleCode());

    Role newRole = loadRolePort.findByCode(newRoleCode)
            .orElseGet(() ->
                    loadRolePort.save(
                            new Role(
                                    null,
                                    newRoleCode,
                                    newRoleCode.getName()
                            )
                    )
            );

    // Serialize concurrent operations when demoting an admin.
    if (user.isSystemAdmin()
            && !newRole.isSystemAdmin()) {
        loadRolePort.lockRoleForUpdate(
                RoleCode.VT_06
        );
    }

    Employee employee = loadEmployeePort
            .findByUserId(user.getId())
            .orElse(null);

    /*
     * OrgUnit mà Employee thực sự thuộc về.
     */
    OrgUnit employeeOrgUnit =
            loadActiveOrgUnitOrThrow(
                    command.orgUnitId()
            );

    /*
     * OrgUnit gốc của phạm vi dữ liệu.
     *
     * Chỉ ORGANIZATION_BRANCH mới cần scopeOrgUnitId.
     */
    if (command.dataScope()
            == DataScope.ORGANIZATION_BRANCH) {

        loadActiveOrgUnitOrThrow(
                command.scopeOrgUnitId()
        );
    }

    long activeAdminCount =
            loadUserPort.countActiveAdmins();

    /*
     * Thay đổi quyền chức năng.
     */
    user.changeRole(
            newRole,
            activeAdminCount
    );

    /*
     * Thay đổi phạm vi dữ liệu.
     *
     * Domain sẽ tự reject:
     * ORGANIZATION_BRANCH + null
     * SELF + orgUnitId
     * COMPANY + orgUnitId
     */
    user.changeDataScope(
            command.dataScope(),
            command.scopeOrgUnitId()
    );

    /*
     * Role + DataScope đã hoàn chỉnh rồi mới persist User.
     */
    User updatedUser =
            saveUserPort.save(user);

    /*
     * Nếu admin đồng thời thay đổi đơn vị làm việc của Employee.
     */
    if (employee != null
            && command.orgUnitId() != null) {

        employee.assignToOrgUnit(
                command.orgUnitId()
        );

        saveEmployeePort.save(employee);
    }

    saveAuditLogPort.save(
            AuditLog.create(
                    currentAdminId,
                    "UPDATE_ROLE",
                    "users",
                    updatedUser.getIdValue()
            )
    );

    String orgUnitName =
            employee != null
                    && employeeOrgUnit != null
                    ? employeeOrgUnit.getUnitName()
                    : resolveOrgUnitName(employee);

    return mapToUserResult(
            updatedUser,
            employee,
            orgUnitName
    );
}

@Override
    public PageResult<UserResult> getUsers(int page, int size) {
        Long currentUserId = authorizationService.require(
                PermissionCode.USER_READ
        );

        User currentUser = loadUserPort
                .findById(new UserId(currentUserId))
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Không tìm thấy người dùng hiện tại với ID: "
                                        + currentUserId
                        )
                );

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        List<User> users;
        long totalElements;

        switch (currentUser.getDataScope()) {
            case COMPANY -> {
                users = loadUserPort.findAll(safePage, safeSize);
                totalElements = loadUserPort.count();
            }
            case ORGANIZATION_BRANCH -> {
                Long scopeOrgUnitId = currentUser.getScopeOrgUnitId();
                users = loadUserPort.findByOrgUnitBranch(
                        scopeOrgUnitId,
                        safePage,
                        safeSize
                );
                totalElements = loadUserPort.countByOrgUnitBranch(
                        scopeOrgUnitId
                );
            }
            case SELF -> {
                totalElements = 1L;
                users = safePage == 0
                        ? List.of(currentUser)
                        : List.of();
            }
            default -> throw new IllegalStateException(
                    "Unsupported DataScope: "
                            + currentUser.getDataScope()
            );
        }

        List<UserId> userIds = users.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .toList();

        List<Employee> employees =
                loadEmployeePort.findAllByUserIdIn(userIds);

        Map<Long, Employee> employeeMap = employees.stream()
                .filter(e -> e.getUserIdValue() != null)
                .collect(
                        Collectors.toMap(
                                Employee::getUserIdValue,
                                e -> e,
                                (existing, replacing) -> existing
                        )
                );

        // Batch load all distinct organization unit names in one query
        List<Long> orgUnitIds = employees.stream()
                .map(Employee::getOrgUnitId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> orgUnitNameMap = orgUnitIds.isEmpty()
                ? Map.of()
                : loadOrgUnitPort.findAllByIdIn(orgUnitIds)
                .stream()
                .collect(
                        Collectors.toMap(
                                orgUnit -> orgUnit.getId().getValue(),
                                OrgUnit::getUnitName,
                                (existing, replacing) -> existing
                        )
                );

        List<UserResult> content = users.stream()
                .map(user -> {
                    Employee employee =
                            employeeMap.get(user.getIdValue());

                    String orgUnitName =
                            employee != null
                                    && employee.getOrgUnitId() != null
                                    ? orgUnitNameMap.get(
                                            employee.getOrgUnitId()
                                    )
                                    : null;

                    return mapToUserResult(
                            user,
                            employee,
                            orgUnitName
                    );
                })
                .toList();

        return new PageResult<>(
                content,
                safePage,
                safeSize,
                totalElements
        );
    }

    @Override
    public UserResult getUserById(Long userId) {
         authorizationService.require(
                PermissionCode.USER_READ
        );

        UserId uId = new UserId(userId);

        User user = loadUserPort.findById(uId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Không tìm thấy người dùng với ID: " + userId
                        )
                );

        Employee employee = loadEmployeePort
                .findByUserId(user.getId())
                .orElse(null);

        String orgUnitName = resolveOrgUnitName(employee);

        return mapToUserResult(
                user,
                employee,
                orgUnitName
        );
    }

    private OrgUnit loadActiveOrgUnitOrThrow(Long orgUnitId) {
        if (orgUnitId == null) {
            return null;
        }

        OrgUnit orgUnit = loadOrgUnitPort.findById(new OrgUnitId(orgUnitId))
                .orElseThrow(() ->
                        new OrgUnitNotFoundException("Organizational unit not found with ID: " + orgUnitId)
                );

        if (orgUnit.getStatus() != OrgUnitStatus.ACTIVE) {
            throw new IllegalArgumentException("Đơn vị tổ chức được chỉ định đang không hoạt động");
        }

        return orgUnit;
    }

    private String resolveOrgUnitName(Employee employee) {
        if (employee == null || employee.getOrgUnitId() == null) {
            return null;
        }

        return loadOrgUnitPort
                .findById(
                        new OrgUnitId(
                                employee.getOrgUnitId()
                        )
                )
                .map(OrgUnit::getUnitName)
                .orElse(null);
    }

    private UserResult mapToUserResult(
            User user,
            Employee employee,
            String orgUnitName
    ) {
        return new UserResult(
                user.getIdValue(),
                user.getUsername(),
                user.getRole().getCode().getCode(),
                user.getRole().getName(),
                user.getStatus(),
                employee != null
                        ? employee.getIdValue()
                        : null,
                employee != null
                        ? employee.getFullName()
                        : null,
                employee != null
                        ? employee.getOrgUnitId()
                        : null,
                orgUnitName
        );
    }
}
