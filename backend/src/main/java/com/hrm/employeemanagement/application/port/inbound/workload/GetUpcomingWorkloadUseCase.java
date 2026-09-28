package com.hrm.employeemanagement.application.port.inbound.workload;

import com.hrm.employeemanagement.application.dto.workload.GetUpcomingWorkloadQuery;
import com.hrm.employeemanagement.application.dto.workload.UpcomingWorkloadResult;

public interface GetUpcomingWorkloadUseCase {

    /**
     * Lấy khối lượng công việc sắp tới của chính người dùng hiện tại (VT-04 / Specialist / Employee).
     */
    UpcomingWorkloadResult getMyUpcomingWorkload(Integer fromYear, Integer fromWeek, Integer durationWeeks);

    /**
     * Lấy khối lượng công việc sắp tới của một nhân sự theo truy vấn (tuân thủ phân quyền và DataScope).
     */
    UpcomingWorkloadResult getUpcomingWorkload(GetUpcomingWorkloadQuery query);
}
