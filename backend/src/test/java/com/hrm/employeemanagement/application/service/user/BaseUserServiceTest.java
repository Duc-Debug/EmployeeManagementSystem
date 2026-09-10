package com.hrm.employeemanagement.application.service.user;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnit;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitId;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitStatus;
import com.hrm.employeemanagement.domain.orgunit.OrgUnitType;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
abstract class BaseUserServiceTest {

    protected static final Long ADMIN_ID = 1L;

    @Mock
    protected LoadUserPort loadUserPort;

    @Mock
    protected SaveUserPort saveUserPort;

    @Mock
    protected LoadRolePort loadRolePort;

    @Mock
    protected LoadEmployeePort loadEmployeePort;

    @Mock
    protected SaveEmployeePort saveEmployeePort;

    @Mock
    protected SaveAuditLogPort saveAuditLogPort;

    @Mock
    protected SaveAuditLogInNewTransactionPort deniedAuditLogPort;

    @Mock
    protected LoadOrgUnitPort loadOrgUnitPort;

    @Mock
    protected PasswordEncoderPort passwordEncoder;

    @Mock
    protected AuthorizationService authorizationService;

    protected UserService userService;

    protected Role adminRole;
    protected Role staffRole;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                loadUserPort,
                saveUserPort,
                loadRolePort,
                loadEmployeePort,
                saveEmployeePort,
                saveAuditLogPort,
                deniedAuditLogPort,
                loadOrgUnitPort,
                passwordEncoder,
                authorizationService
        );

        adminRole = new Role(
                new RoleId(1L),
                RoleCode.VT_06,
                "Quản trị hệ thống"
        );

        staffRole = new Role(
                new RoleId(4L),
                RoleCode.VT_04,
                "Nhân viên chuyên môn"
        );

        User currentAdmin = new User(
                new UserId(ADMIN_ID),
                "admin",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentAdmin.changeDataScope(
                DataScope.COMPANY,
                null
        );

        lenient().when(loadUserPort.findById(new UserId(ADMIN_ID)))
                .thenReturn(Optional.of(currentAdmin));
    }

    protected User testUser(Long id, Role role, Long empId) {
        return new User(
                new UserId(id),
                "user_" + id,
                "hash",
                role,
                UserStatus.ACTIVE,
                empId != null ? new EmployeeId(empId) : null
        );
    }

    protected Employee testEmployee(Long id, Long userId, Long orgUnitId, String code) {
        return new Employee(
                new EmployeeId(id),
                new UserId(userId),
                orgUnitId,
                code,
                "John Doe",
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    protected OrgUnit activeOrgUnit(
            Long id,
            String code,
            String name
    ) {
        return orgUnit(
                id,
                code,
                name,
                OrgUnitStatus.ACTIVE
        );
    }

    protected OrgUnit orgUnit(
            Long id,
            String code,
            String name,
            OrgUnitStatus status
    ) {
        return new OrgUnit(
                new OrgUnitId(id),
                code,
                name,
                OrgUnitType.DEPARTMENT,
                null,
                "/" + id + "/",
                1,
                status,
                null,
                null,
                null,
                null
        );
    }

    protected User currentUserWithScope(
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        User currentUser = new User(
                new UserId(ADMIN_ID),
                "admin",
                "hash",
                staffRole,
                UserStatus.ACTIVE,
                new EmployeeId(10L)
        );

        currentUser.changeDataScope(
                dataScope,
                scopeOrgUnitId
        );

        return currentUser;
    }
}
