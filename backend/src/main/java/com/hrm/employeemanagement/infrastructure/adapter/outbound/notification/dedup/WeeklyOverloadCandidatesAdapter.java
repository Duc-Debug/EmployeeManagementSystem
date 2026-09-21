package com.hrm.employeemanagement.infrastructure.adapter.outbound.notification.dedup;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.dto.notification.dedup.EmployeeWeeklyOverloadCandidate;
import com.hrm.employeemanagement.application.port.outbound.notification.dedup.LoadWeeklyOverloadCandidatesPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyCapacityMatrixPolicy;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.repository.SpringDataWeeklyProjectAllocationRepository;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.entity.EmployeeJpaEntity;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.user.repository.SpringDataEmployeeRepository;

@Component
public class WeeklyOverloadCandidatesAdapter implements LoadWeeklyOverloadCandidatesPort {

    private final SpringDataWeeklyProjectAllocationRepository allocationRepository;
    private final SpringDataEmployeeRepository employeeRepository;

    public WeeklyOverloadCandidatesAdapter(
            SpringDataWeeklyProjectAllocationRepository allocationRepository,
            SpringDataEmployeeRepository employeeRepository
    ) {
        this.allocationRepository = Objects.requireNonNull(allocationRepository, "allocationRepository must not be null");
        this.employeeRepository = Objects.requireNonNull(employeeRepository, "employeeRepository must not be null");
    }

    @Override
    public List<EmployeeWeeklyOverloadCandidate> loadOverloadCandidates(int year, int weekNumber) {
        List<WeeklyProjectAllocationJpaEntity> allocations =
                allocationRepository.findByYearAndWeekNumberBetween(year, weekNumber, weekNumber);

        if (allocations.isEmpty()) {
            return List.of();
        }

        // Nhóm phân bổ theo employeeId
        Map<Long, List<WeeklyProjectAllocationJpaEntity>> allocationsByEmp = allocations.stream()
                .collect(Collectors.groupingBy(WeeklyProjectAllocationJpaEntity::getEmployeeId));

        List<Long> employeeIds = new ArrayList<>(allocationsByEmp.keySet());
        Map<Long, EmployeeJpaEntity> employeeMap = employeeRepository.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(EmployeeJpaEntity::getId, e -> e));

        List<EmployeeWeeklyOverloadCandidate> candidates = new ArrayList<>();

        for (Map.Entry<Long, List<WeeklyProjectAllocationJpaEntity>> entry : allocationsByEmp.entrySet()) {
            Long empId = entry.getKey();
            List<WeeklyProjectAllocationJpaEntity> empAllocations = entry.getValue();
            EmployeeJpaEntity employee = employeeMap.get(empId);

            String empCode = employee != null ? employee.getEmployeeCode() : "EMP" + empId;
            String empName = employee != null ? employee.getFullName() : "Nhân sự #" + empId;

            BigDecimal totalAllocated = empAllocations.stream()
                    .map(WeeklyProjectAllocationJpaEntity::getAllocatedHours)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal availableHours = BigDecimal.valueOf(
                    employee != null && employee.getStandardHoursPerWeek() != null
                            ? employee.getStandardHoursPerWeek()
                            : 40
            );

            // Kiểm tra trạng thái quá tải theo cờ isOverloaded trên entity hoặc theo policy
            boolean markedOverloaded = empAllocations.stream()
                    .anyMatch(a -> Boolean.TRUE.equals(a.getIsOverloaded()));

            boolean calculatedOverloaded = WeeklyCapacityMatrixPolicy.isOverloaded(totalAllocated, availableHours);
            boolean isOverloaded = markedOverloaded || calculatedOverloaded;

            List<Long> recipientUserIds = new ArrayList<>();
            if (employee != null && employee.getUserId() != null) {
                recipientUserIds.add(employee.getUserId());
            }

            candidates.add(new EmployeeWeeklyOverloadCandidate(
                    empId,
                    empCode,
                    empName,
                    year,
                    weekNumber,
                    totalAllocated,
                    availableHours,
                    isOverloaded,
                    recipientUserIds
            ));
        }

        return candidates;
    }
}
