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
                            null
                    ));
            entity.setAllocatedHours(allocation.getAllocatedHours());
        } else {
            entity = new WeeklyProjectAllocationJpaEntity(
                    null,
                    allocation.getEmployeeId(),
                    allocation.getProjectId(),
                    allocation.getYear(),
                    allocation.getWeekNumber(),
                    allocation.getAllocatedHours(),
                    null
            );
        }

        WeeklyProjectAllocationJpaEntity saved = repository.save(entity);
        return new WeeklyProjectAllocation(
                saved.getId(),
                saved.getEmployeeId(),
                saved.getProjectId(),
                YearWeek.of(saved.getYear(), saved.getWeekNumber()),
                saved.getAllocatedHours(),
                saved.getVersion()
        );
    }

    @Override
    public Optional<WeeklyProjectAllocation> loadAllocation(Long employeeId, Long projectId, YearWeek yearWeek) {
        return repository.findByEmployeeIdAndProjectIdAndYearAndWeekNumber(
                employeeId, projectId, yearWeek.year(), yearWeek.weekNumber())
                .map(e -> new WeeklyProjectAllocation(
                        e.getId(), e.getEmployeeId(), e.getProjectId(),
                        YearWeek.of(e.getYear(), e.getWeekNumber()),
                        e.getAllocatedHours(), e.getVersion()));
    }

    @Override
    public List<WeeklyProjectAllocation> loadAllocationsForEmployee(Long employeeId, YearWeek yearWeek) {
        return repository.findByEmployeeIdAndYearAndWeekNumber(employeeId, yearWeek.year(), yearWeek.weekNumber())
                .stream()
                .map(e -> new WeeklyProjectAllocation(
                        e.getId(), e.getEmployeeId(), e.getProjectId(),
                        YearWeek.of(e.getYear(), e.getWeekNumber()),
                        e.getAllocatedHours(), e.getVersion()))
                .toList();
    }

    @Override
    public List<WeeklyProjectAllocation> loadAllocationsForEmployeesInWeekRange(List<Long> employeeIds, Integer year, Integer startWeek, Integer endWeek) {
        return repository.findByEmployeeIdInAndYearAndWeekNumberBetween(employeeIds, year, startWeek, endWeek)
                .stream()
                .map(e -> new WeeklyProjectAllocation(
                        e.getId(), e.getEmployeeId(), e.getProjectId(),
                        YearWeek.of(e.getYear(), e.getWeekNumber()),
                        e.getAllocatedHours(), e.getVersion()))
                .toList();
    }

    @Override
    public List<WeeklyProjectAllocation> loadAllocationsForEmployeesAndWeeks(List<Long> employeeIds, List<YearWeek> targetWeeks) {
        if (employeeIds == null || employeeIds.isEmpty() || targetWeeks == null || targetWeeks.isEmpty()) {
            return List.of();
        }
        Map<Integer, List<Integer>> weeksByYear = targetWeeks.stream()
                .collect(Collectors.groupingBy(YearWeek::year, Collectors.mapping(YearWeek::weekNumber, Collectors.toList())));

        List<WeeklyProjectAllocation> results = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : weeksByYear.entrySet()) {
            Integer year = entry.getKey();
            List<Integer> weeks = entry.getValue();
            List<WeeklyProjectAllocationJpaEntity> entities = repository
                    .findByEmployeeIdInAndYearAndWeekNumberIn(employeeIds, year, weeks);
            results.addAll(entities.stream().map(e -> new WeeklyProjectAllocation(
                    e.getId(), e.getEmployeeId(), e.getProjectId(),
                    YearWeek.of(e.getYear(), e.getWeekNumber()),
                    e.getAllocatedHours(), e.getVersion())).toList());
        }
        return results;
    }
}
