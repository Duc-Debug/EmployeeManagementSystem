package com.hrm.employeemanagement.application.service.authorization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.PermissionQueryPort;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private GetAuthenticatedUserPort authenticatedUserPort;

    @Mock
    private PermissionQueryPort permissionQueryPort;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(
                authenticatedUserPort,
                permissionQueryPort
        );
    }

    @Test
    @DisplayName("Quyền bị revoke có hiệu lực ở request kế tiếp dù principal vẫn là snapshot cũ")
    void testRequire_RevokedPermissionOnNextRequest_ThrowsPermissionDeniedException() {
        User stalePrincipal = new User(
                new UserId(10L),
                "project_manager",
                "hash",
                new Role(
                        new RoleId(2L),
                        RoleCode.VT_02,
                        "Quản lý dự án"
                ),
                UserStatus.ACTIVE,
                new EmployeeId(100L)
        );

        when(authenticatedUserPort.getAuthenticatedUser())
                .thenReturn(stalePrincipal);

        when(permissionQueryPort.hasPermission(
                10L,
                PermissionCode.USER_READ
        )).thenReturn(
                true,
                false
        );

        assertEquals(
                10L,
                authorizationService.require(PermissionCode.USER_READ)
        );

        assertThrows(
                PermissionDeniedException.class,
                () -> authorizationService.require(PermissionCode.USER_READ)
        );

        verify(authenticatedUserPort, times(2))
                .getAuthenticatedUser();

        verify(permissionQueryPort, times(2))
                .hasPermission(
                        10L,
                        PermissionCode.USER_READ
                );
    }
}
