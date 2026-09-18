package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.report.excel;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.report.excel.LoadProjectAllocationsForExcelReportPort;
import com.hrm.employeemanagement.domain.allocation.WeeklyProjectAllocation;
import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.entity.WeeklyProjectAllocationJpaEntity;

@Component
public class ProjectAllocationReportExcelPersistenceAdapter implements LoadProjectAllocationsForExcelReportPort {

    @PersistenceContext
    private final EntityManager entityManager;

    public ProjectAllocationReportExcelPersistenceAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<WeeklyProjectAllocation> loadAllAllocationsForProject(Long projectId) {
        if (projectId == null) {
            return List.of();
        }

        List<WeeklyProjectAllocationJpaEntity> entities = entityManager.createQuery(
                "SELECT e FROM WeeklyProjectAllocationJpaEntity e " +
                        "WHERE e.projectId = :projectId " +
                        "ORDER BY e.year ASC, e.weekNumber ASC",
                WeeklyProjectAllocationJpaEntity.class
        )
        .setParameter("projectId", projectId)
        .getResultList();

        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        return entities.stream().map(this::toDomain).toList();
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
                e.getVarianceNote(),
                e.getUpdatedBy(),
                e.getVersion()
        );
    }
}
