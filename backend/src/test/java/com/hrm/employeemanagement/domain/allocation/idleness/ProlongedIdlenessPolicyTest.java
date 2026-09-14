package com.hrm.employeemanagement.domain.allocation.idleness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ProlongedIdlenessPolicy Domain Unit Tests (NCL-07-CN-006 / QTN-23)")
class ProlongedIdlenessPolicyTest {

    @Test
    @DisplayName("QTN-23 — Tính tỷ lệ sử dụng % chính xác")
    void calculateUtilizationRate_StandardCases() {
        // 10h / 40h = 25.0%
        BigDecimal rate = ProlongedIdlenessPolicy.calculateUtilizationRate(BigDecimal.valueOf(10), BigDecimal.valueOf(40));
        assertEquals(BigDecimal.valueOf(25.0).setScale(1, RoundingMode.HALF_UP), rate);

        // 0h / 40h = 0.0%
        BigDecimal zeroRate = ProlongedIdlenessPolicy.calculateUtilizationRate(BigDecimal.ZERO, BigDecimal.valueOf(40));
        assertEquals(BigDecimal.valueOf(0.0).setScale(1, RoundingMode.HALF_UP), zeroRate);

        // available = 0 -> 0.0%
        BigDecimal noAvailable = ProlongedIdlenessPolicy.calculateUtilizationRate(BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(BigDecimal.valueOf(0.0).setScale(1, RoundingMode.HALF_UP), noAvailable);
    }

    @Test
    @DisplayName("QTN-23 — Tính số giờ trống (emptyHours) chính xác")
    void calculateEmptyHours_StandardCases() {
        // 40h available, 10h allocated -> 30.00h trống
        BigDecimal empty = ProlongedIdlenessPolicy.calculateEmptyHours(BigDecimal.valueOf(10), BigDecimal.valueOf(40));
        assertEquals(BigDecimal.valueOf(30.00).setScale(2, RoundingMode.HALF_UP), empty);

        // 40h available, 45h allocated -> 0.00h trống
        BigDecimal over = ProlongedIdlenessPolicy.calculateEmptyHours(BigDecimal.valueOf(45), BigDecimal.valueOf(40));
        assertEquals(BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_UP), over);
    }

    @Test
    @DisplayName("QTN-23 — Đánh giá tuần nhàn rỗi theo ngưỡng động cấu hình")
    void isWeekUnderutilized_DynamicThreshold() {
        BigDecimal idleThreshold30 = BigDecimal.valueOf(30.0);

        // 10h / 40h = 25% < 30% -> Underutilized
        assertTrue(ProlongedIdlenessPolicy.isWeekUnderutilized(BigDecimal.valueOf(10), BigDecimal.valueOf(40), idleThreshold30));

        // 12h / 40h = 30% == 30% -> Không nhàn rỗi (đạt ngưỡng)
        assertFalse(ProlongedIdlenessPolicy.isWeekUnderutilized(BigDecimal.valueOf(12), BigDecimal.valueOf(40), idleThreshold30));

        // 20h / 40h = 50% > 30% -> Không nhàn rỗi
        assertFalse(ProlongedIdlenessPolicy.isWeekUnderutilized(BigDecimal.valueOf(20), BigDecimal.valueOf(40), idleThreshold30));

        // Tuần có available = 0 và allocated = 0 -> Không tính là nhàn rỗi
        assertFalse(ProlongedIdlenessPolicy.isWeekUnderutilized(BigDecimal.ZERO, BigDecimal.ZERO, idleThreshold30));
    }

    @Test
    @DisplayName("NCL-07-CN-006-TC-01 — Phát hiện chuỗi 3 tuần liên tiếp dưới ngưỡng nhàn rỗi")
    void qualifiesForProlongedIdlenessAlert_SuccessThreeWeeks() {
        WeeklyIdlenessDetail w1 = createDetail(2026, 38, 40, 10, true, false); // 25%
        WeeklyIdlenessDetail w2 = createDetail(2026, 39, 40, 8, true, false);  // 20%
        WeeklyIdlenessDetail w3 = createDetail(2026, 40, 40, 5, true, false);  // 12.5%
        WeeklyIdlenessDetail w4 = createDetail(2026, 41, 40, 35, false, false); // 87.5%

        List<WeeklyIdlenessDetail> list = List.of(w1, w2, w3, w4);

        int maxConsecutive = ProlongedIdlenessPolicy.findMaxConsecutiveIdleWeeks(list);
        assertEquals(3, maxConsecutive);

        assertTrue(ProlongedIdlenessPolicy.qualifiesForProlongedIdlenessAlert(list, 3));
    }

    @Test
    @DisplayName("NCL-07-CN-006-TC-01 — Không đạt điều kiện khi chuỗi nhàn rỗi bị ngắt quãng chỉ còn 2 tuần")
    void qualifiesForProlongedIdlenessAlert_InterruptedStreak() {
        WeeklyIdlenessDetail w1 = createDetail(2026, 38, 40, 10, true, false); // Idle
        WeeklyIdlenessDetail w2 = createDetail(2026, 39, 40, 10, true, false); // Idle
        WeeklyIdlenessDetail w3 = createDetail(2026, 40, 40, 35, false, false); // Optimal
        WeeklyIdlenessDetail w4 = createDetail(2026, 41, 40, 5, true, false);  // Idle

        List<WeeklyIdlenessDetail> list = List.of(w1, w2, w3, w4);

        int maxConsecutive = ProlongedIdlenessPolicy.findMaxConsecutiveIdleWeeks(list);
        assertEquals(2, maxConsecutive);

        assertFalse(ProlongedIdlenessPolicy.qualifiesForProlongedIdlenessAlert(list, 3));
    }

    @Test
    @DisplayName("NCL-07-CN-006-TC-02 — Loại trừ nhân sự đang nghỉ phép dài ngày (APPROVED) toàn bộ thời gian")
    void qualifiesForProlongedIdlenessAlert_ExcludeLongTermLeave() {
        // 3 tuần đều nghỉ phép hợp lệ dài ngày (available = 0, full leave = true)
        WeeklyIdlenessDetail w1 = createDetail(2026, 38, 0, 0, false, true);
        WeeklyIdlenessDetail w2 = createDetail(2026, 39, 0, 0, false, true);
        WeeklyIdlenessDetail w3 = createDetail(2026, 40, 0, 0, false, true);

        List<WeeklyIdlenessDetail> list = List.of(w1, w2, w3);

        assertTrue(ProlongedIdlenessPolicy.isExcludedDueToLongTermLeave(list));
        assertFalse(ProlongedIdlenessPolicy.qualifiesForProlongedIdlenessAlert(list, 3));
    }

    private WeeklyIdlenessDetail createDetail(
            int year,
            int week,
            double available,
            double allocated,
            boolean isUnderutilized,
            boolean isFullLeave
    ) {
        BigDecimal avail = BigDecimal.valueOf(available);
        BigDecimal alloc = BigDecimal.valueOf(allocated);
        BigDecimal empty = avail.compareTo(alloc) > 0 ? avail.subtract(alloc) : BigDecimal.ZERO;
        BigDecimal util = avail.compareTo(BigDecimal.ZERO) > 0
                ? alloc.multiply(BigDecimal.valueOf(100)).divide(avail, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal approvedLeave = isFullLeave ? BigDecimal.valueOf(40) : BigDecimal.ZERO;

        return new WeeklyIdlenessDetail(
                year,
                week,
                BigDecimal.valueOf(40),
                BigDecimal.ZERO,
                approvedLeave,
                avail,
                alloc,
                empty,
                util,
                isUnderutilized,
                isFullLeave
        );
    }
}
