package com.hrm.employeemanagement.application.port.inbound.task;

import java.util.List;

import com.hrm.employeemanagement.application.dto.task.UpcomingDueTaskResult;

/**
 * UseCase lấy danh sách công việc sắp đến hạn dành cho Nhân viên chuyên môn (VT-04) (TC-03).
 */
public interface GetMyUpcomingDueTasksUseCase {

    /**
     * Lấy danh sách công việc sắp đến hạn trong 3 ngày tới được giao cho tài khoản đang đăng nhập.
     *
     * @return Danh sách công việc sắp đến hạn kèm đường dẫn mở trực tiếp
     */
    List<UpcomingDueTaskResult> execute();
}
