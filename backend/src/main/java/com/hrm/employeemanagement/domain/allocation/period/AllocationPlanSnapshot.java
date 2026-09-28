package com.hrm.employeemanagement.domain.allocation.period;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain Entity đại diện cho Bản chụp kế hoạch phân bổ của một kỳ tại thời điểm khóa (TC-01).
 */
public class AllocationPlanSnapshot {

    private Long id;
    private Long periodId;
    private int snapshotVersion;
    private int totalAllocations;
    private BigDecimal totalAllocatedHours;
    private Long createdBy;
    private LocalDateTime createdAt;
    private List<AllocationPlanSnapshotItem> items;

    public AllocationPlanSnapshot(
            Long id,
            Long periodId,
            int snapshotVersion,
            int totalAllocations,
            BigDecimal totalAllocatedHours,
            Long createdBy,
            LocalDateTime createdAt,
            List<AllocationPlanSnapshotItem> items
    ) {
        this.id = id;
        this.periodId = Objects.requireNonNull(periodId, "Period ID không được null");
        this.snapshotVersion = snapshotVersion;
        this.totalAllocations = totalAllocations;
        this.totalAllocatedHours = totalAllocatedHours != null ? totalAllocatedHours : BigDecimal.ZERO;
        this.createdBy = Objects.requireNonNull(createdBy, "ID người tạo không được null");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public static AllocationPlanSnapshot createNew(
            Long periodId,
            int snapshotVersion,
            Long createdBy,
            List<AllocationPlanSnapshotItem> items
    ) {
        int count = items != null ? items.size() : 0;
        BigDecimal totalHours = BigDecimal.ZERO;
        if (items != null) {
            for (AllocationPlanSnapshotItem item : items) {
                if (item.getAllocatedHours() != null) {
                    totalHours = totalHours.add(item.getAllocatedHours());
                }
            }
        }
        return new AllocationPlanSnapshot(
                null,
                periodId,
                snapshotVersion,
                count,
                totalHours,
                createdBy,
                LocalDateTime.now(),
                items
        );
    }

    public Long getId() {
        return id;
    }

    public Long getPeriodId() {
        return periodId;
    }

    public int getSnapshotVersion() {
        return snapshotVersion;
    }

    public int getTotalAllocations() {
        return totalAllocations;
    }

    public BigDecimal getTotalAllocatedHours() {
        return totalAllocatedHours;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<AllocationPlanSnapshotItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<AllocationPlanSnapshotItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.totalAllocations = this.items.size();
    }
}
