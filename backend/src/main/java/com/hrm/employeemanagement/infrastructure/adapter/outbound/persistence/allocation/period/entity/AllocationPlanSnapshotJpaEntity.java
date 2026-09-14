package com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.allocation.period.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "allocation_plan_snapshots")
public class AllocationPlanSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "snapshot_version", nullable = false)
    private Integer snapshotVersion;

    @Column(name = "total_allocations", nullable = false)
    private Integer totalAllocations;

    @Column(name = "total_allocated_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAllocatedHours;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public AllocationPlanSnapshotJpaEntity() {
    }

    public AllocationPlanSnapshotJpaEntity(
            Long id,
            Long periodId,
            Integer snapshotVersion,
            Integer totalAllocations,
            BigDecimal totalAllocatedHours,
            Long createdBy
    ) {
        this.id = id;
        this.periodId = periodId;
        this.snapshotVersion = snapshotVersion;
        this.totalAllocations = totalAllocations;
        this.totalAllocatedHours = totalAllocatedHours;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public void setPeriodId(Long periodId) {
        this.periodId = periodId;
    }

    public Integer getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(Integer snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public Integer getTotalAllocations() {
        return totalAllocations;
    }

    public void setTotalAllocations(Integer totalAllocations) {
        this.totalAllocations = totalAllocations;
    }

    public BigDecimal getTotalAllocatedHours() {
        return totalAllocatedHours;
    }

    public void setTotalAllocatedHours(BigDecimal totalAllocatedHours) {
        this.totalAllocatedHours = totalAllocatedHours;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
