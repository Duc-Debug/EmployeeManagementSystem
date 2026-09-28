package com.hrm.employeemanagement.application.port.outbound.allocation;

import com.hrm.employeemanagement.domain.availability.YearWeek;

/**
 * Outbound port kiểm tra giờ công thực tế phát sinh của nhân sự theo dự án và tuần (TC-02, K3).
 */
public interface CheckActualHoursPort {

    boolean hasActualHours(Long employeeId, Long projectId, YearWeek yearWeek);
}
