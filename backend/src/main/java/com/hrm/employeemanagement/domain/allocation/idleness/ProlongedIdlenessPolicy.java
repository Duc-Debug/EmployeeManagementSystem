package com.hrm.employeemanagement.domain.allocation.idleness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Domain Policy thực hiện các quy tắc nghiệp vụ rà soát nhân sự nhàn rỗi kéo dài (NCL-07-CN-006):
 * - QTN-23: Sử dụng ngưỡng nhàn rỗi động (idleThreshold) do Ban giám đốc cấu hình.
 * - NCL-07-CN-006-TC-01: Phát hiện nhân sự có mức sử dụng dưới ngưỡng nhàn rỗi trong N tuần liên tiếp (mặc định 3 tuần).
 * - NCL-07-CN-006-TC-02: Loại trừ nhân sự đang nghỉ phép dài ngày (APPROVED) khỏi danh sách nhàn rỗi kéo dài.
 */
public class ProlongedIdlenessPolicy {

    public static final BigDecimal DEFAULT_IDLE_THRESHOLD = BigDecimal.valueOf(30.0).setScale(1, RoundingMode.HALF_UP);
    public static final int DEFAULT_CONSECUTIVE_WEEKS = 3;

    /**
     * Tính tỷ lệ sử dụng năng lực (%) trong tuần.
     * Nếu availableHours <= 0:
     * - Nếu allocatedHours > 0 -> null (quá tải / vô cực)
     * - Nếu allocatedHours == 0 -> BigDecimal.ZERO (nghỉ phép trọn tuần)
     */
    public static BigDecimal calculateUtilizationRate(BigDecimal allocatedHours, BigDecimal availableHours) {
        BigDecimal safeAllocated = allocatedHours != null ? allocatedHours : BigDecimal.ZERO;
        BigDecimal safeAvailable = availableHours != null ? availableHours : BigDecimal.ZERO;

        if (safeAvailable.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return safeAllocated
                .multiply(BigDecimal.valueOf(100))
                .divide(safeAvailable, 1, RoundingMode.HALF_UP);
    }

    /**
     * Tính số giờ trống (emptyHours) trong tuần:
     * EmptyHours = max(0, availableHours - allocatedHours)
     */
    public static BigDecimal calculateEmptyHours(BigDecimal allocatedHours, BigDecimal availableHours) {
        BigDecimal safeAllocated = allocatedHours != null ? allocatedHours : BigDecimal.ZERO;
        BigDecimal safeAvailable = availableHours != null ? availableHours : BigDecimal.ZERO;

        if (safeAvailable.compareTo(safeAllocated) > 0) {
            return safeAvailable.subtract(safeAllocated).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Xác định một tuần có bị nhàn rỗi (underutilized) theo QTN-23 hay không:
     * - Nếu availableHours <= 0 và allocatedHours == 0: tuần nghỉ trọn vẹn hợp lệ -> KHÔNG coi là tuần nhàn rỗi bị phạt.
     * - Nếu availableHours > 0 và utilizationRate < idleThreshold -> NHÀN RỖI (true).
     */
    public static boolean isWeekUnderutilized(BigDecimal allocatedHours, BigDecimal availableHours, BigDecimal idleThreshold) {
        BigDecimal safeAllocated = allocatedHours != null ? allocatedHours : BigDecimal.ZERO;
        BigDecimal safeAvailable = availableHours != null ? availableHours : BigDecimal.ZERO;
        BigDecimal effectiveThreshold = idleThreshold != null ? idleThreshold : DEFAULT_IDLE_THRESHOLD;

        if (safeAvailable.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal utilization = calculateUtilizationRate(safeAllocated, safeAvailable);
        return utilization.compareTo(effectiveThreshold) < 0;
    }

    /**
     * Kiểm tra ngoại lệ NCL-07-CN-006-TC-02:
     * Nếu nhân sự đang nghỉ phép dài ngày đã duyệt (APPROVED) trong toàn bộ các tuần rà soát,
     * hoặc phần lớn thời gian là nghỉ phép đã duyệt khiến availableHours = 0 toàn bộ các tuần,
     * thì KHÔNG coi là nhàn rỗi kéo dài.
     */
    public static boolean isExcludedDueToLongTermLeave(List<WeeklyIdlenessDetail> weeklyDetails) {
        if (weeklyDetails == null || weeklyDetails.isEmpty()) {
            return false;
        }

        // Nếu tất cả các tuần đều là tuần nghỉ phép trọn vẹn (isFullLeaveWeek hoặc approvedLeaveHours > 0 và availableHours == 0)
        boolean allWeeksOnLeave = true;
        for (WeeklyIdlenessDetail detail : weeklyDetails) {
            boolean hasSignificantLeave = detail.isFullLeaveWeek()
                    || (detail.approvedLeaveHours().compareTo(BigDecimal.ZERO) > 0
                    && detail.availableHours().compareTo(BigDecimal.ZERO) <= 0);

            if (!hasSignificantLeave) {
                allWeeksOnLeave = false;
                break;
            }
        }

        return allWeeksOnLeave;
    }

    /**
     * Đếm số tuần nhàn rỗi liên tiếp lớn nhất trong chuỗi các tuần (NCL-07-CN-006-TC-01).
     */
    public static int findMaxConsecutiveIdleWeeks(List<WeeklyIdlenessDetail> weeklyDetails) {
        if (weeklyDetails == null || weeklyDetails.isEmpty()) {
            return 0;
        }

        int maxConsecutive = 0;
        int currentStreak = 0;

        for (WeeklyIdlenessDetail detail : weeklyDetails) {
            if (detail.isUnderutilized()) {
                currentStreak++;
                if (currentStreak > maxConsecutive) {
                    maxConsecutive = currentStreak;
                }
            } else {
                currentStreak = 0;
            }
        }

        return maxConsecutive;
    }

    /**
     * Xác định xem nhân sự có thỏa mãn tiêu chí cảnh báo nhàn rỗi kéo dài hay không:
     * 1. Không thuộc ngoại lệ nghỉ phép dài ngày (TC-02).
     * 2. Có chuỗi tuần nhàn rỗi liên tiếp >= consecutiveWeeksThreshold (TC-01).
     */
    public static boolean qualifiesForProlongedIdlenessAlert(
            List<WeeklyIdlenessDetail> weeklyDetails,
            int consecutiveWeeksThreshold
    ) {
        if (weeklyDetails == null || weeklyDetails.isEmpty()) {
            return false;
        }

        // Kiểm tra ngoại lệ nghỉ dài ngày (TC-02)
        if (isExcludedDueToLongTermLeave(weeklyDetails)) {
            return false;
        }

        int threshold = consecutiveWeeksThreshold > 0 ? consecutiveWeeksThreshold : DEFAULT_CONSECUTIVE_WEEKS;
        int maxConsecutive = findMaxConsecutiveIdleWeeks(weeklyDetails);

        return maxConsecutive >= threshold;
    }
}
