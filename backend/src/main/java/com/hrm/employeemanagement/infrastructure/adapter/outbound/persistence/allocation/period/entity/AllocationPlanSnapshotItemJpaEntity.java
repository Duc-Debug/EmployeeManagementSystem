package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "allocation_plan_snapshot_items")
public class AllocationPlanSnapshotItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "original_allocation_id")
    private Long originalAllocationId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "year_number", nullable = false)
    private Integer year;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "allocated_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal allocatedHours;

    @Column(name = "allocation_percentage", precision = 5, scale = 2)
    private BigDecimal allocationPercentage;

    @Column(name = "is_overloaded", nullable = false)
    private Boolean isOverloaded = false;

    @Column(name = "overload_reason", length = 1000)
    private String overloadReason;

    public AllocationPlanSnapshotItemJpaEntity() {
    }

    public AllocationPlanSnapshotItemJpaEntity(
            Long id,
            Long snapshotId,
            Long originalAllocationId,
            Long employeeId,
            Long projectId,
            Integer year,
            Integer weekNumber,
            BigDecimal allocatedHours,
            BigDecimal allocationPercentage,
            Boolean isOverloaded,
            String overloadReason
    ) {
        this.id = id;
        this.snapshotId = snapshotId;
        this.originalAllocationId = originalAllocationId;
        this.employeeId = employeeId;
        this.projectId = projectId;
        this.year = year;
        this.weekNumber = weekNumber;
        this.allocatedHours = allocatedHours;
        this.allocationPercentage = allocationPercentage;
        this.isOverloaded = isOverloaded != null ? isOverloaded : false;
        this.overloadReason = overloadReason;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setOriginalAllocationId(Long originalAllocationId) {
        this.originalAllocationId = originalAllocationId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public BigDecimal getAllocatedHours() {
        return allocatedHours;
    }

    public void setAllocatedHours(BigDecimal allocatedHours) {
        this.allocatedHours = allocatedHours;
    }

    public BigDecimal getAllocationPercentage() {
        return allocationPercentage;
    }

    public void setAllocationPercentage(BigDecimal allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }

    public Boolean getIsOverloaded() {
        return isOverloaded;
    }

    public void setIsOverloaded(Boolean isOverloaded) {
        this.isOverloaded = isOverloaded;
    }

    public String getOverloadReason() {
        return overloadReason;
    }

    public void setOverloadReason(String overloadReason) {
        this.overloadReason = overloadReason;
    }
}
