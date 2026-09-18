package com.hrm.employeemanagement.domain.notification;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.hrm.employeemanagement.domain.task.TaskStatus;

/**
 * Domain Policy thuần Java xử lý quy tắc nghiệp vụ cho chức năng NCL-11-CN-004:
 * - Kiểm tra thời hạn 3 ngày tới (BR-01, TC-01)
 * - Kiểm tra trạng thái hợp lệ, loại trừ công việc hoàn thành / hủy (TC-02)
 * - Xây dựng đường dẫn trực tiếp mở công việc từ thông báo
 * - Xây dựng khóa sự kiện duy nhất tuân thủ quy tắc chống gửi trùng QTN-19
 */
public final class TaskDueReminderPolicy {

    public static final int DEFAULT_DUE_SOON_DAYS = 3;

    private TaskDueReminderPolicy() {
    }

    /**
     * Kiểm tra xem công việc có thời hạn trong khoảng N ngày tới (mặc định 3 ngày) kể từ ngày rà soát hay không.
     */
    public static boolean isDueWithinDays(LocalDate dueDate, LocalDate currentDate, int days) {
        if (dueDate == null || currentDate == null) {
            return false;
        }
        return !dueDate.isBefore(currentDate) && !dueDate.isAfter(currentDate.plusDays(days));
    }

    public static boolean isDueWithinDays(LocalDate dueDate, LocalDate currentDate) {
        return isDueWithinDays(dueDate, currentDate, DEFAULT_DUE_SOON_DAYS);
    }

    /**
     * Kiểm tra trạng thái công việc có hợp lệ để gửi nhắc việc hay không (TC-02).
     * Chỉ gửi nhắc nhở cho công việc chưa hoàn thành (TODO, IN_PROGRESS, IN_REVIEW).
     * Tuyệt đối không gửi cho công việc đã hoàn thành (DONE) hoặc đã hủy (CANCELLED).
     */
    public static boolean isEligibleStatus(TaskStatus status) {
        if (status == null) {
            return false;
        }
        return status != TaskStatus.DONE && status != TaskStatus.CANCELLED;
    }

    /**
     * Tính số ngày còn lại từ ngày hiện tại đến hạn chót.
     */
    public static long calculateDaysRemaining(LocalDate dueDate, LocalDate currentDate) {
        if (dueDate == null || currentDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(currentDate, dueDate);
    }

    /**
     * Tạo đường dẫn trực tiếp mở công việc từ thông báo (Postcondition).
     */
    public static String buildDirectTaskUrl(Long projectId, Long taskId) {
        if (projectId != null && taskId != null) {
            return "/projects/" + projectId + "/tasks/" + taskId;
        }
        if (taskId != null) {
            return "/tasks/" + taskId;
        }
        return "/tasks";
    }

    /**
     * Tạo tiêu đề thông báo nhắc việc.
     */
    public static String buildNotificationTitle(String taskName) {
        String safeName = (taskName != null && !taskName.isBlank()) ? taskName.trim() : "Công việc";
        return "Nhắc việc sắp đến hạn: " + safeName;
    }

    /**
     * Tạo nội dung thông báo kèm thời hạn và đường dẫn tới công việc.
     */
    public static String buildNotificationContent(String taskName, LocalDate dueDate, long daysRemaining, String directUrl) {
        String safeName = (taskName != null && !taskName.isBlank()) ? taskName.trim() : "Công việc";
        String timeText = daysRemaining <= 0 ? "hôm nay" : "còn " + daysRemaining + " ngày";
        String content = "Công việc '" + safeName + "' sẽ đến hạn vào ngày " + dueDate + " (" + timeText + ").";
        if (directUrl != null && !directUrl.isBlank()) {
            content += " Đường dẫn: " + directUrl;
        }
        return content;
    }

    /**
     * Tạo source_event_key duy nhất tuân thủ quy tắc QTN-19:
     * Định danh sự kiện: TASK_DUE_REMINDER:{taskId}:{dueDate}
     * Đảm bảo một sự kiện hạn chót của một công việc chỉ sinh tối đa 1 thông báo, các lần rà soát sau bỏ qua.
     */
    public static String buildSourceEventKey(Long taskId, LocalDate dueDate) {
        return "TASK_DUE_REMINDER:" + taskId + ":" + dueDate;
    }

    /**
     * Xác định mức độ khẩn cấp của thông báo:
     * - CAO: nếu còn <= 1 ngày
     * - TRUNG_BINH: nếu còn 2 - 3 ngày
     */
    public static NotificationLevel determineNotificationLevel(long daysRemaining) {
        if (daysRemaining <= 1) {
            return NotificationLevel.CAO;
        }
        return NotificationLevel.TRUNG_BINH;
    }
}
