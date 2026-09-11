package com.hrm.employeemanagement.domain.leave;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NCL-05-CN-003: LeaveRequest Domain Approval and Rejection Tests")
class LeaveRequestDomainTest {

    @Test
    @DisplayName("Duyệt đơn PENDING thành công -> Chuyển sang APPROVED và lưu approverId")
    void approvePendingLeaveRequest_ShouldChangeStatusToApproved() {
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Nghỉ phép thường niên"
        );

        leaveRequest.approve(99L, "Đồng ý phê duyệt");

        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(leaveRequest.getApproverId()).isEqualTo(99L);
        assertThat(leaveRequest.getApproverComment()).isEqualTo("Đồng ý phê duyệt");
        assertThat(leaveRequest.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Cố tình duyệt đơn không ở trạng thái PENDING -> Ném IllegalStateException")
    void approveNonPendingLeaveRequest_ShouldThrowException() {
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Nghỉ phép thường niên"
        );
        leaveRequest.approve(99L, "Đã duyệt lần 1");

        assertThatThrownBy(() -> leaveRequest.approve(99L, "Cố tình duyệt lại"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Chỉ có thể phê duyệt đơn nghỉ phép khi đang ở trạng thái Chờ duyệt (PENDING)");
    }

    @Test
    @DisplayName("Từ chối đơn PENDING kèm lý do -> Chuyển sang REJECTED và lưu lý do")
    void rejectPendingLeaveRequest_ShouldChangeStatusToRejected() {
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Nghỉ phép thường niên"
        );

        leaveRequest.reject(99L, "Trùng thời điểm dự án Golive");

        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(leaveRequest.getApproverId()).isEqualTo(99L);
        assertThat(leaveRequest.getApproverComment()).isEqualTo("Trùng thời điểm dự án Golive");
        assertThat(leaveRequest.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Từ chối đơn nhưng bỏ trống lý do -> Ném IllegalArgumentException")
    void rejectLeaveRequestWithoutReason_ShouldThrowException() {
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Nghỉ phép thường niên"
        );

        assertThatThrownBy(() -> leaveRequest.reject(99L, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lý do từ chối đơn nghỉ phép không được để trống");

        assertThatThrownBy(() -> leaveRequest.reject(99L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lý do từ chối đơn nghỉ phép không được để trống");
    }
}
