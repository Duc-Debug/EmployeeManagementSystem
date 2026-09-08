package com.hrm.employeemanagement.application.port.inbound.allocation;

import java.util.List;

import com.hrm.employeemanagement.application.dto.allocation.AllocateResourceCommand;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;

public interface AllocateResourceUseCase {

    // Phân bổ giờ cho nhân sự vào dự án theo tuần
    WeeklyCapacityResult allocateResource(AllocateResourceCommand command);

    // Truy vấn công suất & giờ rảnh của danh sách nhân sự theo tuần
    List<WeeklyCapacityResult> getWeeklyCapacities(List<Long> employeeIds, Integer year, Integer weekNumber);
}
