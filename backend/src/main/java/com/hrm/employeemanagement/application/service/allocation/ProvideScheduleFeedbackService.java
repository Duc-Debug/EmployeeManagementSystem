package com.hrm.employeemanagement.application.service.allocation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import com.hrm.employeemanagement.application.dto.allocation.ProvideScheduleFeedbackResult;
import com.hrm.employeemanagement.application.port.inbound.allocation.ProvideScheduleFeedbackUseCase;
import com.hrm.employeemanagement.application.port.outbound.allocation.ScheduleConfirmationPort;
import com.hrm.employeemanagement.application.port.outbound.authorization.GetAuthenticatedUserPort;
import com.hrm.employeemanagement.domain.allocation.confirmation.ScheduleConfirmationPolicy;
import com.hrm.employeemanagement.domain.user.User;

/**
 * Pure Application Service (POJO) - Thuc hien nghiep vu phan hoi lich phan bo (NCL-13-CN-002).
 * Quy tac QTN-24: Y kien phan hoi tu nhan su duoc ghi nhan de PM/RM xem xet, tuyet doi khong lam thay doi phan bo tu dong.
 */
public class ProvideScheduleFeedbackService implements ProvideScheduleFeedbackUseCase {

    private final GetAuthenticatedUserPort authenticatedUserPort;
    private final ScheduleConfirmationPort scheduleConfirmationPort;

    public ProvideScheduleFeedbackService(
            GetAuthenticatedUserPort authenticatedUserPort,
            ScheduleConfirmationPort scheduleConfirmationPort) {
        this.authenticatedUserPort = Objects.requireNonNull(authenticatedUserPort, "GetAuthenticatedUserPort must not be null");
        this.scheduleConfirmationPort = Objects.requireNonNull(scheduleConfirmationPort, "ScheduleConfirmationPort must not be null");
    }

    @Override
    public ProvideScheduleFeedbackResult provideFeedback(LocalDate weekStart, String reason, String ipAddress) {
        User currentUser = authenticatedUserPort.getAuthenticatedUser();
        if (currentUser == null) {
            throw new IllegalStateException("Không tìm thấy người dùng đã xác thực");
        }

        ScheduleConfirmationPolicy.validateFeedbackReason(reason);

        Long userId = currentUser.getId().value();
        LocalDate monday = ScheduleConfirmationPolicy.normalizeToMonday(weekStart);
        LocalDateTime now = LocalDateTime.now();

        // QTN-24: Ghi nhan y kien phan hoi kem ly do ma khong can thiep sua doi allocation hours/percentage
        ScheduleConfirmationPort.SaveConfirmationResult saveResult = scheduleConfirmationPort.saveFeedback(
                userId,
                monday,
                reason.trim(),
                now,
                ipAddress
        );

        LocalDateTime finalFeedbackAt = saveResult.record().feedbackAt() != null ? saveResult.record().feedbackAt() : now;

        return new ProvideScheduleFeedbackResult(
                200,
                monday,
                finalFeedbackAt,
                reason.trim(),
                "HAS_FEEDBACK",
                "Đã gửi ý kiến phản hồi về phân bổ nguồn lực thành công theo quy tắc QTN-24. PM/RM sẽ rà soát và phản hồi nếu cần thiết."
        );
    }
}
