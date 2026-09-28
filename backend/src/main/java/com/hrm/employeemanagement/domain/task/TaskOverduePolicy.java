package com.hrm.employeemanagement.domain.task;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Domain Policy xác định trạng thái quá hạn của công việc (NCL-04-CN-003).
 * Áp dụng Java thuần (Pure Java), bảo vệ quy tắc nghiệp vụ theo tiêu chuẩn DDD.
 */
public final class TaskOverduePolicy {

    private TaskOverduePolicy() {
        // Utility class
    }

    /**
     * Xác định công việc có bị quá hạn hay không dựa trên ngày kết thúc dự kiến và ngày hiện tại.
     * Một công việc được xem là quá hạn khi và chỉ khi:
     * 1. Có ngày kết thúc dự kiến (plannedEndDate hoặc dueDate khác null).
     * 2. Ngày kết thúc dự kiến trước ngày kiểm tra (effectivePlannedEnd.isBefore(currentDate)).
     * 3. Trạng thái công việc chưa hoàn thành (status != DONE) và không bị hủy (status != CANCELLED).
     */
    public static boolean isOverdue(Task task, LocalDate currentDate) {
        if (task == null || currentDate == null) {
            return false;
        }

        TaskStatus status = task.getStatus() != null ? task.getStatus() : TaskStatus.TODO;
        if (status == TaskStatus.DONE || status == TaskStatus.CANCELLED) {
            return false;
        }

        LocalDate effectivePlannedEnd = task.getPlannedEndDate() != null
                ? task.getPlannedEndDate()
                : task.getDueDate();

        if (effectivePlannedEnd == null) {
            return false;
        }

        return effectivePlannedEnd.isBefore(currentDate);
    }

    /**
     * Tính số ngày quá hạn (overdue days).
     * Trả về số ngày quá hạn nếu công việc quá hạn, ngược lại trả về 0.
     */
    public static long calculateOverdueDays(Task task, LocalDate currentDate) {
        if (!isOverdue(task, currentDate)) {
            return 0L;
        }

        LocalDate effectivePlannedEnd = task.getPlannedEndDate() != null
                ? task.getPlannedEndDate()
                : task.getDueDate();

        return ChronoUnit.DAYS.between(effectivePlannedEnd, currentDate);
    }
}
