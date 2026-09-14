package com.hrm.employeemanagement.application.service.allocation;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.hrm.employeemanagement.application.dto.allocation.AllocationNotificationItemResult;
import com.hrm.employeemanagement.application.dto.allocation.AllocationNotificationPageResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetAllocationNotificationsUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadAllocationNotificationPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.project.ProjectNotFoundException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.notification.Notification;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Application service lấy danh sách thông báo khi phân bổ thay đổi (NCL-07-CN-003, BR-05, AC-03).
 * Enforce RBAC:
 * - VT-03 (RM): Chỉ xem trong phạm vi bộ phận (ORGANIZATION_BRANCH / COMPANY).
 * - VT-02 (PM): Chỉ xem thông báo của dự án mà mình làm quản lý (SELF).
 * - Các vai trò khác (VT-01, VT-04, VT-05, VT-06): Bị từ chối truy cập (HTTP 403) và ghi log ACCESS_DENIED.
 */
public class GetAllocationNotificationsService implements GetAllocationNotificationsUseCase {

    private final AuthorizationService authorizationService;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final LoadProjectPort loadProjectPort;
    private final LoadAllocationNotificationPort loadAllocationNotificationPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    public GetAllocationNotificationsService(
            AuthorizationService authorizationService,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            LoadProjectPort loadProjectPort,
            LoadAllocationNotificationPort loadAllocationNotificationPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort
    ) {
        this.authorizationService = Objects.requireNonNull(authorizationService, "AuthorizationService must not be null");
        this.loadUserPort = Objects.requireNonNull(loadUserPort, "LoadUserPort must not be null");
        this.loadEmployeePort = Objects.requireNonNull(loadEmployeePort, "LoadEmployeePort must not be null");
        this.loadProjectPort = Objects.requireNonNull(loadProjectPort, "LoadProjectPort must not be null");
        this.loadAllocationNotificationPort = Objects.requireNonNull(loadAllocationNotificationPort, "LoadAllocationNotificationPort must not be null");
        this.deniedAuditLogPort = Objects.requireNonNull(deniedAuditLogPort, "SaveAuditLogInNewTransactionPort must not be null");
    }

