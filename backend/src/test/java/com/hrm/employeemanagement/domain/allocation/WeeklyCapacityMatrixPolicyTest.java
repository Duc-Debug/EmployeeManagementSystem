package com.hrm.employeemanagement.domain.allocation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyCapacityMatrixPolicyTest {

    @Test
    @DisplayName("QTN-12: Khi tổng giờ phân bổ > giờ khả dụng -> quá tải (OVERLOADED) và tính đúng excessHours")
    void testOverloadedWhenAllocatedExceedsAvailable() {
        BigDecimal allocated = BigDecimal.valueOf(48.0);
        BigDecimal available = BigDecimal.valueOf(40.0);

        assertThat(WeeklyCapacityMatrixPolicy.isOverloaded(allocated, available)).isTrue();
        assertThat(WeeklyCapacityMatrixPolicy.calculateExcessHours(allocated, available))
                .isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(allocated, available))
                .isEqualByComparingTo(BigDecimal.valueOf(120.0));
        assertThat(WeeklyCapacityMatrixPolicy.determineStatus(allocated, available))
                .isEqualTo(CapacityStatus.OVERLOADED);
    }

    @Test
    @DisplayName("QTN-12: Khi tổng giờ phân bổ <= giờ khả dụng -> không quá tải, excessHours = 0")
    void testNotOverloadedWhenAllocatedWithinAvailable() {
        BigDecimal allocated = BigDecimal.valueOf(32.0);
        BigDecimal available = BigDecimal.valueOf(40.0);

        assertThat(WeeklyCapacityMatrixPolicy.isOverloaded(allocated, available)).isFalse();
        assertThat(WeeklyCapacityMatrixPolicy.calculateExcessHours(allocated, available))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(allocated, available))
                .isEqualByComparingTo(BigDecimal.valueOf(80.0));
        assertThat(WeeklyCapacityMatrixPolicy.determineStatus(allocated, available))
                .isEqualTo(CapacityStatus.OPTIMAL);
    }

    @Test
    @DisplayName("QTN-12: Khi phân bổ chính xác bằng giờ khả dụng (100%) -> OPTIMAL, không quá tải")
    void testExactly100PercentCapacity() {
        BigDecimal allocated = BigDecimal.valueOf(40.0);
        BigDecimal available = BigDecimal.valueOf(40.0);

        assertThat(WeeklyCapacityMatrixPolicy.isOverloaded(allocated, available)).isFalse();
        assertThat(WeeklyCapacityMatrixPolicy.calculateExcessHours(allocated, available))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(allocated, available))
                .isEqualByComparingTo(BigDecimal.valueOf(100.0));
        assertThat(WeeklyCapacityMatrixPolicy.determineStatus(allocated, available))
                .isEqualTo(CapacityStatus.OPTIMAL);
    }

    @Test
    @DisplayName("Trạng thái nhàn rỗi (UNDERUTILIZED) khi tỷ lệ sử dụng < 50%")
    void testUnderutilizedStatus() {
        BigDecimal allocated = BigDecimal.valueOf(16.0);
        BigDecimal available = BigDecimal.valueOf(40.0);

        assertThat(WeeklyCapacityMatrixPolicy.isOverloaded(allocated, available)).isFalse();
        assertThat(WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(allocated, available))
                .isEqualByComparingTo(BigDecimal.valueOf(40.0));
        assertThat(WeeklyCapacityMatrixPolicy.determineStatus(allocated, available))
                .isEqualTo(CapacityStatus.UNDERUTILIZED);
    }

    @Test
    @DisplayName("Trạng thái nhàn rỗi (UNDERUTILIZED) khi không được phân bổ giờ nào (0h / 40h)")
    void testZeroAllocatedHours() {
        BigDecimal allocated = BigDecimal.ZERO;
        BigDecimal available = BigDecimal.valueOf(40.0);

        assertThat(WeeklyCapacityMatrixPolicy.isOverloaded(allocated, available)).isFalse();
        assertThat(WeeklyCapacityMatrixPolicy.calculateExcessHours(allocated, available))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(allocated, available))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(WeeklyCapacityMatrixPolicy.determineStatus(allocated, available))
                .isEqualTo(CapacityStatus.UNDERUTILIZED);
    }

    @Test
    @DisplayName("Xử lý an toàn khi availableHours = 0:")
    void testSafeHandlingWhenAvailableHoursIsZero() {
        // Trường hợp 1: Có phân bổ khi available = 0 (nghỉ phép nhưng vẫn bị giao việc) -> Quá tải (OVERLOADED), excessHours = 10, utilization = null (không xác định/vô cực, không gán 100%)
        BigDecimal allocated = BigDecimal.valueOf(10.0);
        BigDecimal available = BigDecimal.ZERO;

        assertThat(WeeklyCapacityMatrixPolicy.isOverloaded(allocated, available)).isTrue();
        assertThat(WeeklyCapacityMatrixPolicy.calculateExcessHours(allocated, available))
                .isEqualByComparingTo(BigDecimal.valueOf(10.0));
        assertThat(WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(allocated, available))
                .isNull();
        assertThat(WeeklyCapacityMatrixPolicy.determineStatus(allocated, available))
                .isEqualTo(CapacityStatus.OVERLOADED);

        // Trường hợp 2: Không phân bổ khi available = 0 (nghỉ phép và không giao việc) -> Đạt kỳ vọng (OPTIMAL), không phạt nhàn rỗi
        assertThat(WeeklyCapacityMatrixPolicy.isOverloaded(BigDecimal.ZERO, BigDecimal.ZERO)).isFalse();
        assertThat(WeeklyCapacityMatrixPolicy.calculateExcessHours(BigDecimal.ZERO, BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(WeeklyCapacityMatrixPolicy.calculateUtilizationPercentage(BigDecimal.ZERO, BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(WeeklyCapacityMatrixPolicy.determineStatus(BigDecimal.ZERO, BigDecimal.ZERO))
                .isEqualTo(CapacityStatus.OPTIMAL);
    }

    @Test
    @DisplayName("Chuẩn hóa ngữ nghĩa remainingHours: luôn >= 0, không bao giờ âm khi quá tải")
    void testCalculateRemainingHoursSemantics() {
        // Còn dư năng lực: available = 40, allocated = 32 -> remaining = 8
        assertThat(WeeklyCapacityMatrixPolicy.calculateRemainingHours(BigDecimal.valueOf(40), BigDecimal.valueOf(32)))
                .isEqualByComparingTo(BigDecimal.valueOf(8.0));

        // Vừa vặn 100%: remaining = 0
        assertThat(WeeklyCapacityMatrixPolicy.calculateRemainingHours(BigDecimal.valueOf(40), BigDecimal.valueOf(40)))
                .isEqualByComparingTo(BigDecimal.ZERO);

        // Quá tải: available = 40, allocated = 48 -> remaining = 0 (thay vì -8, số giờ vượt được phản ánh qua excessHours = 8)
        assertThat(WeeklyCapacityMatrixPolicy.calculateRemainingHours(BigDecimal.valueOf(40), BigDecimal.valueOf(48)))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Điều chỉnh giờ khả dụng khi hợp đồng lao động hết hạn (adjustAvailableHoursForContract)")
    void testAdjustAvailableHoursForContract() {
        java.time.LocalDate weekStart = java.time.LocalDate.of(2026, 9, 7); // Thứ Hai
        java.time.LocalDate weekEnd = java.time.LocalDate.of(2026, 9, 13);   // Chủ Nhật
        BigDecimal baseHours = BigDecimal.valueOf(40.0);

        // 1. Không có ngày hết hạn hợp đồng -> giữ nguyên base
        assertThat(WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(baseHours, null, weekStart, weekEnd, 5))
                .isEqualByComparingTo(BigDecimal.valueOf(40.0));

        // 2. Hết hạn trước tuần bắt đầu (ví dụ 04/09) -> 0h
        assertThat(WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(baseHours, java.time.LocalDate.of(2026, 9, 4), weekStart, weekEnd, 5))
                .isEqualByComparingTo(BigDecimal.ZERO);

        // 3. Hết hạn sau tuần kết thúc (ví dụ 30/09) -> giữ nguyên 40h
        assertThat(WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(baseHours, java.time.LocalDate.of(2026, 9, 30), weekStart, weekEnd, 5))
                .isEqualByComparingTo(BigDecimal.valueOf(40.0));

        // 4. Hết hạn vào giữa tuần: Thứ Tư (09/09) -> Còn 3 ngày làm việc (Thứ 2, 3, 4) -> 40 * 3 / 5 = 24h
        assertThat(WeeklyCapacityMatrixPolicy.adjustAvailableHoursForContract(baseHours, java.time.LocalDate.of(2026, 9, 9), weekStart, weekEnd, 5))
                .isEqualByComparingTo(BigDecimal.valueOf(24.0));
    }

    @Test
    @DisplayName("Tính tỷ lệ sử dụng trung bình (calculateAverageUtilization)")
    void testCalculateAverageUtilization() {
        assertThat(WeeklyCapacityMatrixPolicy.calculateAverageUtilization(BigDecimal.valueOf(30), BigDecimal.valueOf(40)))
                .isEqualByComparingTo(BigDecimal.valueOf(75.0));

        // available = 0, allocated = 10 -> null (Quá tải / Vô cực)
        assertThat(WeeklyCapacityMatrixPolicy.calculateAverageUtilization(BigDecimal.valueOf(10), BigDecimal.ZERO))
                .isNull();

        // available = 0, allocated = 0 -> 0.0
        assertThat(WeeklyCapacityMatrixPolicy.calculateAverageUtilization(BigDecimal.ZERO, BigDecimal.ZERO))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
