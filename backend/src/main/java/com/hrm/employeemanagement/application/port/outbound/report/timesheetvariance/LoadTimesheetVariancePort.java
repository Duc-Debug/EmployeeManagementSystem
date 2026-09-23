package com.hrm.employeemanagement.application.port.outbound.report.timesheetvariance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Outbound Port tải dữ liệu giờ công thực tế đã duyệt phục vụ báo cáo đối chiếu.
 */
public interface LoadTimesheetVariancePort {

    /**
     * Tải tổng số giờ công thực tế ĐÃ ĐƯỢC DUYỆT (status = 'APPROVED')
     * cho danh sách nhân viên và các tuần mục tiêu, có thể lọc theo dự án.
     *
     * @param employeeIds danh sách ID nhân viên
     * @param targetWeeks danh sách các tuần ISO
     * @param projectId   ID dự án (nếu lọc theo dự án cụ thể, null nếu tất cả)
     * @return Map với key dạng "employeeId_projectId_year_week" (hoặc "employeeId_year_week") và value là tổng số giờ đã duyệt
     */
    Map<String, BigDecimal> loadApprovedActualHours(List<Long> employeeIds, List<YearWeek> targetWeeks, Long projectId);

    /**
     * Kiểm tra xem các tuần có tồn tại dữ liệu giờ công đã duyệt (APPROVED) hay không.
     *
     * @param employeeIds danh sách ID nhân viên
     * @param targetWeeks danh sách các tuần ISO
     * @return Map với key dạng "employeeId_year_week" -> boolean (true nếu có ít nhất 1 entry hoặc timesheet đã duyệt)
     */
    Map<String, Boolean> checkApprovedTimesheetExistence(List<Long> employeeIds, List<YearWeek> targetWeeks);
}
