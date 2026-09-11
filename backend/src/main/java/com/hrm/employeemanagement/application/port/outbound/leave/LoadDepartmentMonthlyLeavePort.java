package com.hrm.employeemanagement.application.port.outbound.leave;

import com.hrm.employeemanagement.domain.employee.Employee;
import com.hrm.employeemanagement.domain.leave.LeaveCalendarItem;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Output Port: Tải dữ liệu đơn nghỉ phép của bộ phận trong khoảng ngày (NCL-05-CN-006).
 */
public interface LoadDepartmentMonthlyLeavePort {

    /**
     * Lấy tất cả đơn nghỉ phép (APPROVED và PENDING) của danh sách nhân sự trong khoảng ngày tháng.
     */
    List<LeaveCalendarItem> findLeavesForEmployees(
            List<Long> employeeIds,
            LocalDate startDate,
            LocalDate endDate,
            Map<Long, Employee> employeeMap
    );
}
