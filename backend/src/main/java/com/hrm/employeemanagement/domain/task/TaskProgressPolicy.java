package com.hrm.employeemanagement.domain.task;

import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;

/**
 * Quy tắc nghiệp vụ cập nhật tiến độ công việc của Nhân viên chuyên môn (NCL-04-CN-002).
 */
public final class TaskProgressPolicy {

    private TaskProgressPolicy() {
        // Utility class
    }

    /**
     * Xác thực trạng thái cập nhật tiến độ của nhân viên chuyên môn.
     * Nhân viên chuyên môn chỉ được chuyển trạng thái qua:
     * TODO (Chưa bắt đầu), IN_PROGRESS (Đang làm), IN_REVIEW (Chờ duyệt), DONE (Hoàn thành).
     * Trạng thái CANCELLED thuộc quyền quản trị của Quản lý dự án.
     */
    public static void validateProgressStatus(TaskStatus status) {
        if (status == null) {
            throw new InvalidTaskDataException("Trạng thái tiến độ công việc không được để trống");
        }
        if (status == TaskStatus.CANCELLED) {
            throw new InvalidTaskDataException("Nhân viên chuyên môn không được phép hủy công việc (CANCELLED)");
        }
    }
}
