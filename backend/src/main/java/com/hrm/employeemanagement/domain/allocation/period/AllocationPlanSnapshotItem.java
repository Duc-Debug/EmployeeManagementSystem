package com.hrm.employeemanagement.domain.allocation.period;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Domain Entity đại diện cho một dòng chi tiết trong Bản chụp kế hoạch phân bổ.
 */
public class AllocationPlanSnapshotItem {

    private Long id;
    private Long snapshotId;
    private Long originalAllocationId;
    private Long employeeId;
    private Long projectId;
    private int year;
    private int weekNumber;
    private BigDecimal allocatedHours;
    private BigDecimal allocationPercentage;
    private boolean isOverloaded;
    private String overloadReason;

    public AllocationPlanSnapshotItem(
            Long id,
            Long snapshotId,
            Long originalAllocationId,
            Long employeeId,
            Long projectId,
            int year,
            int weekNumber,
            BigDecimal allocatedHours,
            BigDecimal allocationPercentage,
            boolean isOverloaded,
            String overloadReason
    ) {
        this.id = id;
        this.snapshotId = snapshotId;
        this.originalAllocationId = originalAllocationId;
        this.employeeId = Objects.requireNonNull(employeeId, "Employee ID không được null");
        this.projectId = Objects.requireNonNull(projectId, "Project ID không được null");
        this.year = year;
        this.weekNumber = weekNumber;
        this.allocatedHours = allocatedHours != null ? allocatedHours : BigDecimal.ZERO;
        this.allocationPercentage = allocationPercentage;
        this.isOverloaded = isOverloaded;
        this.overloadReason = overloadReason;
    }

    public static AllocationPlanSnapshotItem createNew(
            Long originalAllocationId,
            Long employeeId,
            Long projectId,
            int year,
            int weekNumber,
            BigDecimal allocatedHours,
            BigDecimal allocationPercentage,
            boolean isOverloaded,
            String overloadReason
    ) {
        return new AllocationPlanSnapshotItem(
                null,
                null,
                originalAllocationId,
                employeeId,
                projectId,
                year,
                weekNumber,
                allocatedHours,
                allocationPercentage,
                isOverloaded,
                overloadReason
        );
    }

    public Long getId() {
        return id;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Long getOriginalAllocationId() {
        return originalAllocationId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public int getYear() {
        return year;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public BigDecimal getAllocatedHours() {
        return allocatedHours;
    }

    public BigDecimal getAllocationPercentage() {
        return allocationPercentage;
    }

    public boolean isOverloaded() {
        return isOverloaded;
    }

    public String getOverloadReason() {
        return overloadReason;
    }
}
