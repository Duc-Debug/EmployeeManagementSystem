package com.hrm.employeemanagement.domain.allocation.period;

import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationPeriodStateException;

/**
 * Domain Aggregate Root đại diện cho một Kỳ kế hoạch phân bổ nguồn lực (NCL-06-CN-009 / QTN-18).
 */
public class AllocationPlanningPeriod {

    private Long id;
    private String name;
    private AllocationPeriodType periodType;
    private int year;
    private int startWeek;
    private int endWeek;
    private AllocationPeriodStatus status;
    private Long lockedBy;
    private LocalDateTime lockedAt;
    private Long unlockedBy;
    private LocalDateTime unlockedAt;
    private String unlockReason;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    public AllocationPlanningPeriod(
            Long id,
            String name,
            AllocationPeriodType periodType,
            int year,
            int startWeek,
            int endWeek,
            AllocationPeriodStatus status,
            Long lockedBy,
            LocalDateTime lockedAt,
            Long unlockedBy,
            LocalDateTime unlockedAt,
            String unlockReason,
            Long createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long version
    ) {
        this.id = id;
        validateFields(name, year, startWeek, endWeek);
        this.name = name.trim();
        this.periodType = periodType != null ? periodType : AllocationPeriodType.QUARTER;
        this.year = year;
        this.startWeek = startWeek;
        this.endWeek = endWeek;
        this.status = status != null ? status : AllocationPeriodStatus.OPEN;
        this.lockedBy = lockedBy;
        this.lockedAt = lockedAt;
        this.unlockedBy = unlockedBy;
        this.unlockedAt = unlockedAt;
        this.unlockReason = unlockReason;
        this.createdBy = Objects.requireNonNull(createdBy, "ID người tạo không được để trống");
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0L;
    }

    public static AllocationPlanningPeriod createNew(
            String name,
            AllocationPeriodType periodType,
            int year,
            int startWeek,
            int endWeek,
            Long createdBy
    ) {
        return new AllocationPlanningPeriod(
                null,
                name,
                periodType,
                year,
                startWeek,
                endWeek,
                AllocationPeriodStatus.OPEN,
                null,
                null,
                null,
                null,
                null,
                createdBy,
                LocalDateTime.now(),
                null,
                0L
        );
    }

    private void validateFields(String name, int year, int startWeek, int endWeek) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidAllocationPeriodException("Tên kỳ kế hoạch không được để trống");
        }
        if (year < 2000 || year > 2100) {
            throw new InvalidAllocationPeriodException("Năm của kỳ kế hoạch không hợp lệ: " + year);
        }
        if (startWeek < 1 || startWeek > 53) {
            throw new InvalidAllocationPeriodException("Tuần bắt đầu phải từ 1 đến 53: " + startWeek);
        }
        if (endWeek < 1 || endWeek > 53) {
            throw new InvalidAllocationPeriodException("Tuần kết thúc phải từ 1 đến 53: " + endWeek);
        }
        if (startWeek > endWeek) {
            throw new InvalidAllocationPeriodException("Tuần bắt đầu (" + startWeek + ") không được lớn hơn tuần kết thúc (" + endWeek + ")");
        }
    }

    /**
     * Khóa kỳ kế hoạch phân bổ (TC-01).
     */
    public void lock(Long userId) {
        if (this.status == AllocationPeriodStatus.LOCKED) {
            throw new InvalidAllocationPeriodStateException("Kỳ kế hoạch '" + this.name + "' đã ở trạng thái khóa (LOCKED)");
        }
        this.lockedBy = Objects.requireNonNull(userId, "ID người thực hiện khóa không được để trống");
        this.lockedAt = LocalDateTime.now();
        this.status = AllocationPeriodStatus.LOCKED;
    }

    /**
     * Mở lại kỳ kế hoạch phân bổ kèm theo lý do bắt buộc (TC-04).
     */
    public void unlock(Long userId, String reason) {
        if (this.status != AllocationPeriodStatus.LOCKED) {
            throw new InvalidAllocationPeriodStateException("Kỳ kế hoạch '" + this.name + "' đang mở, không thể thực hiện thao tác mở lại");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Lý do mở lại kỳ kế hoạch không được để trống");
        }
        this.unlockedBy = Objects.requireNonNull(userId, "ID người thực hiện mở lại không được để trống");
        this.unlockedAt = LocalDateTime.now();
        this.unlockReason = reason.trim();
        this.status = AllocationPeriodStatus.OPEN;
    }

    /**
     * Kiểm tra một tuần cụ thể có nằm trong kỳ kế hoạch này hay không.
     */
    public boolean isWeekWithin(int year, int weekNumber) {
        return this.year == year && weekNumber >= this.startWeek && weekNumber <= this.endWeek;
    }

    public boolean isLocked() {
        return this.status == AllocationPeriodStatus.LOCKED;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AllocationPeriodType getPeriodType() {
        return periodType;
    }

    public int getYear() {
        return year;
    }

    public int getStartWeek() {
        return startWeek;
    }

    public int getEndWeek() {
        return endWeek;
    }

    public AllocationPeriodStatus getStatus() {
        return status;
    }

    public Long getLockedBy() {
        return lockedBy;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public Long getUnlockedBy() {
        return unlockedBy;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public String getUnlockReason() {
        return unlockReason;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
