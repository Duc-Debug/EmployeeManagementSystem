package com.hrm.employeemanagement.application.port.inbound.allocation;

import java.time.LocalDate;
import com.hrm.employeemanagement.application.dto.allocation.MyWeeklyAllocationsResult;

public interface GetMyAllocationsUseCase {
    MyWeeklyAllocationsResult getMyAllocations(LocalDate weekStart, Integer weeks);
}