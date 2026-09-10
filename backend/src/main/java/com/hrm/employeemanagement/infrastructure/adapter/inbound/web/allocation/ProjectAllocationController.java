package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrm.employeemanagement.application.dto.allocation.ProjectWeeklyAllocationResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.GetProjectWeeklyAllocationsUseCase;
import com.hrm.employeemanagement.infrastructure.adapter.inbound.web.user.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectAllocationController {
    private final GetProjectWeeklyAllocationsUseCase useCase;

    public ProjectAllocationController(GetProjectWeeklyAllocationsUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{projectId}/allocations")
    public ResponseEntity<ApiResponse<List<ProjectWeeklyAllocationResult>>> getProjectAllocations(
            @PathVariable Long projectId,
            @RequestParam Integer year,
            @RequestParam Integer startWeek,
            @RequestParam Integer endWeek) {
        return ResponseEntity.ok(ApiResponse.success("Lay phan bo nguon luc cua du an thanh cong",
                useCase.getByProject(projectId, year, startWeek, endWeek)));
    }
}
