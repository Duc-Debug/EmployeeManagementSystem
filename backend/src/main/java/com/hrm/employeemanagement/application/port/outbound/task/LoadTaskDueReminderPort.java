package com.hrm.employeemanagement.application.port.outbound.task;

import java.time.LocalDate;
import java.util.List;

import com.hrm.employeemanagement.domain.employee.EmployeeId;
import com.hrm.employeemanagement.domain.task.Task;

/**
 * Outbound port tải danh sách công việc phục vụ rà soát nhắc hạn (NCL-11-CN-004).
 */
public interface LoadTaskDueReminderPort {

    /**
     * Tải các công việc có hạn (dueDate hoặc plannedEndDate) nằm trong khoảng thời gian từ fromDate đến toDate
     * và chưa ở trạng thái hoàn thành / hủy.
     */
    List<Task> findTasksDueBetween(LocalDate fromDate, LocalDate toDate);

    /**
     * Tải các công việc sắp đến hạn được giao cho một nhân sự cụ thể.
     */
    List<Task> findUpcomingTasksByAssignee(EmployeeId assigneeId, LocalDate fromDate, LocalDate toDate);
}
