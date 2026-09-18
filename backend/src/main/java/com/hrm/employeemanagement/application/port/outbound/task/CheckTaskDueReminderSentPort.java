package com.hrm.employeemanagement.application.port.outbound.task;

import java.time.LocalDate;

import com.hrm.employeemanagement.domain.user.UserId;

/**
 * Outbound port kiểm tra thông báo nhắc việc đã được gửi hay chưa theo quy tắc QTN-19.
 * Một sự kiện chỉ sinh ra một thông báo cho mỗi người nhận, các lần rà soát sau không gửi lại.
 */
public interface CheckTaskDueReminderSentPort {

    /**
     * Kiểm tra xem thông báo nhắc hạn cho task này vào hạn chót dueDate đã gửi cho recipientId chưa.
     *
     * @param recipientId ID người nhận (UserId)
     * @param taskId ID công việc
     * @param dueDate Hạn chót của công việc
     * @return true nếu đã từng gửi thông báo cho sự kiện này, false nếu chưa từng gửi
     */
    boolean hasReminderBeenSent(UserId recipientId, Long taskId, LocalDate dueDate);
}
