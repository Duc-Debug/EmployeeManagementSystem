package com.hrm.employeemanagement.domain.outsourcedcontract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.hrm.employeemanagement.domain.availability.YearWeek;

import static org.assertj.core.api.Assertions.assertThat;

class OutsourcedContractExpirationPolicyTest {

    private final LocalDate today = LocalDate.of(2026, 9, 21);

    @Test
    @DisplayName("TC-01: Hợp đồng còn 25 ngày là hết hạn -> Đánh giá trạng thái EXPIRING_SOON")
    void shouldEvaluateExpiringSoonWhen25DaysRemaining() {
        LocalDate contractEnd = today.plusDays(25); // 2026-10-16
        long daysRemaining = OutsourcedContractExpirationPolicy.calculateDaysRemaining(contractEnd, today);
        OutsourcedContractStatus status = OutsourcedContractExpirationPolicy.evaluateStatus(contractEnd, today, 30);

        assertThat(daysRemaining).isEqualTo(25);
        assertThat(status).isEqualTo(OutsourcedContractStatus.EXPIRING_SOON);
    }

    @Test
    @DisplayName("TC-02: Hợp đồng còn hơn 30 ngày -> Đánh giá trạng thái ACTIVE_SAFE")
    void shouldEvaluateActiveSafeWhenMoreThan30DaysRemaining() {
        LocalDate contractEnd = today.plusDays(60);
        long daysRemaining = OutsourcedContractExpirationPolicy.calculateDaysRemaining(contractEnd, today);
        OutsourcedContractStatus status = OutsourcedContractExpirationPolicy.evaluateStatus(contractEnd, today, 30);

        assertThat(daysRemaining).isEqualTo(60);
        assertThat(status).isEqualTo(OutsourcedContractStatus.ACTIVE_SAFE);
    }

    @Test
    @DisplayName("Hợp đồng đã quá hạn -> Đánh giá trạng thái EXPIRED")
    void shouldEvaluateExpiredWhenPastContractEndDate() {
        LocalDate contractEnd = today.minusDays(5);
        long daysRemaining = OutsourcedContractExpirationPolicy.calculateDaysRemaining(contractEnd, today);
        OutsourcedContractStatus status = OutsourcedContractExpirationPolicy.evaluateStatus(contractEnd, today, 30);

        assertThat(daysRemaining).isNegative();
        assertThat(status).isEqualTo(OutsourcedContractStatus.EXPIRED);
    }

    @Test
    @DisplayName("QTN-21: Hợp đồng hết hạn giữa tuần (Thứ Sáu) -> Phân bổ cả tuần bị vắt qua ngày hết hạn SPANS_OVER_EXPIRY")
    void shouldDetectSpansOverExpiryWhenContractEndsMidWeek() {
        // Tuần 42 năm 2026: Thứ Hai 2026-10-12 đến Chủ Nhật 2026-10-18
        YearWeek week42 = YearWeek.of(2026, 42);
        LocalDate contractEnd = LocalDate.of(2026, 10, 16); // Thứ Sáu

        Optional<OutsourcedContractAffectedAllocation> result = OutsourcedContractExpirationPolicy.evaluateAllocationImpact(
                contractEnd, week42, BigDecimal.valueOf(40.0), 1001L, 200L, "Dự án CRM"
        );

        assertThat(result).isPresent();
        OutsourcedContractAffectedAllocation affected = result.get();
        assertThat(affected.affectedType()).isEqualTo(AffectedAllocationType.SPANS_OVER_EXPIRY);
        assertThat(affected.allocationId()).isEqualTo(1001L);
        assertThat(affected.projectId()).isEqualTo(200L);
        assertThat(affected.projectName()).isEqualTo("Dự án CRM");
        assertThat(affected.reason()).contains("vắt qua hạn hợp đồng vi phạm quy tắc QTN-21");
    }

    @Test
    @DisplayName("QTN-21: Tuần phân bổ nằm sau ngày hết hạn hợp đồng -> Đánh giá AFTER_EXPIRY")
    void shouldDetectAfterExpiryWhenWeekStartsAfterContractEndDate() {
        // Hợp đồng hết hạn vào 2026-10-16 (Tuần 42)
        // Tuần 43 năm 2026: Thứ Hai 2026-10-19 đến Chủ Nhật 2026-10-25
        YearWeek week43 = YearWeek.of(2026, 43);
        LocalDate contractEnd = LocalDate.of(2026, 10, 16);

        Optional<OutsourcedContractAffectedAllocation> result = OutsourcedContractExpirationPolicy.evaluateAllocationImpact(
                contractEnd, week43, BigDecimal.valueOf(20.0), 1002L, 200L, "Dự án CRM"
        );

        assertThat(result).isPresent();
        OutsourcedContractAffectedAllocation affected = result.get();
        assertThat(affected.affectedType()).isEqualTo(AffectedAllocationType.AFTER_EXPIRY);
        assertThat(affected.reason()).contains("bắt đầu sau ngày hết hạn hợp đồng");
        assertThat(affected.reason()).contains("vi phạm quy tắc QTN-21");
    }

    @Test
    @DisplayName("QTN-21: Tuần phân bổ kết thúc trước hoặc đúng ngày hết hạn hợp đồng -> Hợp lệ, không bị ảnh hưởng")
    void shouldReturnEmptyWhenWeekEndsOnOrBeforeContractEndDate() {
        // Tuần 41 năm 2026: Thứ Hai 2026-10-05 đến Chủ Nhật 2026-10-11
        YearWeek week41 = YearWeek.of(2026, 41);
        LocalDate contractEnd = LocalDate.of(2026, 10, 16);

        Optional<OutsourcedContractAffectedAllocation> result = OutsourcedContractExpirationPolicy.evaluateAllocationImpact(
                contractEnd, week41, BigDecimal.valueOf(40.0), 1000L, 200L, "Dự án CRM"
        );

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Phân bổ có số giờ bằng 0 hoặc null -> Bỏ qua, không coi là phân bổ ảnh hưởng")
    void shouldIgnoreZeroOrNullAllocatedHours() {
        YearWeek week43 = YearWeek.of(2026, 43);
        LocalDate contractEnd = LocalDate.of(2026, 10, 16);

        Optional<OutsourcedContractAffectedAllocation> zeroHours = OutsourcedContractExpirationPolicy.evaluateAllocationImpact(
                contractEnd, week43, BigDecimal.ZERO, 1003L, 200L, "Dự án CRM"
        );
        Optional<OutsourcedContractAffectedAllocation> nullHours = OutsourcedContractExpirationPolicy.evaluateAllocationImpact(
                contractEnd, week43, null, 1004L, 200L, "Dự án CRM"
        );

        assertThat(zeroHours).isEmpty();
        assertThat(nullHours).isEmpty();
    }
}
