package com.hrm.employeemanagement.application.service.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.employee.EmployeeStatus;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.project.ProjectStatus;
import com.hrm.employeemanagement.domain.role.Role;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.role.RoleId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;
import com.hrm.employeemanagement.domain.user.UserStatus;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private static final Long CURRENT_USER_ID = 10L;
    private static final Long CURRENT_EMPLOYEE_ID = 100L;

    @Mock
    private LoadProjectPort loadProjectPort;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private LoadEmployeePort loadEmployeePort;

    @Mock
    private AuthorizationService authorizationService;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(
                loadProjectPort,
                loadUserPort,
                loadEmployeePort,
                authorizationService
        );
    }

    @Test
    @DisplayName("PROJECT_READ + COMPANY tra ve toan bo project")
    void testGetProjects_CompanyScope_ReturnsAllProjects() {
        User currentUser =
                currentUser(
                        RoleCode.VT_06,
                        DataScope.COMPANY,
                        null
                );

        when(authorizationService.require(PermissionCode.PROJECT_READ))
                .thenReturn(CURRENT_USER_ID);

        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(currentUser));

        Project p2 = project(
                2L,
                "P-02",
                5L,
                101L
        );

        Project p1 = project(
                1L,
                "P-01",
                6L,
                102L
        );

        when(loadProjectPort.findAll(0, 20))
                .thenReturn(List.of(p2, p1));

        when(loadProjectPort.count())
                .thenReturn(2L);

        PageResult<ProjectResult> result =
                projectService.getProjects(0, 20);

        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getTotalElements());
        assertEquals(
                List.of(2L, 1L),
                result.getContent()
                        .stream()
                        .map(ProjectResult::getId)
                        .toList()
        );

        verify(loadProjectPort)
                .findAll(0, 20);

        verify(loadProjectPort)
                .count();
    }

    @Test
    @DisplayName("PROJECT_READ + ORGANIZATION_BRANCH chi tra project thuoc nhanh")
    void testGetProjects_OrganizationBranchScope_ReturnsBranchProjects() {
        User currentUser =
                currentUser(
                        RoleCode.VT_02,
                        DataScope.ORGANIZATION_BRANCH,
                        5L
                );

        when(authorizationService.require(PermissionCode.PROJECT_READ))
                .thenReturn(CURRENT_USER_ID);

        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(currentUser));

        Project projectA = project(
                12L,
                "P-A",
                5L,
                CURRENT_EMPLOYEE_ID
        );

        Project projectB = project(
                11L,
                "P-B",
                8L,
                CURRENT_EMPLOYEE_ID
        );

        when(loadProjectPort.findByOrgUnitBranch(5L, 1, 2))
                .thenReturn(List.of(projectA, projectB));

        when(loadProjectPort.countByOrgUnitBranch(5L))
                .thenReturn(5L);

        PageResult<ProjectResult> result =
                projectService.getProjects(1, 2);

        assertEquals(
                List.of(12L, 11L),
                result.getContent()
                        .stream()
                        .map(ProjectResult::getId)
                        .toList()
        );

        assertEquals(1, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(5L, result.getTotalElements());
        assertEquals(3, result.getTotalPages());

        verify(loadProjectPort)
                .findByOrgUnitBranch(5L, 1, 2);

        verify(loadProjectPort)
                .countByOrgUnitBranch(5L);

        verify(loadProjectPort, never())
                .findAll(
                        any(Integer.class),
                        any(Integer.class)
                );
    }

    @Test
    @DisplayName("SELF + VT-02 chi tra project do employee hien tai quan ly")
    void testGetProjects_SelfProjectManager_ReturnsManagedProjects() {
        User currentUser =
                currentUser(
                        RoleCode.VT_02,
                        DataScope.SELF,
                        null
                );

        when(authorizationService.require(PermissionCode.PROJECT_READ))
                .thenReturn(CURRENT_USER_ID);

        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(currentEmployee()));

        Project managedProject =
                project(
                        20L,
                        "PM-20",
                        5L,
                        CURRENT_EMPLOYEE_ID
                );

        when(loadProjectPort.findManagedBy(CURRENT_EMPLOYEE_ID, 0, 20))
                .thenReturn(List.of(managedProject));

        when(loadProjectPort.countManagedBy(CURRENT_EMPLOYEE_ID))
                .thenReturn(1L);

        PageResult<ProjectResult> result =
                projectService.getProjects(0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(20L, result.getContent().get(0).getId());

        verify(loadProjectPort)
                .findManagedBy(CURRENT_EMPLOYEE_ID, 0, 20);

        verify(loadProjectPort, never())
                .findMemberProjects(
                        any(Long.class),
                        any(Integer.class),
                        any(Integer.class)
                );
    }

    @Test
    @DisplayName("SELF + VT-04 chi tra project ma employee hien tai la member")
    void testGetProjects_SelfDeveloper_ReturnsMemberProjects() {
        User currentUser =
                currentUser(
                        RoleCode.VT_04,
                        DataScope.SELF,
                        null
                );

        when(authorizationService.require(PermissionCode.PROJECT_READ))
                .thenReturn(CURRENT_USER_ID);

        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(currentEmployee()));

        Project memberProject =
                project(
                        30L,
                        "DEV-30",
                        8L,
                        200L
                );

        when(loadProjectPort.findMemberProjects(CURRENT_EMPLOYEE_ID, 0, 20))
                .thenReturn(List.of(memberProject));

        when(loadProjectPort.countMemberProjects(CURRENT_EMPLOYEE_ID))
                .thenReturn(1L);

        PageResult<ProjectResult> result =
                projectService.getProjects(0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals(30L, result.getContent().get(0).getId());

        verify(loadProjectPort)
                .findMemberProjects(CURRENT_EMPLOYEE_ID, 0, 20);

        verify(loadProjectPort, never())
                .findManagedBy(
                        any(Long.class),
                        any(Integer.class),
                        any(Integer.class)
                );
    }

    @Test
    @DisplayName("Khong co PROJECT_READ thi bi tu choi truoc khi query project")
    void testGetProjects_NoProjectRead_ThrowsPermissionDeniedException() {
        when(authorizationService.require(PermissionCode.PROJECT_READ))
                .thenThrow(
                        new PermissionDeniedException(
                                PermissionCode.PROJECT_READ
                        )
                );

        assertThrows(
                PermissionDeniedException.class,
                () -> projectService.getProjects(0, 20)
        );

        verify(loadUserPort, never())
                .findById(any());

        verify(loadProjectPort, never())
                .findAll(
                        any(Integer.class),
                        any(Integer.class)
                );
    }

    @Test
    @DisplayName("SELF voi role chua co business rule thi fail closed")
    void testGetProjects_SelfUnknownRole_ThrowsPermissionDeniedException() {
        User currentUser =
                currentUser(
                        RoleCode.VT_05,
                        DataScope.SELF,
                        null
                );

        when(authorizationService.require(PermissionCode.PROJECT_READ))
                .thenReturn(CURRENT_USER_ID);

        when(loadUserPort.findById(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(currentUser));

        when(loadEmployeePort.findByUserId(new UserId(CURRENT_USER_ID)))
                .thenReturn(Optional.of(currentEmployee()));

        assertThrows(
                PermissionDeniedException.class,
                () -> projectService.getProjects(0, 20)
        );

        verify(loadProjectPort, never())
                .findAll(
                        any(Integer.class),
                        any(Integer.class)
                );

        verify(loadProjectPort, never())
                .findManagedBy(
                        any(Long.class),
                        any(Integer.class),
                        any(Integer.class)
                );
    }

    private User currentUser(
            RoleCode roleCode,
            DataScope dataScope,
            Long scopeOrgUnitId
    ) {
        User user =
                new User(
                        new UserId(CURRENT_USER_ID),
                        "current",
                        "hash",
                        new Role(
                                new RoleId(1L),
                                roleCode,
                                roleCode.getName()
                        ),
                        UserStatus.ACTIVE,
                        null
                );

        user.changeDataScope(
                dataScope,
                scopeOrgUnitId
        );

        return user;
    }

    private Employee currentEmployee() {
        return new Employee(
                new EmployeeId(CURRENT_EMPLOYEE_ID),
                new UserId(CURRENT_USER_ID),
                5L,
                "EMP-100",
                "Current Employee",
                false,
                40,
                EmployeeStatus.ACTIVE
        );
    }

    private Project project(
            Long id,
            String code,
            Long orgUnitId,
            Long managerId
    ) {
        return new Project(
                new ProjectId(id),
                code,
                "Project " + code,
                orgUnitId,
                managerId != null
                        ? new EmployeeId(managerId)
                        : null,
                ProjectStatus.ACTIVE,
                new UserId(CURRENT_USER_ID),
                LocalDateTime.now(),
                null,
                0L
        );
    }
}
