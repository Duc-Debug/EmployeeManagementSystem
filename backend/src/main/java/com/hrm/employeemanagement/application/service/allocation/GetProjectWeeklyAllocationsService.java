package com.hrm.employeemanagement.application.service.allocation;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.IsoFields;
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
                || year < Year.MIN_VALUE || year > Year.MAX_VALUE
                || startWeek < 1 || startWeek > endWeek || endWeek > getIsoWeeksInYear(year)) {
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

    private int getIsoWeeksInYear(int year) {
        return LocalDate.of(year, 12, 28).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    }
}
