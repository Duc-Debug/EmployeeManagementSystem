package com.hrm.employeemanagement.application.port.outbound.allocation;

import com.hrm.employeemanagement.domain.allocation.AllocationChangeLog;

public interface SaveAllocationChangeLogPort {

    AllocationChangeLog save(AllocationChangeLog changeLog);
}
