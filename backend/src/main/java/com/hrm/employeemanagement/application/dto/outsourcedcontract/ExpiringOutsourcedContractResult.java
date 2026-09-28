package com.hrm.employeemanagement.application.dto.outsourcedcontract;

import java.time.LocalDate;
import java.util.List;

import com.hrm.employeemanagement.domain.outsourcedcontract.ExpiringOutsourcedContract;

/**
 * DTO đại diện hồ sơ hợp đồng thuê ngoài sắp hết hạn hoặc quá hạn kèm danh sách phân bổ bị ảnh hưởng.
 */
public record ExpiringOutsourcedContractResult(
        Long employeeId,
        String employeeCode,
        String fullName,
        String professionalRole,
        Long orgUnitId,
        String orgUnitName,
        LocalDate startDate,
        LocalDate contractEndDate,
        long daysRemaining,
        String status,
        int totalAffectedAllocations,
        List<OutsourcedAffectedAllocationResult> affectedAllocations
) {
    public static ExpiringOutsourcedContractResult fromDomain(ExpiringOutsourcedContract domain) {
        List<OutsourcedAffectedAllocationResult> allocations = domain.getAffectedAllocations().stream()
                .map(OutsourcedAffectedAllocationResult::fromDomain)
                .toList();
        return new ExpiringOutsourcedContractResult(
                domain.getEmployeeId(),
                domain.getEmployeeCode(),
                domain.getFullName(),
                domain.getProfessionalRole(),
                domain.getOrgUnitId(),
                domain.getOrgUnitName(),
                domain.getStartDate(),
                domain.getContractEndDate(),
                domain.getDaysRemaining(),
                domain.getStatus().name(),
                domain.countAffectedAllocations(),
                allocations
        );
    }
}
