package com.hrm.employeemanagement.application.service.allocation;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.ProjectWeeklyAllocationResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetProjectWeeklyAllocationsUseCase;
import com.hrm.employeemanagement.application.port.inbound.project.GetProjectDetailUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.service.authorization.AuthorizationService;
import com.hrm.employeemanagement.domain.authorization.PermissionCode;

public class GetProjectWeeklyAllocationsService implements GetProjectWeeklyAllocationsUseCase {
    private final GetProjectDetailUseCase getProjectDetailUseCase;
    private final LoadWeeklyProjectAllocationPort loadAllocationPort;
    private final AuthorizationService authorizationService;

    public GetProjectWeeklyAllocationsService(GetProjectDetailUseCase getProjectDetailUseCase,
            LoadWeeklyProjectAllocationPort loadAllocationPort,
            AuthorizationService authorizationService) {
        this.getProjectDetailUseCase = getProjectDetailUseCase;
        this.loadAllocationPort = loadAllocationPort;
        this.authorizationService = authorizationService;
    }

    @Override
    public List<ProjectWeeklyAllocationResult> getByProject(Long projectId, Integer year, Integer startWeek, Integer endWeek) {
        if (projectId == null || year == null || startWeek == null || endWeek == null
                || startWeek < 1 || endWeek > 53 || startWeek > endWeek) {
            throw new IllegalArgumentException("Project, year and week range are invalid");
        }
        authorizationService.require(PermissionCode.RESOURCE_ALLOCATION_READ);
        // Reuse project detail authorization so PROJECT_READ and project data scope are enforced.
        getProjectDetailUseCase.getProjectById(projectId);
        return loadAllocationPort.loadAllocationsForProjectInWeekRange(projectId, year, startWeek, endWeek)
                .stream()
                .map(a -> new ProjectWeeklyAllocationResult(a.getEmployeeId(), a.getProjectId(),
                        a.getYear(), a.getWeekNumber(), a.getAllocatedHours()))
                .toList();
    }
}
