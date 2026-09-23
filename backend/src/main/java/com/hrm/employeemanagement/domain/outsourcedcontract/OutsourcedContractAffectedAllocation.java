package com.hrm.employeemanagement.domain.outsourcedcontract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Value Object biểu diễn một dòng phân bổ tuần bị ảnh hưởng hoặc vi phạm QTN-21 đối với hợp đồng thuê ngoài.
 */
public record OutsourcedContractAffectedAllocation(
        Long allocationId,
        Long projectId,
        String projectName,
        YearWeek yearWeek,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        BigDecimal allocatedHours,
        AffectedAllocationType affectedType,
        String reason
) {
    public OutsourcedContractAffectedAllocation {
        Objects.requireNonNull(yearWeek, "yearWeek must not be null");
        Objects.requireNonNull(affectedType, "affectedType must not be null");
        if (allocatedHours == null) {
            allocatedHours = BigDecimal.ZERO;
        }
        if (weekStartDate == null) {
            weekStartDate = yearWeek.getStartDate();
        }
        if (weekEndDate == null) {
            weekEndDate = yearWeek.getEndDate();
        }
    }
}
