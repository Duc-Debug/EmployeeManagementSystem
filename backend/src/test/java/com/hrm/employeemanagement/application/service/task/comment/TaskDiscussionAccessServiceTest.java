package com.hrm.employeemanagement.application.service.task.comment;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.task.LoadTaskPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.project.ProjectId;
import com.hrm.employeemanagement.domain.task.Task;
import com.hrm.employeemanagement.domain.task.TaskId;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

class TaskDiscussionAccessServiceTest {
    private AuthorizationService authorization;
    private LoadTaskPort tasks;
    private LoadProjectPort projects;
    private LoadUserPort users;
    private LoadEmployeePort employees;
    private TaskDiscussionAccessService service;
    private Task task;
    private Project project;
    private User user;

    @BeforeEach
    void setUp() {
        authorization = mock(AuthorizationService.class);
        tasks = mock(LoadTaskPort.class);
        projects = mock(LoadProjectPort.class);
        users = mock(LoadUserPort.class);
        employees = mock(LoadEmployeePort.class);
        service = new TaskDiscussionAccessService(authorization, tasks, projects, users, employees);
        task = mock(Task.class);
        project = mock(Project.class);
        user = mock(User.class);
        when(authorization.require(PermissionCode.TASK_DISCUSSION_READ)).thenReturn(7L);
        when(authorization.require(PermissionCode.TASK_DISCUSSION_CREATE)).thenReturn(7L);
        when(tasks.findById(TaskId.of(10L))).thenReturn(Optional.of(task));
        when(task.getProjectId()).thenReturn(new ProjectId(20L));
        when(projects.findById(new ProjectId(20L))).thenReturn(Optional.of(project));
        when(project.getIdValue()).thenReturn(20L);
        when(users.findById(new UserId(7L))).thenReturn(Optional.of(user));
    }

    @Test
    void deniesSelfScopeUserOutsideProject() {
        Employee employee = mock(Employee.class);
        when(user.getDataScope()).thenReturn(DataScope.SELF);
        when(employees.findByUserId(new UserId(7L))).thenReturn(Optional.of(employee));
        when(employee.getIdValue()).thenReturn(8L);
        assertThrows(PermissionDeniedException.class,
                () -> service.requireAccess(10L, PermissionCode.TASK_DISCUSSION_READ));
    }

    @Test
    void allowsSelfScopeProjectMember() {
        Employee employee = mock(Employee.class);
        when(user.getDataScope()).thenReturn(DataScope.SELF);
        when(employees.findByUserId(new UserId(7L))).thenReturn(Optional.of(employee));
        when(employee.getIdValue()).thenReturn(8L);
        when(projects.existsMember(20L, 8L)).thenReturn(true);
        assertSame(task, service.requireAccess(10L, PermissionCode.TASK_DISCUSSION_READ));
        verify(authorization).require(PermissionCode.TASK_DISCUSSION_READ);
    }

    @Test
    void deniesBranchScopeUserOutsideBranch() {
        when(user.getDataScope()).thenReturn(DataScope.ORGANIZATION_BRANCH);
        when(user.getScopeOrgUnitId()).thenReturn(30L);
        assertThrows(PermissionDeniedException.class,
                () -> service.requireAccess(10L, PermissionCode.TASK_DISCUSSION_CREATE));
        verify(projects).existsInOrgUnitBranch(20L, 30L);
    }
}
