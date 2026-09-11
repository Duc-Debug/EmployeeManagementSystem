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
}
