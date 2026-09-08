package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.application.port.outbound.allocation.SaveWeeklyProjectAllocationPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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
}