    @Override
    public AllocationNotificationPageResult getAllocationNotifications(Long projectId, int page, int size) {
        Long currentUserId = authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        User currentUser = loadUserPort.findById(new UserId(currentUserId))
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));

        RoleCode roleCode = currentUser.getRole().getCode();

        // BR-05 / AC-03: Chỉ VT-02 và VT-03 được phép mở danh sách thông báo phân bổ
        if (roleCode != RoleCode.VT_02 && roleCode != RoleCode.VT_03) {
            deniedAuditLogPort.save(AuditLog.createChange(
                    currentUserId,
                    "ACCESS_DENIED_ALLOCATION_NOTIFICATIONS",
                    "notifications",
                    projectId,
                    null,
                    "user_id=" + currentUserId + ";role=" + roleCode.getCode() + ";denied_reason=ROLE_NOT_AUTHORIZED"
            ));
            throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
        }

        List<Long> allowedProjectIds = Collections.emptyList();

        if (roleCode == RoleCode.VT_02) {
            // PM: Chỉ được xem dự án mình quản lý (SELF)
            if (currentUser.getEmployeeId() == null) {
                deniedAuditLogPort.save(AuditLog.createChange(
                        currentUserId,
                        "ACCESS_DENIED_ALLOCATION_NOTIFICATIONS",
                        "notifications",
                        projectId,
                        null,
                        "user_id=" + currentUserId + ";role=VT-02;denied_reason=NO_LINKED_EMPLOYEE"
                ));
                throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
            }

            Long pmEmployeeId = currentUser.getEmployeeId().value();

            if (projectId != null) {
                boolean isManager = loadProjectPort.existsManagedBy(projectId, pmEmployeeId);
                if (!isManager) {
                    deniedAuditLogPort.save(AuditLog.createChange(
                            currentUserId,
                            "ACCESS_DENIED_ALLOCATION_NOTIFICATIONS",
                            "notifications",
                            projectId,
                            null,
                            "user_id=" + currentUserId + ";role=VT-02;denied_reason=PROJECT_NOT_MANAGED;projectId=" + projectId
                    ));
                    throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
                }
                allowedProjectIds = List.of(projectId);
            } else {
                allowedProjectIds = loadProjectPort.findAllManagedProjectIds(pmEmployeeId);
                if (allowedProjectIds.isEmpty()) {
                    return new AllocationNotificationPageResult(Collections.emptyList(), 0, 0, page, size);
                }
            }
        } else {
            // VT-03: RM - DataScope theo đơn vị / nhánh
            if (projectId != null) {
                Project targetProject = loadProjectPort.findById(new ProjectId(projectId))
                        .orElseThrow(() -> new ProjectNotFoundException("Không tìm thấy dự án với ID: " + projectId));

                boolean inScope = switch (currentUser.getDataScope()) {
                    case COMPANY -> true;
                    case ORGANIZATION_BRANCH -> currentUser.getScopeOrgUnitId() != null
                            && loadProjectPort.existsInOrgUnitBranch(projectId, currentUser.getScopeOrgUnitId());
                    case SELF -> false;
                };

                if (!inScope) {
                    deniedAuditLogPort.save(AuditLog.createChange(
                            currentUserId,
                            "ACCESS_DENIED_ALLOCATION_NOTIFICATIONS",
                            "notifications",
                            projectId,
                            null,
                            "user_id=" + currentUserId + ";role=VT-03;denied_reason=OUT_OF_DATA_SCOPE;projectId=" + projectId
                    ));
                    throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
                }
                allowedProjectIds = List.of(projectId);
            } else {
                // Toàn bộ dự án trong scope
                switch (currentUser.getDataScope()) {
                    case COMPANY -> allowedProjectIds = Collections.emptyList(); // rỗng = không giới hạn projectId
                    case ORGANIZATION_BRANCH -> {
                        if (currentUser.getScopeOrgUnitId() == null) {
                            return new AllocationNotificationPageResult(Collections.emptyList(), 0, 0, page, size);
                        }
                        allowedProjectIds = loadProjectPort.findAllProjectIdsByOrgUnitBranch(currentUser.getScopeOrgUnitId());
                        if (allowedProjectIds.isEmpty()) {
                            return new AllocationNotificationPageResult(Collections.emptyList(), 0, 0, page, size);
                        }
                    }
                    case SELF -> {
                        deniedAuditLogPort.save(AuditLog.createChange(
                                currentUserId,
                                "ACCESS_DENIED_ALLOCATION_NOTIFICATIONS",
                                "notifications",
                                null,
                                null,
                                "user_id=" + currentUserId + ";role=VT-03;denied_reason=INVALID_DATA_SCOPE_SELF"
                        ));
                        throw new PermissionDeniedException(PermissionCode.RESOURCE_ALLOCATION_READ);
                    }
                }
            }
        }

        Long recipientUserId = (roleCode == RoleCode.VT_02) ? currentUserId : null;
        List<Notification> notifications = loadAllocationNotificationPort.findAllocationNotifications(recipientUserId, allowedProjectIds, page, size);
        long totalElements = loadAllocationNotificationPort.countAllocationNotifications(recipientUserId, allowedProjectIds);
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        // Load sender and recipient user information for user-friendly display
        List<UserId> allUserIds = notifications.stream()
                .flatMap(n -> java.util.stream.Stream.of(n.getRecipientId(), n.getSenderId()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> usersMap = loadUserPort.findAllByIdIn(allUserIds).stream()
                .collect(Collectors.toMap(u -> u.getId().value(), Function.identity(), (a, b) -> a));

        List<EmployeeId> employeeIds = usersMap.values().stream()
                .map(User::getEmployeeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Employee> employeesMap = loadEmployeePort.findAllByIdIn(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getIdValue, Function.identity(), (a, b) -> a));

        List<AllocationNotificationItemResult> items = notifications.stream().map(n -> {
            String recipientName = resolveDisplayName(n.getRecipientId(), usersMap, employeesMap);
            String senderName = resolveDisplayName(n.getSenderId(), usersMap, employeesMap);

            return new AllocationNotificationItemResult(
                    n.getId() != null ? n.getId().value() : null,
                    n.getRecipientId().value(),
                    recipientName,
                    n.getSenderId() != null ? n.getSenderId().value() : null,
                    senderName,
                    n.getType().name(),
                    n.getTargetType(),
                    n.getTargetId(),
                    n.getTitle(),
                    n.getContent(),
                    n.isRead(),
                    n.getCreatedAt()
            );
        }).toList();

        return new AllocationNotificationPageResult(items, totalElements, totalPages, page, size);
    }

    private String resolveDisplayName(UserId userId, Map<Long, User> usersMap, Map<Long, Employee> employeesMap) {
        if (userId == null) {
            return "Hệ thống";
        }
        User user = usersMap.get(userId.value());
        if (user == null) {
            return "ID:" + userId.value();
        }
        if (user.getEmployeeId() != null) {
            Employee emp = employeesMap.get(user.getEmployeeId().value());
            if (emp != null && emp.getFullName() != null && !emp.getFullName().isBlank()) {
                return emp.getFullName();
            }
        }
        return user.getUsername();
    }
}
