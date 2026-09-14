package com.hrm.employeemanagement.domain.allocation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import com.hrm.employeemanagement.domain.availability.YearWeek;
import com.hrm.employeemanagement.domain.exception.allocation.CannotRemoveAllocationWithActualHoursException;
import com.hrm.employeemanagement.domain.exception.allocation.InvalidAllocationAdjustmentException;

/**
 * Domain Policy chứa các quy tắc nghiệp vụ cho điều chỉnh phân bổ nguồn lực (QTN-15, TC-01, TC-02).
 */
public final class AllocationAdjustmentPolicy {

    private AllocationAdjustmentPolicy() {
    }

    /**
     * Kiểm tra xem một tuần đã kết thúc/trôi qua tính đến ngày hiện tại hay chưa.
     * Một tuần kết thúc sau ngày Chủ Nhật (endDate) của tuần đó.
     */
    public static boolean isWeekEnded(YearWeek yearWeek, LocalDate today) {
        Objects.requireNonNull(yearWeek, "YearWeek không được null");
        LocalDate refDate = today != null ? today : LocalDate.now();
        return refDate.isAfter(yearWeek.getEndDate());
    }

    /**
     * [TC-02] Kiểm tra điều kiện chặn gỡ dòng phân bổ:
     * Nếu tuần đã trôi qua VÀ đã có giờ công thực tế -> chặn gỡ, yêu cầu ghi chú lý do chênh lệch.
     */
    public static void validateCanRemove(Long allocationId, YearWeek yearWeek, boolean hasActualHours, LocalDate today) {
        if (isWeekEnded(yearWeek, today) && hasActualHours) {
            throw new CannotRemoveAllocationWithActualHoursException(
                    allocationId,
                    yearWeek.year(),
                    yearWeek.weekNumber()
            );
        }
    }

    /**
     * [K4] Kiểm tra tính hợp lệ của tuần đích khi chuyển tuần (MOVE_WEEK).
     * Tuần đích không được là tuần đã kết thúc và phải khác tuần hiện tại.
     */
    public static void validateTargetWeek(YearWeek currentWeek, YearWeek targetWeek, LocalDate today) {
        Objects.requireNonNull(currentWeek, "Tuần hiện tại không được null");
        if (targetWeek == null) {
            throw new InvalidAllocationAdjustmentException("Tuần đích (targetWeek) không được để trống khi chuyển tuần");
        }
        if (currentWeek.equals(targetWeek)) {
            throw new InvalidAllocationAdjustmentException("Tuần đích phải khác tuần hiện tại đang phân bổ");
        }
        if (isWeekEnded(targetWeek, today)) {
            throw new InvalidAllocationAdjustmentException(
                    "Không thể chuyển phân bổ đến tuần đã kết thúc (" + targetWeek.weekNumber() + "/" + targetWeek.year() + ")"
            );
        }
    }

    /**
     * Kiểm tra số giờ phân bổ mới khi sửa giờ.
     */
    public static void validateNewHours(BigDecimal hours) {
        if (hours == null) {
            throw new InvalidAllocationAdjustmentException("Số giờ phân bổ mới không được để trống");
        }
        if (hours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAllocationAdjustmentException("Số giờ phân bổ mới phải lớn hơn 0");
        }
        if (hours.compareTo(BigDecimal.valueOf(168)) > 0) {
            throw new InvalidAllocationAdjustmentException("Số giờ phân bổ cho 1 dự án trong tuần không được vượt quá 168 giờ");
        }
    }

    /**
     * Kiểm tra tỷ lệ phần trăm phân bổ mới khi sửa giờ.
     */
    public static void validateAllocationPercentage(BigDecimal percentage) {
        if (percentage == null) {
            throw new InvalidAllocationAdjustmentException("Tỷ lệ phần trăm phân bổ mới không được để trống");
        }
        if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAllocationAdjustmentException("Tỷ lệ phần trăm phân bổ mới phải lớn hơn 0%");
        }
        if (percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidAllocationAdjustmentException("Tỷ lệ phần trăm phân bổ không được vượt quá 100%");
        }
    }

    /**
     * Kiểm tra nội dung lý do chênh lệch khi ghi chú.
     */
    public static void validateVarianceNote(String varianceNote) {
        if (varianceNote == null || varianceNote.trim().isEmpty()) {
            throw new InvalidAllocationAdjustmentException("Lý do chênh lệch không được để trống");
        }
    }
}
