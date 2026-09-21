package com.hrm.employeemanagement.application.port.inbound.allocation;

import java.time.LocalDate;
import com.hrm.employeemanagement.application.dto.allocation.ConfirmScheduleViewedResult;

public interface ConfirmScheduleViewedUseCase {
    ConfirmScheduleViewedResult confirmScheduleViewed(LocalDate weekStart, String ipAddress);
}