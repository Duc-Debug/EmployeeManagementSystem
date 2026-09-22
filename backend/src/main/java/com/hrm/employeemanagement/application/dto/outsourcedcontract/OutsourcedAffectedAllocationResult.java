package com.hrm.employeemanagement.application.dto.outsourcedcontract;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hrm.employeemanagement.domain.outsourcedcontract.OutsourcedContractAffectedAllocation;

/**
 * DTO chi tiết dòng phân bổ tuần bị ảnh hưởng hoặc vi phạm QTN-21.
 */
public record OutsourcedAffectedAllocationResult(
        Long allocationId,
        Long projectId,
        String projectName,
        int year,
        int weekNumber,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        BigDecimal allocatedHours,
        String affectedType,
        String reason
) {
    public static OutsourcedAffectedAllocationResult fromDomain(OutsourcedContractAffectedAllocation allocation) {
        return new OutsourcedAffectedAllocationResult(
                allocation.allocationId(),
                allocation.projectId(),
                allocation.projectName(),
                allocation.yearWeek().year(),
                allocation.yearWeek().weekNumber(),
                allocation.weekStartDate(),
                allocation.weekEndDate(),
                allocation.allocatedHours(),
                allocation.affectedType().name(),
                allocation.reason()
        );
    }
}
