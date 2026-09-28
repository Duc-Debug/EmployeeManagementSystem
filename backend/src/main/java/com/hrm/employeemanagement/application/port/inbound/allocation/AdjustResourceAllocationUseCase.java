package com.hrm.employeemanagement.application.port.inbound.allocation;

import java.util.List;
import com.hrm.employeemanagement.application.dto.allocation.AdjustAllocationCommand;
import com.hrm.employeemanagement.application.dto.allocation.AllocationChangeLogResult;
import com.hrm.employeemanagement.application.dto.allocation.WeeklyCapacityResult;

public interface AdjustResourceAllocationUseCase {

    WeeklyCapacityResult adjustAllocation(Long allocationId, AdjustAllocationCommand command);

    void removeAllocation(Long allocationId);

    WeeklyCapacityResult noteVariance(Long allocationId, String varianceReason);

    List<AllocationChangeLogResult> getHistory(Long allocationId);
}
