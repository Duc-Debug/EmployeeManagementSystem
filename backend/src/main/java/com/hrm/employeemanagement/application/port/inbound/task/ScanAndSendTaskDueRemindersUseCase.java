package com.hrm.employeemanagement.application.port.inbound.task;

import java.time.LocalDate;

import com.hrm.employeemanagement.application.dto.task.TaskDueReminderScanResult;

/**
 * UseCase rà soát các công việc đang làm có hạn trong ba ngày tới và gửi nhắc cho người phụ trách (TC-01, TC-02, QTN-19).
 */
public interface ScanAndSendTaskDueRemindersUseCase {

    /**
     * Rà soát các công việc có hạn trong ba ngày tới tính từ ngày chỉ định và gửi thông báo nhắc việc.
     *
     * @param scanDate Ngày thực hiện rà soát
     * @return Kết quả tổng hợp số lượng quét, gửi và bỏ qua do QTN-19
     */
    TaskDueReminderScanResult execute(LocalDate scanDate);
}
