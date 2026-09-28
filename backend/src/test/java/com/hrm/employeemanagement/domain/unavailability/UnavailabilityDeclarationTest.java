package com.hrm.employeemanagement.domain.unavailability;

import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityPeriodException;
import com.hrm.employeemanagement.domain.exception.unavailability.InvalidUnavailabilityStatusException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UnavailabilityDeclarationTest {

    @Test
    @DisplayName("Tạo khai báo thời gian không sẵn sàng hợp lệ")
    void testCreateValidDeclaration() {
        LocalDate start = LocalDate.of(2026, 9, 21);
        LocalDate end = LocalDate.of(2026, 9, 22);

        UnavailabilityDeclaration decl = UnavailabilityDeclaration.create(
                1L,
                start,
                end,
                UnavailabilityReasonType.TRAINING,
                "Đi đào tạo chuyên môn",
                BigDecimal.valueOf(16.00)
        );

        assertNotNull(decl);
        assertEquals(1L, decl.getEmployeeId());
        assertEquals(start, decl.getStartDate());
        assertEquals(end, decl.getEndDate());
        assertEquals(UnavailabilityReasonType.TRAINING, decl.getReasonType());
        assertEquals("Đi đào tạo chuyên môn", decl.getReasonDetail());
        assertEquals(BigDecimal.valueOf(16.00), decl.getTotalHoursDeducted());
        assertEquals(UnavailabilityStatus.PENDING, decl.getStatus());
        assertTrue(decl.isPending());
        assertFalse(decl.isApproved());
        assertNull(decl.getApproverId());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi ngày bắt đầu sau ngày kết thúc")
    void testCreateInvalidPeriodThrowsException() {
        LocalDate start = LocalDate.of(2026, 9, 25);
        LocalDate end = LocalDate.of(2026, 9, 21);

        assertThrows(InvalidUnavailabilityPeriodException.class, () ->
                UnavailabilityDeclaration.create(
                        1L,
                        start,
                        end,
                        UnavailabilityReasonType.BUSINESS_TRIP,
                        "Đi công tác",
                        BigDecimal.valueOf(16.00)
                )
        );
    }

    @Test
    @DisplayName("Phê duyệt khai báo chuyển trạng thái sang APPROVED và lưu người duyệt")
    void testApproveDeclarationSuccess() {
        UnavailabilityDeclaration decl = UnavailabilityDeclaration.create(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Đào tạo",
                BigDecimal.valueOf(16.00)
        );

        decl.approve(99L, "Đồng ý cho đi đào tạo");

        assertEquals(UnavailabilityStatus.APPROVED, decl.getStatus());
        assertTrue(decl.isApproved());
        assertEquals(99L, decl.getApproverId());
        assertEquals("Đồng ý cho đi đào tạo", decl.getApproverComment());
        assertNotNull(decl.getApprovedAt());
    }

    @Test
    @DisplayName("Không cho phép duyệt đơn đã được duyệt trước đó")
    void testApproveAlreadyApprovedThrowsException() {
        UnavailabilityDeclaration decl = UnavailabilityDeclaration.create(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.TRAINING,
                "Đào tạo",
                BigDecimal.valueOf(16.00)
        );
        decl.approve(99L, "OK");

        assertThrows(InvalidUnavailabilityStatusException.class, () ->
                decl.approve(100L, "Duyệt lại")
        );
    }

    @Test
    @DisplayName("Từ chối khai báo chuyển trạng thái sang REJECTED và lưu lý do")
    void testRejectDeclarationSuccess() {
        UnavailabilityDeclaration decl = UnavailabilityDeclaration.create(
                1L,
                LocalDate.of(2026, 9, 21),
                LocalDate.of(2026, 9, 22),
                UnavailabilityReasonType.BUSINESS_TRIP,
                "Công tác",
                BigDecimal.valueOf(16.00)
        );

        decl.reject(99L, "Dự án đang gấp, không thể đi lúc này");

        assertEquals(UnavailabilityStatus.REJECTED, decl.getStatus());
        assertEquals(99L, decl.getApproverId());
        assertEquals("Dự án đang gấp, không thể đi lúc này", decl.getApproverComment());
    }
}
