package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class WeeklyProjectAllocationPersistenceAdapter implements SaveWeeklyProjectAllocationPort, LoadWeeklyProjectAllocationPort {

    private final SpringDataWeeklyProjectAllocationRepository repository;

    public WeeklyProjectAllocationPersistenceAdapter(SpringDataWeeklyProjectAllocationRepository repository) {
        this.repository = repository;
    }

    @Override
    public WeeklyProjectAllocation save(WeeklyProjectAllocation allocation) {
        WeeklyProjectAllocationJpaEntity entity;
        if (allocation.getId() != null) {
            entity = repository.findById(allocation.getId())
                    .orElseGet(() -> new WeeklyProjectAllocationJpaEntity(
                            allocation.getId(),
                            allocation.getEmployeeId(),
                            allocation.getProjectId(),
                            allocation.getYear(),
                            allocation.getWeekNumber(),
                            allocation.getAllocatedHours(),
                            allocation.getAllocationPercentage(),
                            allocation.isOverloaded(),
                            allocation.getOverloadReason(),
                            allocation.getOverloadApprovedBy(),
                            allocation.getOverloadApprovedAt(),
                            null
                    ));
            entity.setAllocatedHours(allocation.getAllocatedHours());
            entity.setAllocationPercentage(allocation.getAllocationPercentage());
            entity.setIsOverloaded(allocation.isOverloaded());
            entity.setOverloadReason(allocation.getOverloadReason());
            entity.setOverloadApprovedBy(allocation.getOverloadApprovedBy());
            entity.setOverloadApprovedAt(allocation.getOverloadApprovedAt());
        } else {
            entity = new WeeklyProjectAllocationJpaEntity(
                    null,
                    allocation.getEmployeeId(),
                    allocation.getProjectId(),
                    allocation.getYear(),
                    allocation.getWeekNumber(),
                    allocation.getAllocatedHours(),
                    allocation.getAllocationPercentage(),
                    allocation.isOverloaded(),
                    allocation.getOverloadReason(),
                    allocation.getOverloadApprovedBy(),
                    allocation.getOverloadApprovedAt(),
                    null
            );
        }

        WeeklyProjectAllocationJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<WeeklyProjectAllocation> loadAllocation(Long employeeId, Long projectId, YearWeek yearWeek) {
        return repository.findByEmployeeIdAndProjectIdAndYearAndWeekNumber(
                employeeId, projectId, yearWeek.year(), yearWeek.weekNumber())
                .map(this::toDomain);
    }

    @Override
    public List<WeeklyProjectAllocation> loadAllocationsForEmployee(Long employeeId, YearWeek yearWeek) {
        return repository.findByEmployeeIdAndYearAndWeekNumber(employeeId, yearWeek.year(), yearWeek.weekNumber())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WeeklyProjectAllocation> loadAllocationsForEmployeesInWeekRange(List<Long> employeeIds, Integer year, Integer startWeek, Integer endWeek) {
        return repository.findByEmployeeIdInAndYearAndWeekNumberBetween(employeeIds, year, startWeek, endWeek)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private static <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    @Override
    public List<WeeklyProjectAllocation> loadAllocationsForEmployeesAndWeeks(List<Long> employeeIds, List<YearWeek> targetWeeks) {
        if (employeeIds == null || employeeIds.isEmpty() || targetWeeks == null || targetWeeks.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<Integer>> weeksByYear = targetWeeks.stream()
                .collect(Collectors.groupingBy(YearWeek::year, Collectors.mapping(YearWeek::weekNumber, Collectors.toList())));

        List<WeeklyProjectAllocation> results = new ArrayList<>();
        List<List<Long>> chunks = partitionList(employeeIds, 500);
        for (List<Long> chunk : chunks) {
            for (Map.Entry<Integer, List<Integer>> entry : weeksByYear.entrySet()) {
                Integer year = entry.getKey();
                List<Integer> weeks = entry.getValue();
                List<WeeklyProjectAllocationJpaEntity> entities = repository
                        .findByEmployeeIdInAndYearAndWeekNumberIn(chunk, year, weeks);
                results.addAll(entities.stream().map(this::toDomain).toList());
            }
        }
        return results;
    }

    @Override
    public List<WeeklyProjectAllocation> loadAllocationsForProjectInWeekRange(Long projectId, Integer year, Integer startWeek, Integer endWeek) {
        return repository.findByProjectIdAndYearAndWeekNumberBetween(projectId, year, startWeek, endWeek)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private WeeklyProjectAllocation toDomain(WeeklyProjectAllocationJpaEntity e) {
        return new WeeklyProjectAllocation(
                e.getId(),
                e.getEmployeeId(),
                e.getProjectId(),
                YearWeek.of(e.getYear(), e.getWeekNumber()),
                e.getAllocatedHours(),
                e.getAllocationPercentage(),
                e.getIsOverloaded() != null ? e.getIsOverloaded() : false,
                e.getOverloadReason(),
                e.getOverloadApprovedBy(),
                e.getOverloadApprovedAt(),
                e.getVersion()
        );
    }
}
