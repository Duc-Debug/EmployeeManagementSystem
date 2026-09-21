package com.hrm.employeemanagement.application.port.outbound.notification.dedup;

import java.time.LocalDateTime;

import com.hrm.employeemanagement.application.dto.notification.dedup.EmployeeWeeklyOverloadCandidate;

public interface OverloadAlertDispatcherPort {

    /**
     * Thực hiện cấp phát khóa chống trùng và gửi thông báo cảnh báo quá tải một cách nguyên tử (Atomic transaction).
     *
     * @param candidate       thông tin nhân sự quá tải
     * @param recipientUserId ID người nhận thông báo
     * @param yearWeek        chuỗi tuần ISO (vd: 2026-W38)
     * @param now             thời điểm hiện tại
     * @param expiresAt       thời điểm hết hạn của bản ghi dedup
     * @return kết quả dispatch: ALERTED nếu gửi thành công, SKIPPED_DEDUP nếu đã tồn tại hoặc đụng độ race condition, FAILED nếu có lỗi xảy ra (được rollback)
     */
    OverloadAlertDispatchResult dispatchOverloadAlert(
            EmployeeWeeklyOverloadCandidate candidate,
            Long recipientUserId,
            String yearWeek,
            LocalDateTime now,
            LocalDateTime expiresAt
    );
}
