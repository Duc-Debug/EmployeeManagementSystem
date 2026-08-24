package com.hrm.employeemanagement.application.service.project;

import java.util.List;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.project.ProjectResult;
import com.hrm.employeemanagement.application.dto.user.PageResult;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectListUseCase;
import com.hrm.employeemanagement.application.port.outbound.project.LoadProjectPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadEmployeePort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.DataScope;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;
import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.exception.authorization.PermissionDeniedException;
import com.hrm.employeemanagement.domain.exception.user.UserNotFoundException;
import com.hrm.employeemanagement.domain.project.Project;
import com.hrm.employeemanagement.domain.role.RoleCode;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.domain.user.UserId;

public class ProjectService implements GetProjectListUseCase {

    private final LoadProjectPort loadProjectPort;
    private final LoadUserPort loadUserPort;
    private final LoadEmployeePort loadEmployeePort;
    private final AuthorizationService authorizationService;

    public ProjectService(
            LoadProjectPort loadProjectPort,
            LoadUserPort loadUserPort,
            LoadEmployeePort loadEmployeePort,
            AuthorizationService authorizationService
    ) {
        this.loadProjectPort = Objects.requireNonNull(
                loadProjectPort,
                "LoadProjectPort must not be null"
        );
        this.loadUserPort = Objects.requireNonNull(
                loadUserPort,
                "LoadUserPort must not be null"
        );
        this.loadEmployeePort = Objects.requireNonNull(
                loadEmployeePort,
                "LoadEmployeePort must not be null"
        );
        this.authorizationService = Objects.requireNonNull(
                authorizationService,
                "AuthorizationService must not be null"
        );
    }

    @Override
    public PageResult<ProjectResult> getProjects(
            int page,
            int size
    ) {
        Long currentUserId = authorizationService.require(
                PermissionCode.PROJECT_READ
        );

        User currentUser =
                loadCurrentUserOrThrow(currentUserId);

        int safePage = Math.max(
                0,
                page
        );

        int safeSize = Math.min(
                Math.max(1, size),
                100
        );

        ProjectPage pageData =
                loadScopedProjects(
                        currentUser,
                        currentUserId,
                        safePage,
                        safeSize
                );

        return new PageResult<>(
                pageData.projects()
                        .stream()
                        .map(this::mapToProjectResult)
                        .toList(),
                safePage,
                safeSize,
                pageData.totalElements()
        );
    }

    private ProjectPage loadScopedProjects(
            User currentUser,
            Long currentUserId,
            int page,
            int size
    ) {
        DataScope dataScope =
                currentUser.getDataScope();

        return switch (dataScope) {
            case COMPANY -> new ProjectPage(
                    loadProjectPort.findAll(page, size),
                    loadProjectPort.count()
            );
            case ORGANIZATION_BRANCH -> new ProjectPage(
                    loadProjectPort.findByOrgUnitBranch(
                            currentUser.getScopeOrgUnitId(),
                            page,
                            size
                    ),
                    loadProjectPort.countByOrgUnitBranch(
                            currentUser.getScopeOrgUnitId()
                    )
            );
            case SELF -> loadSelfScopedProjects(
                    currentUser,
                    currentUserId,
                    page,
                    size
            );
        };
    }

    private ProjectPage loadSelfScopedProjects(
            User currentUser,
            Long currentUserId,
            int page,
            int size
    ) {
        RoleCode roleCode =
                currentUser.getRole().getCode();

        Long employeeId =
                loadCurrentEmployeeIdOrDeny(
                        currentUserId
                );

        return switch (roleCode) {
            case VT_02 -> new ProjectPage(
                    loadProjectPort.findManagedBy(
                            employeeId,
                            page,
                            size
                    ),
                    loadProjectPort.countManagedBy(
                            employeeId
                    )
            );
            case VT_04 -> new ProjectPage(
                    loadProjectPort.findMemberProjects(
                            employeeId,
                            page,
                            size
                    ),
                    loadProjectPort.countMemberProjects(
                            employeeId
                    )
            );
            default -> throw new PermissionDeniedException(
                    PermissionCode.PROJECT_READ
            );
        };
    }

    private Long loadCurrentEmployeeIdOrDeny(Long currentUserId) {
        return loadEmployeePort
                .findByUserId(
                        new UserId(currentUserId)
                )
                .map(Employee::getIdValue)
                .orElseThrow(() ->
                        new PermissionDeniedException(
                                PermissionCode.PROJECT_READ
                        )
                );
    }

    private User loadCurrentUserOrThrow(Long currentUserId) {
        return loadUserPort
                .findById(
                        new UserId(currentUserId)
                )
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Khong tim thay nguoi dung hien tai voi ID: "
                                        + currentUserId
                        )
                );
    }

    private ProjectResult mapToProjectResult(Project project) {
        return new ProjectResult(
                project.getIdValue(),
                project.getProjectCode(),
                project.getProjectName(),
                project.getOrgUnitId(),
                project.getManagerIdValue(),
                project.getStatus(),
                project.getCreatedByValue(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private record ProjectPage(
            List<Project> projects,
            long totalElements
    ) {
    }
}
