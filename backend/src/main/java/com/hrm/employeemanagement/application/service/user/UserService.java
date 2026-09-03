package com.hrm.employeemanagement.application.service.user;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.user.CreateUserCommand;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.dto.user.RoleResult;
import com.hrm.employeemanagement.application.dto.user.UpdateUserRoleCommand;
import com.hrm.employeemanagement.application.dto.user.UserResult;
import com.hrm.employeemanagement.application.port.inbound.user.CreateUserUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.GetCurrentUserProfileUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.GetRoleListUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.GetUserListUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.ToggleUserStatusUseCase;
import com.hrm.employeemanagement.application.port.inbound.user.UpdateUserRoleUseCase;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
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
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
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
        GetUserListUseCase,
        GetRoleListUseCase,
        GetCurrentUserProfileUseCase {

    private final LoadUserPort loadUserPort;
    private final SaveUserPort saveUserPort;
    private final LoadRolePort loadRolePort;
    private final LoadEmployeePort loadEmployeePort;
    private final SaveEmployeePort saveEmployeePort;
    private final SaveAuditLogPort saveAuditLogPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;
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
            SaveAuditLogInNewTransactionPort deniedAuditLogPort,
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
        this.deniedAuditLogPort = Objects.requireNonNull(
                deniedAuditLogPort,
                "SaveAuditLogInNewTransactionPort must not be null"
        );
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

        User currentUser =
                loadCurrentUserOrThrow(currentAdminId);

        requireOrgUnitInDataScope(
                currentUser,
                command.orgUnitId(),
                PermissionCode.USER_CREATE
        );

        OrgUnit orgUnit =
                loadActiveOrgUnitOrThrow(command.orgUnitId());

        if (loadUserPort.existsByUsername(command.username())) {
            throw new DuplicateUsernameException(
                    "Tên đăng nhập '" + command.username() + "' đã tồn tại trong hệ thống"
            );
        }

        if (loadUserPort.existsByEmail(command.username())) {
            throw new DuplicateUsernameException(
                    "Tên đăng nhập xung đột với email khôi phục của một tài khoản khác"
            );
        }

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
                null,
                command.email()
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

        User currentUser =
                loadCurrentUserOrThrow(currentAdminId);

        requireUserInDataScope(
                currentAdminId,
                currentUser,
                userId,
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

    User currentUser =
            loadCurrentUserOrThrow(currentAdminId);

    requireUserInDataScope(
            currentAdminId,
            currentUser,
            command.userId(),
            PermissionCode.USER_UPDATE_ROLE
    );

    requireAssignableDataScope(
            currentUser,
            command.dataScope(),
            command.scopeOrgUnitId(),
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

    String oldRoleCode =
            user.getRole().getCode().getCode();

    DataScope oldDataScope =
            user.getDataScope();

    Long oldScopeOrgUnitId =
            user.getScopeOrgUnitId();

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
     * Thay đổi role + dataScope như một authorization update duy nhất để
     * domain không rơi vào trạng thái tạm thời sai invariant.
     */
    user.changeAuthorization(
            newRole,
            command.dataScope(),
            command.scopeOrgUnitId(),
            activeAdminCount
    );

    /*
     * Role + DataScope đã hoàn chỉnh rồi mới persist User.
     */
    User updatedUser =
            saveUserPort.save(user);

    saveAuditLogPort.save(
            AuditLog.createChange(
                    currentAdminId,
                    "UPDATE_AUTHORIZATION",
                    "users",
                    updatedUser.getIdValue(),
                    authorizationAuditValue(
                            oldRoleCode,
                            oldDataScope,
                            oldScopeOrgUnitId
                    ),
                    authorizationAuditValue(
                            newRole.getCode().getCode(),
                            command.dataScope(),
                            command.scopeOrgUnitId()
                    )
            )
    );

    String orgUnitName =
            resolveOrgUnitName(employee);

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
        Long currentUserId = authorizationService.require(
                PermissionCode.USER_READ
        );

        User currentUser =
                loadCurrentUserOrThrow(currentUserId);

        requireUserInDataScope(
                currentUserId,
                currentUser,
                userId,
                PermissionCode.USER_READ
        );

        UserId targetUserId = new UserId(userId);

        User user = loadUserPort.findById(targetUserId)
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

    @Override
    public UserResult getCurrentUserProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        User user = loadUserPort.findById(new UserId(userId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        Employee employee = loadEmployeePort.findByUserId(user.getId()).orElse(null);
        String orgUnitName = resolveOrgUnitName(employee);

        return mapToUserResult(user, employee, orgUnitName);
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

    private User loadCurrentUserOrThrow(Long currentUserId) {
        return loadUserPort
                .findById(new UserId(currentUserId))
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Không tìm thấy người dùng hiện tại với ID: "
                                        + currentUserId
                        )
                );
    }

    private void requireUserInDataScope(
            Long currentUserId,
            User currentUser,
            Long targetUserId,
            PermissionCode permission
    ) {
        boolean allowed = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> currentUserId.equals(targetUserId);
            case ORGANIZATION_BRANCH ->
                    loadUserPort.existsInOrgUnitBranch(
                            targetUserId,
                            currentUser.getScopeOrgUnitId()
                    );
        };

        if (!allowed) {
            saveDeniedAudit(
                    currentUserId,
                    "USER_ACCESS_DENIED",
                    "users",
                    targetUserId,
                    permission,
                    deniedAuditDetails(
                            permission,
                            currentUser,
                            "OUTSIDE_DATA_SCOPE"
                    )
            );

            throw new PermissionDeniedException(permission);
        }
    }

    private void requireOrgUnitInDataScope(
            User currentUser,
            Long orgUnitId,
            PermissionCode permission
    ) {
        if (!isOrgUnitInDataScope(
                currentUser,
                orgUnitId
        )) {
            saveDeniedAudit(
                    currentUser.getIdValue(),
                    "ORG_UNIT_ACCESS_DENIED",
                    "org_units",
                    orgUnitId,
                    permission,
                    deniedAuditDetails(
                            permission,
                            currentUser,
                            "OUTSIDE_DATA_SCOPE"
                    )
            );

            throw new PermissionDeniedException(permission);
        }
    }

    private boolean isOrgUnitInDataScope(
            User currentUser,
            Long orgUnitId
    ) {
        if (orgUnitId == null) {
            return false;
        }

        return switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> false;
            case ORGANIZATION_BRANCH ->
                    loadOrgUnitPort.existsInOrgUnitBranch(
                            orgUnitId,
                            currentUser.getScopeOrgUnitId()
                    );
        };
    }

    private void requireAssignableDataScope(
            User currentUser,
            DataScope targetDataScope,
            Long targetScopeOrgUnitId,
            PermissionCode permission
    ) {
        boolean allowed = switch (currentUser.getDataScope()) {
            case COMPANY -> true;
            case SELF -> targetDataScope == DataScope.SELF
                    && targetScopeOrgUnitId == null;
            case ORGANIZATION_BRANCH -> targetDataScope == DataScope.SELF
                    || targetDataScope == DataScope.ORGANIZATION_BRANCH
                    && isOrgUnitInDataScope(
                            currentUser,
                            targetScopeOrgUnitId
                    );
        };

        if (!allowed) {
            saveDeniedAudit(
                    currentUser.getIdValue(),
                    "DATA_SCOPE_ASSIGN_DENIED",
                    "users",
                    currentUser.getIdValue(),
                    permission,
                    deniedAuditDetails(
                            permission,
                            currentUser,
                            "UNASSIGNABLE_DATA_SCOPE"
                    )
                            + ";targetDataScope=" + targetDataScope
                            + ";targetScopeOrgUnitId=" + targetScopeOrgUnitId
            );

            throw new PermissionDeniedException(permission);
        }
    }

    private void saveDeniedAudit(
            Long actorUserId,
            String action,
            String tableName,
            Long recordId,
            PermissionCode permission,
            String details
    ) {
        deniedAuditLogPort.save(
                AuditLog.createChange(
                        actorUserId,
                        action,
                        tableName,
                        recordId,
                        null,
                        details != null
                                ? details
                                : "permission=" + permission.name()
                                + ";reason=DENIED"
                )
        );
    }

    private String deniedAuditDetails(
            PermissionCode permission,
            User currentUser,
            String reason
    ) {
        return "permission=" + permission.name()
                + ";dataScope=" + currentUser.getDataScope()
                + ";scopeOrgUnitId=" + currentUser.getScopeOrgUnitId()
                + ";reason=" + reason;
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

    @Override
    public List<RoleResult> getRoles() {
        authorizationService.require(PermissionCode.USER_READ);
        return loadRolePort.findAll().stream()
                .map(role -> new RoleResult(
                        role.getId() != null ? role.getId().value() : null,
                        role.getCode() != null ? role.getCode().getCode() : null,
                        role.getName(),
                        null
                ))
                .toList();
    }

    private String authorizationAuditValue(
            String roleCode,
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        return "role=" + roleCode
                + ";dataScope=" + dataScope
                + ";scopeOrgUnitId=" + scopeOrgUnitId;
    }

    private UserResult mapToUserResult(
            User user,
            Employee employee,
            String orgUnitName
    ) {
        return new UserResult(
                user.getIdValue(),
                user.getUsername(),
                user.getEmail(),
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
                orgUnitName,
                user.getDataScope(),
                user.getScopeOrgUnitId()
        );
    }
}
