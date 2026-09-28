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

    @Test
    @DisplayName("NCL-05-CN-007: Yêu cầu hủy đơn đã duyệt thành công khi ngày nghỉ ở tương lai -> Chuyển CANCEL_REQUESTED")
    void requestCancellation_Success_WhenLeaveInFuture() {
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Nghỉ phép"
        );
        leaveRequest.approve(99L, "Đã duyệt");

        LocalDate today = LocalDate.of(2026, 10, 20); // Trước ngày nghỉ 2026-11-02
        leaveRequest.requestCancellation("Kế hoạch dự án thay đổi", today);

        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveStatus.CANCEL_REQUESTED);
        assertThat(leaveRequest.getCancellationReason()).isEqualTo("Kế hoạch dự án thay đổi");
        assertThat(leaveRequest.getCancellationRequestedAt()).isNotNull();
    }

    @Test
    @DisplayName("NCL-05-CN-007: Yêu cầu hủy đơn khi ngày nghỉ đã hoặc đang diễn ra -> Ném PastLeaveCancellationException")
    void requestCancellation_Fails_WhenLeaveAlreadyStartedOrPast() {
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Nghỉ phép"
        );
        leaveRequest.approve(99L, "Đã duyệt");

        // Hôm nay trùng ngày bắt đầu nghỉ
        LocalDate today = LocalDate.of(2026, 11, 2);
        assertThatThrownBy(() -> leaveRequest.requestCancellation("Xin hủy ngày bắt đầu", today))
                .isInstanceOf(com.hrm.employeemanagement.domain.exception.leave.PastLeaveCancellationException.class)
                .hasMessageContaining("Chỉ có thể hủy đơn nghỉ phép khi ngày nghỉ chưa diễn ra");

        // Hôm nay đã qua ngày bắt đầu nghỉ
        LocalDate pastDate = LocalDate.of(2026, 11, 3);
        assertThatThrownBy(() -> leaveRequest.requestCancellation("Xin hủy quá khứ", pastDate))
                .isInstanceOf(com.hrm.employeemanagement.domain.exception.leave.PastLeaveCancellationException.class);
    }

    @Test
    @DisplayName("NCL-05-CN-007: Duyệt yêu cầu hủy thành công khi ngày nghỉ ở tương lai -> Chuyển CANCELLED")
    void approveCancellation_Success_WhenLeaveInFuture() {
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Nghỉ phép"
        );
        leaveRequest.approve(99L, "Đã duyệt");
        leaveRequest.requestCancellation("Kế hoạch thay đổi", LocalDate.of(2026, 10, 20));

        LocalDate approveDate = LocalDate.of(2026, 10, 25);
        leaveRequest.approveCancellation(88L, "Đồng ý hủy đơn", approveDate);

        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(leaveRequest.getApproverId()).isEqualTo(88L);
        assertThat(leaveRequest.getApproverComment()).isEqualTo("Đồng ý hủy đơn");
    }

    @Test
    @DisplayName("NCL-05-CN-007: Duyệt yêu cầu hủy nhưng ngày nghỉ đã diễn ra -> Ném PastLeaveCancellationException")
    void approveCancellation_Fails_WhenLeaveAlreadyStarted() {
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Nghỉ phép"
        );
        leaveRequest.approve(99L, "Đã duyệt");
        leaveRequest.requestCancellation("Kế hoạch thay đổi", LocalDate.of(2026, 10, 20));

        LocalDate lateApproveDate = LocalDate.of(2026, 11, 2);
        assertThatThrownBy(() -> leaveRequest.approveCancellation(88L, "Duyệt muộn", lateApproveDate))
                .isInstanceOf(com.hrm.employeemanagement.domain.exception.leave.PastLeaveCancellationException.class);
    }

    @Test
    @DisplayName("NCL-05-CN-007: Quản lý từ chối yêu cầu hủy -> Chuyển lại về APPROVED")
    void rejectCancellation_Success_ShouldRevertToApproved() {
        LeaveRequest leaveRequest = LeaveRequest.createPending(
                10L,
                LeaveType.ANNUAL,
                LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 4),
                3,
                BigDecimal.valueOf(24.00),
                "Nghỉ phép"
        );
        leaveRequest.approve(99L, "Đã duyệt");
        leaveRequest.requestCancellation("Kế hoạch thay đổi", LocalDate.of(2026, 10, 20));

        leaveRequest.rejectCancellation(88L, "Dự án không kịp sắp xếp lại nhân sự");

        assertThat(leaveRequest.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(leaveRequest.getApproverId()).isEqualTo(88L);
        assertThat(leaveRequest.getApproverComment()).isEqualTo("Dự án không kịp sắp xếp lại nhân sự");
    }
}
