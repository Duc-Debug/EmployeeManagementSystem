package com.hrm.employeemanagement.domain.task;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import com.hrm.employeemanagement.domain.exception.task.InvalidTaskDataException;

/**
 * Quy tắc nghiệp vụ cập nhật tiến độ công việc của Nhân viên chuyên môn (NCL-04-CN-002).
 */
public final class TaskProgressPolicy {

    private static final Set<TaskStatus> ALLOWED_PROGRESS_STATUSES = Collections.unmodifiableSet(
            EnumSet.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW, TaskStatus.DONE)
    );

    private TaskProgressPolicy() {
        // Utility class
    }

    /**
     * Xác thực trạng thái cập nhật tiến độ của nhân viên chuyên môn theo cơ chế Whitelist.
     * Nhân viên chuyên môn chỉ được chuyển trạng thái qua:
     * TODO (Chưa bắt đầu), IN_PROGRESS (Đang làm), IN_REVIEW (Chờ duyệt), DONE (Hoàn thành).
     * Các trạng thái khác (như CANCELLED) thuộc quyền quản trị của Quản lý dự án.
     */
    public static void validateProgressStatus(TaskStatus status) {
        if (status == null) {
            throw new InvalidTaskDataException("Trạng thái tiến độ công việc không được để trống");
        }
        if (!ALLOWED_PROGRESS_STATUSES.contains(status)) {
            throw new InvalidTaskDataException("Nhân viên chuyên môn chỉ được chuyển trạng thái qua: Chưa bắt đầu (TODO), Đang làm (IN_PROGRESS), Chờ duyệt (IN_REVIEW) hoặc Hoàn thành (DONE)");
        }
    }

    public static Set<TaskStatus> getAllowedProgressStatuses() {
        return ALLOWED_PROGRESS_STATUSES;
    }
}
