package com.hrm.employeemanagement.domain.allocation;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity đại diện cho một bản ghi nhật ký kiểm toán thay đổi phân bổ nguồn lực (QTN-15, TC-04).
 */
public class AllocationChangeLog {

    private final Long id;
    private final Long allocationId;
    private final AdjustmentAction action;
    private final String oldValue;
    private final String newValue;
    private final Long changedBy;
    private final LocalDateTime changedAt;
    private final String notifiedPmIds;

    public AllocationChangeLog(
            Long id,
            Long allocationId,
            AdjustmentAction action,
            String oldValue,
            String newValue,
            Long changedBy,
            LocalDateTime changedAt,
            String notifiedPmIds
    ) {
        this.id = id;
        this.allocationId = Objects.requireNonNull(allocationId, "allocationId không được null");
        this.action = Objects.requireNonNull(action, "action không được null");
        this.oldValue = oldValue != null ? oldValue : "";
        this.newValue = newValue != null ? newValue : "";
        this.changedBy = Objects.requireNonNull(changedBy, "changedBy không được null");
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
        this.notifiedPmIds = notifiedPmIds;
    }

    public static AllocationChangeLog create(
            Long allocationId,
            AdjustmentAction action,
            String oldValue,
            String newValue,
            Long changedBy,
            String notifiedPmIds
    ) {
        return new AllocationChangeLog(
                null,
                allocationId,
                action,
                oldValue,
                newValue,
                changedBy,
                LocalDateTime.now(),
                notifiedPmIds
        );
    }

    public Long getId() {
        return id;
    }

    public Long getAllocationId() {
        return allocationId;
    }

    public AdjustmentAction getAction() {
        return action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getNotifiedPmIds() {
        return notifiedPmIds;
    }
}
