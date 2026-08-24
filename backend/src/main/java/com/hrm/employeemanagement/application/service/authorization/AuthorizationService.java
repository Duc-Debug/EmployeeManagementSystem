package com.hrm.employeemanagement.application.service.authorization;

import java.util.Objects;

import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.PermissionQueryPort;
import com.hrm.employeemanagement.application.port.outbound.audit.SaveAuditLogInNewTransactionPort;
import com.hrm.employeemanagement.domain.audit.AuditLog;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.user.User;

public class AuthorizationService {

    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final PermissionQueryPort permissionQueryPort;
    private final SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    public AuthorizationService(
            GetAuthenticatedUserPort authenticatedUserPort,
            PermissionQueryPort permissionQueryPort,
            SaveAuditLogInNewTransactionPort deniedAuditLogPort
    ) {
        this.authenticatedUserPort = Objects.requireNonNull(
                authenticatedUserPort,
                "GetAuthenticatedUserPort must not be null"
        );

        this.permissionQueryPort = Objects.requireNonNull(
                permissionQueryPort,
                "PermissionQueryPort must not be null"
        );

        this.deniedAuditLogPort = Objects.requireNonNull(
                deniedAuditLogPort,
                "SaveAuditLogInNewTransactionPort must not be null"
        );
    }

    public Long require(PermissionCode permission) {
        Objects.requireNonNull(
                permission,
                "PermissionCode must not be null"
        );

        User currentUser =
                authenticatedUserPort.getAuthenticatedUser();

        if (currentUser == null) {
            throw new IllegalStateException(
                    "Không tìm thấy người dùng đã xác thực"
            );
        }

        Long currentUserId = currentUser.getIdValue();

        if (!permissionQueryPort.hasPermission(
                currentUserId,
                permission
        )) {
            deniedAuditLogPort.save(
                    AuditLog.createChange(
                            currentUserId,
                            "PERMISSION_DENIED",
                            "permissions",
                            null,
                            null,
                            "permission=" + permission.name()
                                    + ";reason=MISSING_PERMISSION"
                    )
            );

            throw new PermissionDeniedException(permission);
        }

        return currentUserId;
    }
}
