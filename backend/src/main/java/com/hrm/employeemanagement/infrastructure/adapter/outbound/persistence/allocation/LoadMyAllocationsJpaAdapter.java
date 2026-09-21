package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.hrm.employeemanagement.application.port.outbound.allocation.LoadMyAllocationsPort;
import com.hrm.employeemanagement.domain.allocation.confirmation.AllocationItem;
import com.hrm.employeemanagement.domain.availability.YearWeek;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Component
public class LoadMyAllocationsJpaAdapter implements LoadMyAllocationsPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<AllocationItem> loadAllocationsForEmployeeInWeek(Long employeeId, LocalDate weekStartDate) {
        if (employeeId == null || weekStartDate == null) {
            return Collections.emptyList();
        }

        YearWeek yw = YearWeek.from(weekStartDate);
        int year = yw.year();
        int weekNumber = yw.weekNumber();

        String sql = """
            SELECT 
                ra.id AS allocation_id,
                p.id AS project_id,
                p.project_name AS project_name,
                p.status AS project_status,
                ra.allocated_hours AS allocated_hours,
                ra.updated_at AS allocation_updated_at
            FROM weekly_project_allocations ra
            JOIN projects p ON ra.project_id = p.id
            WHERE ra.employee_id = :employeeId
              AND ra.year_number = :year
              AND ra.week_number = :weekNumber
            ORDER BY ra.id ASC, p.project_name ASC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("employeeId", employeeId);
        query.setParameter("year", year);
        query.setParameter("weekNumber", weekNumber);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<AllocationItem> results = new ArrayList<>();
        for (Object[] row : rows) {
            Long allocationId = ((Number) row[0]).longValue();
            Long projectId = ((Number) row[1]).longValue();
            String projectName = (String) row[2];
            String projectStatus = (String) row[3];
            BigDecimal allocatedHours = row[4] instanceof BigDecimal bd ? bd : new BigDecimal(row[4].toString());

            LocalDateTime updatedAt = null;
            if (row[5] instanceof Timestamp ts) {
                updatedAt = ts.toLocalDateTime();
            } else if (row[5] instanceof LocalDateTime ldt) {
                updatedAt = ldt;
            }

            results.add(new AllocationItem(
                    allocationId,
                    projectId,
                    projectName,
                    projectStatus,
                    allocatedHours,
                    updatedAt
            ));
        }

        return results;
    }

    @Override
    public List<AllocationItem> loadAllocationsForEmployeeInWeeks(Long employeeId, List<LocalDate> weekStartDates) {
        if (employeeId == null || weekStartDates == null || weekStartDates.isEmpty()) {
            return Collections.emptyList();
        }

        List<AllocationItem> allItems = new ArrayList<>();
        for (LocalDate weekStartDate : weekStartDates) {
            allItems.addAll(loadAllocationsForEmployeeInWeek(employeeId, weekStartDate));
        }
        return allItems;
    }
}