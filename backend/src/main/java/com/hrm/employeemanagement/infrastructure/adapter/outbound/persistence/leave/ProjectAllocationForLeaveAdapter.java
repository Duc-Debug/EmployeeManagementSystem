package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.leave;

import com.hrm.employeemanagement.application.port.outbound.leave.LoadProjectAllocationForLeavePort;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.entity.ProjectJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.project.repository.SpringDataProjectRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProjectAllocationForLeaveAdapter implements LoadProjectAllocationForLeavePort {

    private final SpringDataWeeklyProjectAllocationRepository allocationRepository;
    private final SpringDataProjectRepository projectRepository;

    public ProjectAllocationForLeaveAdapter(
            SpringDataWeeklyProjectAllocationRepository allocationRepository,
            SpringDataProjectRepository projectRepository
    ) {
        this.allocationRepository = Objects.requireNonNull(allocationRepository, "allocationRepository must not be null");
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository must not be null");
    }

    @Override
    public List<ProjectAllocationInfo> findAllocations(Long employeeId, int year, List<Integer> weekNumbers) {
        if (employeeId == null || weekNumbers == null || weekNumbers.isEmpty()) {
            return Collections.emptyList();
        }

        List<WeeklyProjectAllocationJpaEntity> allocations = allocationRepository
                .findByEmployeeIdInAndYearAndWeekNumberIn(List.of(employeeId), year, weekNumbers);

        if (allocations.isEmpty()) {
            return Collections.emptyList();
        }

        // Lấy danh sách tên dự án
        Set<Long> projectIds = allocations.stream()
                .map(WeeklyProjectAllocationJpaEntity::getProjectId)
                .collect(Collectors.toSet());

        Map<Long, String> projectNameMap = projectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(ProjectJpaEntity::getId, ProjectJpaEntity::getProjectName, (a, b) -> a));

        return allocations.stream()
                .map(alloc -> new ProjectAllocationInfo(
                        alloc.getProjectId(),
                        projectNameMap.getOrDefault(alloc.getProjectId(), "Dự án #" + alloc.getProjectId()),
                        alloc.getYear(),
                        alloc.getWeekNumber(),
                        alloc.getAllocatedHours()
                ))
                .toList();
    }
}
