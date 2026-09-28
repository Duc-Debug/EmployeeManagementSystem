package com.hrm.employeemanagement.domain.allocation.confirmation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Domain utility phụ trách tính toán định lượng trên danh sách phân bổ của tuần:
 * - Tổng số giờ (totalHours) làm tròn 2 chữ số thập phân (RoundingMode.HALF_UP).
 * - Mốc thời gian cập nhật lớn nhất (maxUpdatedAt) giữa các dòng phân bổ hợp lệ.
 */
public final class WeeklyAllocationCalculator {

    private WeeklyAllocationCalculator() {
    }

    public static BigDecimal calculateTotalHours(List<AllocationItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return items.stream()
                .map(AllocationItem::allocatedHours)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public static LocalDateTime findMaxUpdatedAt(List<AllocationItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.stream()
                .map(AllocationItem::updatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }
}