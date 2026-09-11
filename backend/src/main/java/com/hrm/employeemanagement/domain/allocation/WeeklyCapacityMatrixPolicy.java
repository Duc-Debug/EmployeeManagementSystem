package com.hrm.employeemanagement.domain.allocation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domain Policy thực hiện các quy tắc tính toán năng lực tuần theo QTN-12:
 * - Nếu Tổng giờ phân bổ trong tuần > Giờ khả dụng trong tuần -> nhân sự bị đánh dấu quá tải (OVERLOADED).
 * - Hiển thị số giờ vượt: excessHours = allocatedHours - availableHours.
 * - Tỷ lệ sử dụng: Utilization = (allocatedHours / availableHours) * 100%.
 * - Xử lý trường hợp đặc biệt khi availableHours = 0:
 *   + Khi allocated > 0 và available = 0: quá tải (OVERLOADED), excessHours = allocated, utilization = null (không xác định / vô cực).
 *   + Khi allocated = 0 và available = 0 (nghỉ phép hợp lệ và không giao việc): trạng thái OPTIMAL (đạt kỳ vọng), utilization = 0.0%, không đánh giá nhàn rỗi ảnh hưởng KPI.
 * - Trạng thái thông thường: OVERLOADED (> 100%), OPTIMAL (50% - 100%), UNDERUTILIZED (< 50%).
 */
public class WeeklyCapacityMatrixPolicy {

    public static final BigDecimal UNDERUTILIZED_THRESHOLD = BigDecimal.valueOf(50.0);

    /**
     * Kiểm tra nhân sự có bị phân bổ quá tải trong tuần không theo QTN-12.
     */
    public static boolean isOverloaded(BigDecimal allocatedHours, BigDecimal availableHours) {
        BigDecimal safeAllocated = allocatedHours != null ? allocatedHours : BigDecimal.ZERO;
        BigDecimal safeAvailable = availableHours != null ? availableHours : BigDecimal.ZERO;
        return safeAllocated.compareTo(safeAvailable) > 0;
    }

    /**
     * Tính số giờ phân bổ vượt quá giờ khả dụng (excessHours) theo QTN-12.
     */
    public static BigDecimal calculateExcessHours(BigDecimal allocatedHours, BigDecimal availableHours) {
        BigDecimal safeAllocated = allocatedHours != null ? allocatedHours : BigDecimal.ZERO;
        BigDecimal safeAvailable = availableHours != null ? availableHours : BigDecimal.ZERO;

        if (safeAllocated.compareTo(safeAvailable) > 0) {
            return safeAllocated.subtract(safeAvailable).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tính tỷ lệ sử dụng năng lực (%) của nhân sự trong tuần.
     * Xử lý trường hợp availableHours = 0 an toàn:
     * - Nếu available = 0 và allocated > 0: trả về null (vô cực / không xác định) vì không thể chia cho 0.
     * - Nếu available = 0 và allocated = 0: trả về 0.0%.
     */
    public static BigDecimal calculateUtilizationPercentage(BigDecimal allocatedHours, BigDecimal availableHours) {
        BigDecimal safeAllocated = allocatedHours != null ? allocatedHours : BigDecimal.ZERO;
        BigDecimal safeAvailable = availableHours != null ? availableHours : BigDecimal.ZERO;

        if (safeAvailable.compareTo(BigDecimal.ZERO) <= 0) {
            if (safeAllocated.compareTo(BigDecimal.ZERO) > 0) {
                return null; // Vô cực / không thể chia cho 0
            }
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return safeAllocated
                .multiply(BigDecimal.valueOf(100))
                .divide(safeAvailable, 1, RoundingMode.HALF_UP);
    }

    /**
     * Xác định trạng thái năng lực của ô nhân sự trong tuần:
     * - Khi available = 0:
     *   + allocated > 0 -> OVERLOADED (giao việc khi không có khả dụng)
     *   + allocated = 0 -> OPTIMAL (nghỉ hợp lệ không giao việc, không phạt nhàn rỗi)
     * - Khi available > 0:
     *   + allocated > available -> OVERLOADED
     *   + utilization < 50% -> UNDERUTILIZED
     *   + 50% <= utilization <= 100% -> OPTIMAL
     */
    public static CapacityStatus determineStatus(BigDecimal allocatedHours, BigDecimal availableHours) {
        BigDecimal safeAllocated = allocatedHours != null ? allocatedHours : BigDecimal.ZERO;
        BigDecimal safeAvailable = availableHours != null ? availableHours : BigDecimal.ZERO;

        if (safeAvailable.compareTo(BigDecimal.ZERO) <= 0) {
            if (safeAllocated.compareTo(BigDecimal.ZERO) > 0) {
                return CapacityStatus.OVERLOADED;
            }
            return CapacityStatus.OPTIMAL;
        }

        if (safeAllocated.compareTo(safeAvailable) > 0) {
            return CapacityStatus.OVERLOADED;
        }

        BigDecimal utilization = calculateUtilizationPercentage(safeAllocated, safeAvailable);
        if (utilization != null && utilization.compareTo(UNDERUTILIZED_THRESHOLD) < 0) {
            return CapacityStatus.UNDERUTILIZED;
        }

        return CapacityStatus.OPTIMAL;
    }
}
