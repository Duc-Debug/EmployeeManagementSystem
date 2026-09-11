package com.hrm.employeemanagement.application.dto.leave;

/**
 * Query DTO yêu cầu tra cứu lịch nghỉ bộ phận theo tháng (NCL-05-CN-006).
 */
public record GetDepartmentMonthlyLeaveCalendarQuery(
        Long orgUnitId,
        Integer year,
        Integer month,
        Double warningThresholdRate
) {
    public GetDepartmentMonthlyLeaveCalendarQuery {
        if (orgUnitId == null || orgUnitId <= 0) {
            throw new IllegalArgumentException("ID đơn vị/bộ phận (orgUnitId) phải là số dương hợp lệ");
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new IllegalArgumentException("Tháng phải nằm trong khoảng từ 1 đến 12");
        }
        if (year != null && year < 1970) {
            throw new IllegalArgumentException("Năm không hợp lệ");
        }
        if (warningThresholdRate != null && (warningThresholdRate <= 0.0 || warningThresholdRate > 1.0)) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo phải lớn hơn 0 và nhỏ hơn hoặc bằng 1.0 (ví dụ 0.5 là 50%)");
        }
    }
}
