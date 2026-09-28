package com.hrm.employeemanagement.domain.outsourcedcontract;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain Model biểu diễn hồ sơ hợp đồng nhân sự thuê ngoài sắp hết hạn hoặc đã hết hạn,
 * kèm danh sách các phân bổ dự án bị ảnh hưởng/vắt qua ngày hết hạn theo QTN-21.
 */
public class ExpiringOutsourcedContract {

    private final Long employeeId;
    private final String employeeCode;
    private final String fullName;
    private final String professionalRole;
    private final Long orgUnitId;
    private final String orgUnitName;
    private final LocalDate startDate;
    private final LocalDate contractEndDate;
    private final long daysRemaining;
    private final OutsourcedContractStatus status;
    private final List<OutsourcedContractAffectedAllocation> affectedAllocations;

    public ExpiringOutsourcedContract(
            Long employeeId,
            String employeeCode,
            String fullName,
            String professionalRole,
            Long orgUnitId,
            String orgUnitName,
            LocalDate startDate,
            LocalDate contractEndDate,
            long daysRemaining,
            OutsourcedContractStatus status,
            List<OutsourcedContractAffectedAllocation> affectedAllocations
    ) {
        this.employeeId = Objects.requireNonNull(employeeId, "employeeId must not be null");
        this.employeeCode = Objects.requireNonNull(employeeCode, "employeeCode must not be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.professionalRole = professionalRole;
        this.orgUnitId = orgUnitId;
        this.orgUnitName = orgUnitName;
        this.startDate = startDate;
        this.contractEndDate = Objects.requireNonNull(contractEndDate, "contractEndDate must not be null");
        this.daysRemaining = daysRemaining;
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.affectedAllocations = affectedAllocations != null ? List.copyOf(affectedAllocations) : Collections.emptyList();
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public String getProfessionalRole() {
        return professionalRole;
    }

    public Long getOrgUnitId() {
        return orgUnitId;
    }

    public String getOrgUnitName() {
        return orgUnitName;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public OutsourcedContractStatus getStatus() {
        return status;
    }

    public List<OutsourcedContractAffectedAllocation> getAffectedAllocations() {
        return affectedAllocations;
    }

    public boolean hasAffectedAllocations() {
        return !affectedAllocations.isEmpty();
    }

    public int countAffectedAllocations() {
        return affectedAllocations.size();
    }
}
